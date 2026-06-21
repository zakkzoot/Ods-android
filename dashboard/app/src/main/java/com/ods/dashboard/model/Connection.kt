package com.ods.dashboard.model

/**
 * Top-level grouping on the dashboard home. Each category is a card that acts as a
 * collective inbox — it rolls up the status + notifications of every connection inside it.
 */
enum class Category(val title: String) {
    SOCIALS("SOCIALS"),
    SERVICES("SERVICES"),
    SITES("SITES"),
    MESSAGES("MESSAGES"),
}

/** How a connection's status is determined. */
enum class CheckType {
    HTTP,        // GET/HEAD the url; up on 2xx/3xx
    VERCEL,      // Vercel REST API (deploy state + platform status)
    SUPABASE,    // Supabase REST health endpoint
    GITHUB,      // GitHub notifications/PR counts
    GMAIL,       // Gmail unread count (OAuth per account)
    META,        // Meta Graph API page activity (Facebook / Instagram)
    TELEGRAM,    // Telegram Bot API (channel members / message updates)
    LINK_ONLY,   // no status source — deep-link only (e.g. LinkedIn)
}

/**
 * One ODS connection tile.
 *
 * @param id        stable key (used for deep links from the widget)
 * @param label     display name
 * @param monogram  2–3 char fallback shown when no logo drawable / custom icon is set
 * @param category  which section it renders in
 * @param check     how status is resolved
 * @param url       where a confirmed (second) tap navigates
 * @param iconRes   optional bundled drawable res id for a brand logo (0 = use monogram)
 * @param apiRef    optional API identifier (e.g. Telegram channel @handle) for the check
 */
data class Connection(
    val id: String,
    val label: String,
    val monogram: String,
    val category: Category,
    val check: CheckType,
    val url: String,
    val iconRes: Int = 0,
    val apiRef: String? = null,
)

/**
 * The connection registry — single source of truth for the whole dashboard.
 *
 * URLs / handles marked TODO are placeholders: drop in the real values and (where noted)
 * provide the matching token in Settings so the status source can light up. Every tile
 * still deep-links and renders without a token; it just shows an "unknown" dot.
 */
object Connections {
    val all: List<Connection> = listOf(
        // ---- SOCIALS ----
        Connection("facebook", "Facebook", "FB", Category.SOCIALS, CheckType.META,
            url = "https://facebook.com/outlineddesignsolutions"),
        Connection("instagram", "Instagram", "IG", Category.SOCIALS, CheckType.META,
            url = "https://instagram.com/outlineddesignsolutions"),
        Connection("linkedin", "LinkedIn", "IN", Category.SOCIALS, CheckType.LINK_ONLY,
            url = "https://www.linkedin.com/company/outlineddesignsolutions"),
        Connection("telegram_channel", "Telegram Channel", "TG", Category.SOCIALS, CheckType.TELEGRAM,
            url = "https://t.me/OutlinedDesignSolutions", apiRef = "@OutlinedDesignSolutions"),

        // ---- SERVICES ----
        Connection("github", "GitHub @zakkzoot", "GH", Category.SERVICES, CheckType.GITHUB,
            url = "https://github.com/zakkzoot"),
        Connection("vercel", "Vercel", "VC", Category.SERVICES, CheckType.VERCEL,
            url = "https://vercel.com/dashboard"),
        Connection("supabase", "ODS Supabase", "SB", Category.SERVICES, CheckType.SUPABASE,
            url = "https://supabase.com/dashboard/project/kfbtpcwakpyptwqqhabu"), // Link-Core project
        Connection("tasjeel", "Tasjeel", "TJ", Category.SERVICES, CheckType.HTTP,
            url = "https://tasjeel.app"), // TODO: real Tasjeel url

        // ---- SITES (uptime / health) ----
        Connection("website", "outlined-design.com", "OD", Category.SITES, CheckType.HTTP,
            url = "https://www.outlined-design.com"),
        Connection("recon", "ods-recon", "RC", Category.SITES, CheckType.HTTP,
            url = "https://ods-recon.com"), // live production (custom domain)
        Connection("link", "ods-link", "LK", Category.SITES, CheckType.HTTP,
            url = "https://ods-link-core.vercel.app"), // Vercel project (prod may be protected/erroring)
        Connection("aether", "ods-aether", "AE", Category.SITES, CheckType.HTTP,
            url = "https://ods-aether.vercel.app"), // TODO: no Vercel project found yet — set when deployed
        Connection("assist", "ods-assist", "AS", Category.SITES, CheckType.HTTP,
            url = "https://ods-assist.vercel.app"), // TODO: no Vercel project found yet — set when deployed

        // ---- MESSAGES (inboxes) ----
        Connection("telegram", "Telegram", "TG", Category.MESSAGES, CheckType.TELEGRAM,
            url = "https://web.telegram.org/"),
        Connection("email_gmail", "zakkgray1@gmail.com", "@G", Category.MESSAGES, CheckType.GMAIL,
            url = "mailto:zakkgray1@gmail.com"),
        // info@outlined-design.com is NOT a Google account — it's the business mailbox on
        // cPanel (host s1308.sgp1.mysecurecloudhost.com, IMAP/POP/SMTP). The tile opens
        // its webmail inbox (no Gmail OAuth / unread count). To light up a live count
        // later, add an IMAP check against that host with stored credentials.
        Connection("email_ods", "info@outlined-design.com", "@O", Category.MESSAGES, CheckType.LINK_ONLY,
            url = "https://s1308.sgp1.mysecurecloudhost.com:2096"),
    )

    fun byId(id: String): Connection? = all.firstOrNull { it.id == id }
    fun inCategory(category: Category): List<Connection> = all.filter { it.category == category }
}
