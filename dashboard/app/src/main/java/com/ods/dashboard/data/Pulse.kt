package com.ods.dashboard.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One site's roll-up from the ODS Pulse edge function. */
data class PulseSite(
    val id: String,
    val ok: Boolean?,
    val status: Int?,
    val ttfbMs: Int?,
    val uptime24h: Int?,
    val checks24h: Int?,
)

/**
 * Reads the ODS Pulse edge function (uptime / latency / status for the web properties).
 * Requires the Supabase anon key (Settings → Supabase anon key, or SUPABASE_ANON_KEY in an
 * imported .env). Returns an empty map if the key is absent or the call fails, so the Sites
 * tiles transparently fall back to the device-side HTTP check.
 */
class PulseClient(private val config: SecureConfig) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetch(): Map<String, PulseSite> {
        val base = (config.get(SecureConfig.SUPABASE_URL) ?: Defaults.SUPABASE_URL).trimEnd('/')
        val key = config.get(SecureConfig.SUPABASE_ANON_KEY) ?: return emptyMap()
        val req = Request.Builder()
            .url("$base/functions/v1/pulse")
            .header("apikey", key)
            .header("Authorization", "Bearer $key")
            .build()
        return runCatching {
            http.newCall(req).execute().use { resp ->
                if (resp.code !in 200..299) return emptyMap()
                val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("sites")
                    ?: return emptyMap()
                buildMap {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val id = o.optString("id")
                        if (id.isBlank()) continue
                        put(
                            id,
                            PulseSite(
                                id = id,
                                ok = if (o.isNull("ok")) null else o.optBoolean("ok"),
                                status = if (o.isNull("status")) null else o.optInt("status"),
                                ttfbMs = if (o.isNull("ttfb_ms")) null else o.optInt("ttfb_ms"),
                                uptime24h = if (o.isNull("uptime_24h")) null else o.optInt("uptime_24h"),
                                checks24h = if (o.isNull("checks_24h")) null else o.optInt("checks_24h"),
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }
}
