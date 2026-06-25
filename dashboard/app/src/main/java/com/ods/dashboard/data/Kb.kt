package com.ods.dashboard.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One knowledge-base entry from the ODS Vault `kb` edge function. */
data class KbEntry(
    val id: String,
    val title: String,
    val question: String?,
    val answer: String?,
    val content: String?,
    val category: String,
    val language: String?,
    val status: String?,
    val updatedAt: String?,
)

data class KbCategory(val category: String, val count: Int)

/** A client/tenant the KB entries belong to — used by the editor's client picker. */
data class KbClientRef(
    val id: String,
    val name: String,
    val emoji: String?,
    val color: String?,
)

/** Result of a KB fetch. [error] is non-null on failure ("no_key" when unconfigured). */
data class KbData(
    val categories: List<KbCategory> = emptyList(),
    val entries: List<KbEntry> = emptyList(),
    val clients: List<KbClientRef> = emptyList(),
    val error: String? = null,
)

/** Result of a write (create/update/archive/restore). */
data class KbWriteResult(val ok: Boolean, val error: String? = null)

private fun JSONObject.strOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

/**
 * Reads the knowledge base from the Supabase `kb` edge function (which serves the
 * RLS-locked knowledge_base table via the service role). Requires the Supabase anon key
 * (Settings → Supabase anon key, or SUPABASE_ANON_KEY in an imported .env). The key is
 * stored on-device only — never committed — because the KB contains client content.
 */
class KbClient(private val config: SecureConfig) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun fetch(): KbData {
        val base = (config.get(SecureConfig.SUPABASE_URL) ?: Defaults.SUPABASE_URL).trimEnd('/')
        val key = config.get(SecureConfig.SUPABASE_ANON_KEY)
            ?: return KbData(error = "no_key")
        val req = Request.Builder()
            .url("$base/functions/v1/kb")
            .header("apikey", key)
            .header("Authorization", "Bearer $key")
            .build()
        return runCatching {
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code !in 200..299) return KbData(error = "http ${resp.code}")
                val o = JSONObject(body)
                val cats = o.optJSONArray("categories")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        arr.optJSONObject(i)?.let { KbCategory(it.optString("category"), it.optInt("count")) }
                    }
                } ?: emptyList()
                val entries = o.optJSONArray("entries")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        val e = arr.optJSONObject(i) ?: return@mapNotNull null
                        KbEntry(
                            id = e.optString("id"),
                            title = e.optString("title").ifBlank { "(untitled)" },
                            question = e.strOrNull("question"),
                            answer = e.strOrNull("answer"),
                            content = e.strOrNull("content"),
                            category = e.strOrNull("category") ?: "Uncategorised",
                            language = e.strOrNull("language"),
                            status = e.strOrNull("status"),
                            updatedAt = e.strOrNull("updated_at"),
                        )
                    }
                } ?: emptyList()
                val clients = o.optJSONArray("clients")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        val c = arr.optJSONObject(i) ?: return@mapNotNull null
                        val id = c.strOrNull("id") ?: return@mapNotNull null
                        KbClientRef(id, c.optString("name").ifBlank { id.take(8) }, c.strOrNull("emoji"), c.strOrNull("color"))
                    }
                } ?: emptyList()
                KbData(categories = cats, entries = entries, clients = clients)
            }
        }.getOrElse { KbData(error = it.message ?: "error") }
    }

    /** True when an admin token is configured, i.e. editing is possible. */
    fun canEdit(): Boolean = config.get(SecureConfig.SUPABASE_ADMIN_TOKEN) != null

    fun create(clientId: String, fields: Map<String, String?>): KbWriteResult =
        write(JSONObject(fields.filterValues { !it.isNullOrBlank() }).put("action", "create").put("client_id", clientId))

    fun update(id: String, fields: Map<String, String?>): KbWriteResult =
        write(JSONObject(fields.filterValues { it != null }).put("action", "update").put("id", id))

    fun archive(id: String): KbWriteResult = write(JSONObject().put("action", "archive").put("id", id))

    fun restore(id: String): KbWriteResult = write(JSONObject().put("action", "restore").put("id", id))

    private fun write(payload: JSONObject): KbWriteResult {
        val base = (config.get(SecureConfig.SUPABASE_URL) ?: Defaults.SUPABASE_URL).trimEnd('/')
        val key = config.get(SecureConfig.SUPABASE_ANON_KEY) ?: return KbWriteResult(false, "no_key")
        val admin = config.get(SecureConfig.SUPABASE_ADMIN_TOKEN) ?: return KbWriteResult(false, "no_admin_token")
        val req = Request.Builder()
            .url("$base/functions/v1/kb-write")
            .header("apikey", key)
            .header("Authorization", "Bearer $key")
            .header("x-admin-token", admin)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            http.newCall(req).execute().use { resp ->
                if (resp.code in 200..299) KbWriteResult(true)
                else KbWriteResult(false, "http ${resp.code}: ${resp.body?.string().orEmpty().take(200)}")
            }
        }.getOrElse { KbWriteResult(false, it.message ?: "error") }
    }
}
