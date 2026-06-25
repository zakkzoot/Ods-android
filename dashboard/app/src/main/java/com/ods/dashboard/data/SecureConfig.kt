package com.ods.dashboard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted store for tokens/keys entered in Settings. Nothing is hardcoded in source.
 *
 * Keys expected (all optional — absent means that source stays "unknown"):
 *  - vercel_token      : Vercel REST API token
 *  - github_pat        : GitHub personal access token (notifications scope)
 *  - supabase_url      : https://<ref>.supabase.co
 *  - supabase_anon_key : Supabase anon key
 *  - meta_page_token   : Meta Graph API page access token
 *  - gmail_*           : Gmail OAuth handled separately by AccountManager/AuthorizationClient
 */
class SecureConfig(context: Context) {
    private val app = context.applicationContext
    private val prefs: SharedPreferences

    /** True when tokens are stored encrypted; false if we fell back to plain prefs. */
    val encrypted: Boolean

    init {
        val enc = runCatching {
            EncryptedSharedPreferences.create(
                app,
                "ods_secure_config",
                MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull()
        if (enc != null) {
            prefs = enc
            encrypted = true
        } else {
            // Some devices/ROMs fail to provision the AndroidKeyStore-backed key. Rather
            // than crash on launch, fall back to plain prefs so the app still runs.
            prefs = app.getSharedPreferences("ods_secure_config_plain", Context.MODE_PRIVATE)
            encrypted = false
        }
    }

    fun get(key: String): String? = prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    fun set(key: String, value: String?) = prefs.edit().putString(key, value).apply()
    fun has(key: String): Boolean = get(key) != null

    /**
     * Bulk-apply parsed key/value pairs (e.g. from an imported .env file). Only keys in
     * [ENV_KEYS] are accepted; unknown keys are ignored. Returns the names that were set.
     */
    fun applyEnv(values: Map<String, String>): List<String> {
        val applied = mutableListOf<String>()
        ENV_KEYS.forEach { key ->
            val v = values[key]?.trim().orEmpty()
            if (v.isNotEmpty()) { set(key, v); applied += key }
        }
        return applied
    }

    companion object {
        const val VERCEL_TOKEN = "vercel_token"
        const val GITHUB_PAT = "github_pat"
        const val SUPABASE_URL = "supabase_url"
        const val SUPABASE_ANON_KEY = "supabase_anon_key"
        const val SUPABASE_ADMIN_TOKEN = "supabase_admin_token"
        const val META_PAGE_TOKEN = "meta_page_token"
        const val GOOGLE_CLIENT_ID = "google_client_id"
        const val TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        const val IMAP_HOST = "imap_host"
        const val IMAP_USER = "imap_user"
        const val IMAP_PASSWORD = "imap_password"

        /** Keys accepted from an imported .env file (UPPER_SNAKE in the file == these). */
        val ENV_KEYS = listOf(
            VERCEL_TOKEN, GITHUB_PAT, SUPABASE_URL, SUPABASE_ANON_KEY,
            SUPABASE_ADMIN_TOKEN, META_PAGE_TOKEN, GOOGLE_CLIENT_ID,
            TELEGRAM_BOT_TOKEN, IMAP_HOST, IMAP_USER, IMAP_PASSWORD,
        )

        /** Per-account serialized AppAuth AuthState, keyed by connection id. */
        fun gmailStateKey(connectionId: String) = "gmail_state_$connectionId"

        /**
         * Parse a .env file body into a map. Accepts `KEY=value`, `export KEY=value`,
         * `#` comments, blank lines, and optional surrounding quotes. Keys are lower-cased
         * so `VERCEL_TOKEN` in the file maps onto the constants above.
         */
        fun parseEnv(text: String): Map<String, String> {
            val out = LinkedHashMap<String, String>()
            text.lineSequence().forEach { raw ->
                val line = raw.trim().removePrefix("export ").trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach
                val eq = line.indexOf('=')
                if (eq <= 0) return@forEach
                val key = line.substring(0, eq).trim().lowercase()
                var value = line.substring(eq + 1).trim()
                if (value.length >= 2 &&
                    ((value.first() == '"' && value.last() == '"') ||
                        (value.first() == '\'' && value.last() == '\''))
                ) {
                    value = value.substring(1, value.length - 1)
                }
                out[key] = value
            }
            return out
        }
    }
}
