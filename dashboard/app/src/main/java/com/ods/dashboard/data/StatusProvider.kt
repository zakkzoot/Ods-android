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
import java.util.concurrent.TimeUnit

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

    suspend fun check(c: Connection): ConnectionStatus = withContext(Dispatchers.IO) {
        runCatching {
            when (c.check) {
                CheckType.HTTP -> httpHealth(c)
                CheckType.VERCEL -> vercel(c)
                CheckType.SUPABASE -> supabase(c)
                CheckType.GITHUB -> github(c)
                CheckType.GMAIL -> gmail(c)
                CheckType.META -> meta(c)
                CheckType.TELEGRAM -> telegram(c)
                CheckType.LINK_ONLY -> ConnectionStatus(c.id, Health.UNKNOWN, checkedAtMs = now())
            }
        }.getOrElse { e ->
            ConnectionStatus(
                c.id, Health.DOWN, checkedAtMs = now(),
                figures = listOf(Figure("error", e.message ?: "check failed")),
            )
        }
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
            return ConnectionStatus(
                c.id, if (resp.code in 200..399) Health.UP else Health.DOWN,
                badge = count, checkedAtMs = now(),
                figures = listOf(Figure("unread", count.toString()), Figure("api", resp.code.toString())),
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

    // ---- Gmail: unread inbox count via OAuth (per account) ----
    private suspend fun gmail(c: Connection): ConnectionStatus {
        if (!GmailAuth.isConfigured(config)) return unconfigured(c, "add Google client ID in Settings")
        if (!GmailAuth.isConnected(config, c.id)) return unconfigured(c, "connect this account in Settings")
        val count = GmailAuth.unreadCount(context, config, c.id)
            ?: return ConnectionStatus(
                c.id, Health.DEGRADED, checkedAtMs = now(),
                figures = listOf(Figure("gmail", "reauth needed")),
            )
        return ConnectionStatus(
            c.id, Health.UP, badge = count, checkedAtMs = now(),
            figures = listOf(Figure("unread", count.toString())),
        )
    }

    // ---- Meta (Facebook / Instagram): page activity (page token) ----
    private fun meta(c: Connection): ConnectionStatus {
        config.get(SecureConfig.META_PAGE_TOKEN)
            ?: return unconfigured(c, "add Meta page token for live counts")
        // With a token, query Graph API page insights / unread here and set badge.
        return ConnectionStatus(c.id, Health.UP, checkedAtMs = now())
    }

    private fun unconfigured(c: Connection, hint: String) =
        ConnectionStatus(c.id, Health.UNKNOWN, checkedAtMs = now(), figures = listOf(Figure("setup", hint)))

    private fun now() = System.currentTimeMillis()
}
