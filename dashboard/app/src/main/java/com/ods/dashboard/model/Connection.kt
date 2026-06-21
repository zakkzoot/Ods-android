package com.ods.dashboard.model

/** Which band a connection lives in on the dashboard. */
enum class Band { SOCIALS, SYSTEMS, ACCOUNTS }

/** How a connection's status is determined. */
enum class CheckType {
    HTTP,        // GET/HEAD the url; up on 2xx/3xx
    VERCEL,      // Vercel REST API (deploy state + platform status)
    SUPABASE,    // Supabase REST health endpoint
    GITHUB,      // GitHub notifications/PR counts
    GMAIL,       // Gmail unread count (OAuth per account)
    META,        // Meta Graph API page activity (Facebook / Instagram)
    LINK_ONLY,   // no status source — deep-link only (e.g. LinkedIn)
}

/**
 * One ODS connection tile.
 *
 * @param id        stable key (used for deep links from the widget)
 * @param label     display name
 * @param monogram  2–3 char fallback shown when no logo drawable is set
 * @param band      which section it renders in
 * @param check     how status is resolved
 * @param url       where a confirmed (second) tap navigates
 * @param iconRes   optional drawable res id for a real brand logo (0 = use monogram)
 */
data class Connection(
    val id: String,
    val label: String,
    val monogram: String,
    val band: Band,
    val check: CheckType,
    val url: String,
    val iconRes: Int = 0,
)

/**
 * The connection registry — single source of truth for the whole dashboard.
 *
 * URLs marked TODO are placeholders: drop in the real handles/refs and (where noted)
 * provide the matching token in Settings so the status source can light up. Every
 * tile still deep-links and renders without a token; it just shows an "unknown" dot.
 */
object Connections {
    val all: List<Connection> = listOf(
        // ---- SOCIALS (direct access + notification bubbles) ----
        Connection("facebook", "Facebook", "FB", Band.SOCIALS, CheckType.META,
            url = "https://facebook.com/outlineddesignsolutions"),
        Connection("instagram", "Instagram", "IG", Band.SOCIALS, CheckType.META,
            url = "https://instagram.com/outlineddesignsolutions"),
        Connection("linkedin", "LinkedIn", "IN", Band.SOCIALS, CheckType.LINK_ONLY,
            url = "https://www.linkedin.com/company/outlineddesignsolutions"),

        // ---- SYSTEMS (uptime / health) ----
        Connection("website", "outlined-design.com", "OD", Band.SYSTEMS, CheckType.HTTP,
            url = "https://www.outlined-design.com"),
        Connection("tasjeel", "Tasjeel", "TJ", Band.SYSTEMS, CheckType.HTTP,
            url = "https://tasjeel.app"), // TODO: real Tasjeel url
        Connection("vercel", "Vercel", "VC", Band.SYSTEMS, CheckType.VERCEL,
            url = "https://vercel.com/dashboard"),
        Connection("supabase", "ODS Supabase", "SB", Band.SYSTEMS, CheckType.SUPABASE,
            url = "https://supabase.com/dashboard/project/kfbtpcwakpyptwqqhabu"), // Link-Core project
        Connection("recon", "ods-recon", "RC", Band.SYSTEMS, CheckType.HTTP,
            url = "https://ods-recon.com"), // live production (custom domain)
        Connection("link", "ods-link", "LK", Band.SYSTEMS, CheckType.HTTP,
            url = "https://ods-link-core.vercel.app"), // Vercel project (prod may be protected/erroring)
        Connection("assist", "ods-assist", "AS", Band.SYSTEMS, CheckType.HTTP,
            url = "https://ods-assist.vercel.app"), // TODO: no Vercel project found yet — set when deployed
        Connection("aether", "ods-aether", "AE", Band.SYSTEMS, CheckType.HTTP,
            url = "https://ods-aether.vercel.app"), // TODO: no Vercel project found yet — set when deployed

        // ---- ACCOUNTS (unread / activity counts) ----
        Connection("github", "GitHub @zakkzoot", "GH", Band.ACCOUNTS, CheckType.GITHUB,
            url = "https://github.com/zakkzoot"),
        // info@outlined-design.com is NOT a Google account — it's the business mailbox on
        // cPanel (host s1308.sgp1.mysecurecloudhost.com, IMAP/POP/SMTP). The tile opens
        // its webmail inbox (no Gmail OAuth / unread count). To light up a live count
        // later, add an IMAP check against that host with stored credentials.
        // Fallback inbox URL if the webmail subdomain isn't set up:
        // https://s1308.sgp1.mysecurecloudhost.com:2096
        Connection("email_ods", "info@outlined-design.com", "@O", Band.ACCOUNTS, CheckType.LINK_ONLY,
            url = "https://webmail.outlined-design.com"),
        // The personal Google account — the only Gmail-checked inbox.
        Connection("email_gmail", "zakkgray1@gmail.com", "@G", Band.ACCOUNTS, CheckType.GMAIL,
            url = "mailto:zakkgray1@gmail.com"),
    )

    fun byId(id: String): Connection? = all.firstOrNull { it.id == id }
    fun inBand(band: Band): List<Connection> = all.filter { it.band == band }
}
