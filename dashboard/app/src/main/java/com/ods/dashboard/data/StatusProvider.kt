package com.ods.dashboard.data

import android.content.Context
import com.ods.dashboard.model.CheckType
import com.ods.dashboard.model.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Session
import javax.mail.search.FlagTerm

/**
 * Resolves the live status of one connection. All work runs on IO. Failures degrade
 * gracefully to Health.DOWN / UNKNOWN with a human-readable figure rather than crashing.
 */
class StatusProvider(
    private val context: Context,
    private val config: SecureConfig,
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun check(c: Connection, pulse: PulseSite? = null): ConnectionStatus = withContext(Dispatchers.IO) {
        runCatching {
            when (c.check) {
                CheckType.HTTP -> if (pulse != null) httpFromPulse(c, pulse) else httpHealth(c)
                CheckType.VERCEL -> vercel(c)
                CheckType.SUPABASE -> supabase(c)
                CheckType.GITHUB -> github(c)
                CheckType.GMAIL -> gmail(c)
                CheckType.META -> meta(c)
                CheckType.TELEGRAM -> telegram(c)
                CheckType.IMAP -> imap(c)
                CheckType.LINK_ONLY -> ConnectionStatus(c.id, Health.UNKNOWN, checkedAtMs = now())
            }
        }.getOrElse { e ->
            ConnectionStatus(
                c.id, Health.DOWN, checkedAtMs = now(),
                figures = listOf(Figure("error", e.message ?: "check failed")),
            )
        }
    }

    // ---- Site health from ODS Pulse (server-side uptime/latency) ----
    private fun httpFromPulse(c: Connection, p: PulseSite): ConnectionStatus {
        val health = when {
            p.ok == true && (p.ttfbMs ?: 0) <= 2000 -> Health.UP
            p.ok == true -> Health.DEGRADED
            p.ok == false -> Health.DOWN
            else -> Health.UNKNOWN
        }
        val figures = buildList {
            p.status?.let { add(Figure("status", it.toString())) }
            p.ttfbMs?.let { add(Figure("load", "$it ms")) }
            p.uptime24h?.let { add(Figure("uptime 24h", "$it%")) }
            p.checks24h?.let { add(Figure("checks 24h", it.toString())) }
            add(Figure("host", c.url.substringAfter("//").substringBefore("/")))
        }
        return ConnectionStatus(c.id, health, latencyMs = p.ttfbMs?.toLong(), checkedAtMs = now(), figures = figures)
    }

    // ---- HTTP uptime: 2xx/3xx = up, slow = degraded, 5xx/timeout = down ----
    private fun httpHealth(c: Connection): ConnectionStatus {
        val start = System.currentTimeMillis()
        val req = Request.Builder().url(c.url).head().header("User-Agent", "ODS-Dashboard").build()
        http.newCall(req).execute().use { resp ->
            val ms = System.currentTimeMillis() - start
            val health = when {
                resp.code in 200..399 && ms <= 2000 -> Health.UP
                resp.code in 200..399 -> Health.DEGRADED
                else -> Health.DOWN
            }
            return ConnectionStatus(
                c.id, health, latencyMs = ms, checkedAtMs = now(),
                figures = listOf(
                    Figure("status", resp.code.toString()),
                    Figure("latency", "${ms} ms"),
                    Figure("host", c.url.substringAfter("//").substringBefore("/")),
                ),
            )
        }
    }

    // ---- Vercel: latest deployment state (token-gated) ----
    private fun vercel(c: Connection): ConnectionStatus {
        val token = config.get(SecureConfig.VERCEL_TOKEN)
            ?: return unconfigured(c, "add Vercel token in Settings")
        val req = Request.Builder()
            .url("https://api.vercel.com/v6/deployments?limit=1")
            .header("Authorization", "Bearer $token").build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            val state = runCatching {
                JSONObject(body).getJSONArray("deployments").optJSONObject(0)?.optString("state")
            }.getOrNull() ?: "unknown"
            val health = when {
                state.equals("READY", true) -> Health.UP
                state.equals("ERROR", true) -> Health.DOWN
                else -> Health.DEGRADED
            }
            return ConnectionStatus(
                c.id, health, checkedAtMs = now(),
                figures = listOf(Figure("latest deploy", state), Figure("api", resp.code.toString())),
            )
        }
    }

    // ---- Supabase: keyless GoTrue health (no anon key needed for up/down) ----
    private fun supabase(c: Connection): ConnectionStatus {
        val base = config.get(SecureConfig.SUPABASE_URL) ?: Defaults.SUPABASE_URL
        val req = Request.Builder().url("$base/auth/v1/health").build()
        http.newCall(req).execute().use { resp ->
            val health = if (resp.code in 200..399) Health.UP else Health.DOWN
            return ConnectionStatus(
                c.id, health, checkedAtMs = now(),
                figures = listOf(
                    Figure("auth health", resp.code.toString()),
                    Figure("host", base.substringAfter("//").substringBefore("/")),
                ),
            )
        }
    }

    // ---- GitHub: unread notifications count + headlines ----
    private fun github(c: Connection): ConnectionStatus {
        val pat = config.get(SecureConfig.GITHUB_PAT)
            ?: return unconfigured(c, "add GitHub PAT in Settings")
        val req = Request.Builder()
            .url("https://api.github.com/notifications")
            .header("Authorization", "Bearer $pat")
            .header("Accept", "application/vnd.github+json").build()
        http.newCall(req).execute().use { resp ->
            val arr = runCatching { JSONArray(resp.body?.string().orEmpty()) }.getOrNull()
            val count = arr?.length() ?: 0
            val items = buildList {
                if (arr != null) for (i in 0 until minOf(arr.length(), 6)) {
                    val n = arr.optJSONObject(i) ?: continue
                    val title = n.optJSONObject("subject")?.optString("title").orEmpty()
                    val repo = n.optJSONObject("repository")?.optString("full_name").orEmpty()
                    val line = listOf(repo, title).filter { it.isNotBlank() }.joinToString(" · ")
                    if (line.isNotBlank()) add(line)
                }
            }
            val reviews = runCatching {
                val r = Request.Builder()
                    .url("https://api.github.com/search/issues?q=is:open+is:pr+review-requested:@me&per_page=1")
                    .header("Authorization", "Bearer $pat")
                    .header("Accept", "application/vnd.github+json").build()
                http.newCall(r).execute().use { rr ->
                    JSONObject(rr.body?.string().orEmpty()).optInt("total_count", 0)
                }
            }.getOrDefault(0)
            val figures = buildList {
                add(Figure("unread", count.toString()))
                if (reviews > 0) add(Figure("PRs to review", reviews.toString()))
                add(Figure("api", resp.code.toString()))
            }
            return ConnectionStatus(
                c.id, if (resp.code in 200..399) Health.UP else Health.DOWN,
                badge = count + reviews, checkedAtMs = now(),
                figures = figures,
                items = items,
            )
        }
    }

    // ---- Telegram: bot-token health + channel members (best effort) ----
    private fun telegram(c: Connection): ConnectionStatus {
        val token = config.get(SecureConfig.TELEGRAM_BOT_TOKEN)
            ?: return unconfigured(c, "add Telegram bot token in Settings")
        val me = Request.Builder().url("https://api.telegram.org/bot$token/getMe").build()
        val ok = http.newCall(me).execute().use { it.code in 200..299 }
        if (!ok) return ConnectionStatus(
            c.id, Health.DOWN, checkedAtMs = now(),
            figures = listOf(Figure("telegram", "invalid bot token")),
        )
        val handle = c.apiRef
        if (handle != null) {
            val req = Request.Builder()
                .url("https://api.telegram.org/bot$token/getChatMemberCount?chat_id=$handle").build()
            val members = http.newCall(req).execute().use { resp ->
                runCatching { JSONObject(resp.body?.string().orEmpty()).optInt("result", -1) }.getOrDefault(-1)
            }
            if (members >= 0) return ConnectionStatus(
                c.id, Health.UP, checkedAtMs = now(),
                figures = listOf(Figure("members", members.toString())),
            )
        }
        return ConnectionStatus(c.id, Health.UP, checkedAtMs = now(), figures = listOf(Figure("bot", "ok")))
    }

    // ---- Gmail: unread inbox count + recent subjects via OAuth (per account) ----
    private suspend fun gmail(c: Connection): ConnectionStatus {
        if (!GmailAuth.isConfigured(config)) return unconfigured(c, "add Google client ID in Settings")
        if (!GmailAuth.isConnected(config, c.id)) return unconfigured(c, "connect this account in Settings")
        val summary = GmailAuth.inboxSummary(context, config, c.id)
            ?: return ConnectionStatus(
                c.id, Health.DEGRADED, checkedAtMs = now(),
                figures = listOf(Figure("gmail", "reauth needed")),
            )
        return ConnectionStatus(
            c.id, Health.UP, badge = summary.unread, checkedAtMs = now(),
            figures = listOf(Figure("unread", summary.unread.toString())),
            items = summary.subjects,
        )
    }

    // ---- IMAP: unread inbox count for the cPanel mailbox ----
    private fun imap(c: Connection): ConnectionStatus {
        val host = config.get(SecureConfig.IMAP_HOST) ?: Defaults.IMAP_HOST
        val user = config.get(SecureConfig.IMAP_USER) ?: Defaults.IMAP_USER
        val pass = config.get(SecureConfig.IMAP_PASSWORD)
            ?: return unconfigured(c, "add mailbox password in Settings")
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.connectiontimeout", "8000")
            put("mail.imaps.timeout", "8000")
            put("mail.imaps.writetimeout", "8000")
        }
        val store = Session.getInstance(props).getStore("imaps")
        return try {
            store.connect(host, Defaults.IMAP_PORT, user, pass)
            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            val unread = inbox.unreadMessageCount.coerceAtLeast(0)
            val total = inbox.messageCount.coerceAtLeast(0)
            val subjects = runCatching {
                inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
                    .takeLast(5).asReversed().mapNotNull { m ->
                        val subj = runCatching { m.subject }.getOrNull()
                        val from = runCatching {
                            m.from?.firstOrNull()?.toString()?.substringBefore("<")?.trim()
                        }.getOrNull()
                        listOfNotNull(from, subj).filter { it.isNotBlank() }.joinToString(" — ").ifBlank { null }
                    }
            }.getOrDefault(emptyList())
            inbox.close(false)
            ConnectionStatus(
                c.id, Health.UP, badge = unread, checkedAtMs = now(),
                figures = listOf(Figure("unread", unread.toString()), Figure("total", total.toString())),
                items = subjects,
            )
        } catch (e: Exception) {
            ConnectionStatus(
                c.id, Health.DOWN, checkedAtMs = now(),
                figures = listOf(Figure("imap", e.message ?: "connect failed")),
            )
        } finally {
            runCatching { store.close() }
        }
    }

    // ---- Meta (Facebook / Instagram): followers + unread page messages (page token) ----
    private fun meta(c: Connection): ConnectionStatus {
        val token = config.get(SecureConfig.META_PAGE_TOKEN)
            ?: return unconfigured(c, "add Meta page token for live counts")
        val base = "https://graph.facebook.com/v19.0"
        val followers = runCatching {
            http.newCall(
                Request.Builder().url("$base/me?fields=followers_count,fan_count,name&access_token=$token").build(),
            ).execute().use { resp ->
                val o = JSONObject(resp.body?.string().orEmpty())
                if (o.has("error")) -2 else maxOf(o.optInt("followers_count", -1), o.optInt("fan_count", -1))
            }
        }.getOrDefault(-1)
        if (followers == -2) return ConnectionStatus(
            c.id, Health.DEGRADED, checkedAtMs = now(),
            figures = listOf(Figure("meta", "check token / permissions")),
        )
        val unread = runCatching {
            http.newCall(
                Request.Builder().url("$base/me/conversations?fields=unread_count&limit=25&access_token=$token").build(),
            ).execute().use { resp ->
                val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("data")
                if (arr == null) 0 else (0 until arr.length()).sumOf { arr.optJSONObject(it)?.optInt("unread_count", 0) ?: 0 }
            }
        }.getOrDefault(0)
        val figures = buildList {
            if (followers >= 0) add(Figure("followers", followers.toString()))
            add(Figure("unread msgs", unread.toString()))
        }
        return ConnectionStatus(c.id, Health.UP, badge = unread, checkedAtMs = now(), figures = figures)
    }

    private fun unconfigured(c: Connection, hint: String) =
        ConnectionStatus(c.id, Health.UNKNOWN, checkedAtMs = now(), figures = listOf(Figure("setup", hint)))

    private fun now() = System.currentTimeMillis()
}
