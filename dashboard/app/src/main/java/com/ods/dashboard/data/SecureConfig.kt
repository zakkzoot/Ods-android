package com.ods.dashboard.data

import android.content.Context
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
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ods_secure_config",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun get(key: String): String? = prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    fun set(key: String, value: String?) = prefs.edit().putString(key, value).apply()
    fun has(key: String): Boolean = get(key) != null

    companion object {
        const val VERCEL_TOKEN = "vercel_token"
        const val GITHUB_PAT = "github_pat"
        const val SUPABASE_URL = "supabase_url"
        const val SUPABASE_ANON_KEY = "supabase_anon_key"
        const val META_PAGE_TOKEN = "meta_page_token"
        const val GOOGLE_CLIENT_ID = "google_client_id"

        /** Per-account serialized AppAuth AuthState, keyed by connection id. */
        fun gmailStateKey(connectionId: String) = "gmail_state_$connectionId"
    }
}
