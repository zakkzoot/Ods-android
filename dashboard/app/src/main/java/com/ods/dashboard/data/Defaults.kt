package com.ods.dashboard.data

/**
 * Non-secret, prefilled defaults so the dashboard works out of the box. Anything here
 * can be overridden at runtime from the Settings screen (stored in [SecureConfig]).
 */
object Defaults {
    /**
     * ODS Supabase REST/Auth base. Health is checked via the keyless
     * `/auth/v1/health` endpoint, so no anon key is required just to see up/down.
     * (Primary project: "ODS-Link-Core". Alternate: ODS-Recon =
     * https://sdkypnswnheajfppsjcd.supabase.co)
     */
    const val SUPABASE_URL = "https://kfbtpcwakpyptwqqhabu.supabase.co"

    /**
     * OAuth scope for reading Gmail unread counts. The Google OAuth client ID itself is
     * NOT shipped — enter it in Settings (see SETUP_GOOGLE_OAUTH.md).
     */
    const val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
}
