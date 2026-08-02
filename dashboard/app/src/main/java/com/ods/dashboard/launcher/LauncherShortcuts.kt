package com.ods.dashboard.launcher

import com.ods.dashboard.R

/**
 * One cell of the home-screen launcher grid: a logo, a one-word label under it, and the
 * destination a tap opens.
 *
 * @param id       stable key — used for the per-slot overrides in [LauncherStore] and to
 *                 make each widget cell's PendingIntent distinct
 * @param label    the word rendered under the icon (keep it to one short word)
 * @param url      default destination; the user can override it in Customise → Launcher
 * @param iconRes  bundled drawable for the mark (overridable with a picked image)
 */
data class Shortcut(
    val id: String,
    val label: String,
    val url: String,
    val iconRes: Int,
)

/**
 * The launcher registry — a plain data structure, which is all this needs to be
 * (CLAUDE.md: prefer a lookup table over anything cleverer).
 *
 * Laid out as [ROWS] rows of [COLUMNS], read left-to-right, top-to-bottom:
 *
 * ```
 * Aether   Link     Recon    Assist     <- the OIS products
 * Leads    Socials  AANi     3D Print   <- the ventures / tools
 * Home     Tools    Docs     Vercel     <- the site, tooling and infra
 * ```
 *
 * URLs were resolved from the live Vercel projects and repo routing, not guessed:
 * `ods-link.com` is the ODS Link production domain (socials = its marketing dashboard,
 * tools = its tools hub); `ods-recon.com/aani-app.html` is the page the AANi Telegram
 * mini app actually serves (the `aani-webapp` edge function behind it is POST-only and
 * has nothing to render in a browser); Docs is ODS TheView, the GitHub-vault viewer.
 */
object LauncherGrid {
    const val COLUMNS = 4
    const val ROWS = 3

    val all: List<Shortcut> = listOf(
        // ---- row 1 — the OIS products ----
        Shortcut("aether", "Aether", "https://ods-aether.vercel.app", R.drawable.ic_launch_aether),
        Shortcut("link", "Link", "https://ods-link.com", R.drawable.ic_launch_link),
        Shortcut("recon", "Recon", "https://ods-recon.com", R.drawable.ic_launch_recon),
        Shortcut("assist", "Assist", "https://ods-assistant.vercel.app", R.drawable.ic_launch_assist),

        // ---- row 2 — ventures and operator tools ----
        Shortcut("leads", "Leads", "https://lead-gen-beryl-one.vercel.app", R.drawable.ic_launch_leads),
        Shortcut("socials", "Socials", "https://ods-link.com/dashboard/marketing", R.drawable.ic_launch_socials),
        Shortcut("aani", "AANi", "https://ods-recon.com/aani-app.html", R.drawable.ic_launch_aani),
        Shortcut("print3d", "3D Print", "https://ods-3dprint-web.vercel.app", R.drawable.ic_launch_print3d),

        // ---- row 3 — site, tooling, infrastructure ----
        Shortcut("home", "Home", "https://www.outlined-design.com", R.drawable.ic_ods_logo),
        Shortcut("tools", "Tools", "https://ods-link.com/tools", R.drawable.ic_launch_tools),
        Shortcut("docs", "Docs", "https://ods-the-view.vercel.app", R.drawable.ic_launch_docs),
        Shortcut("vercel", "Vercel", "https://vercel.com/zakkgray1-3026s-projects", R.drawable.ic_launch_vercel),
    )

    /** [all] chunked into rows for the widget's Column-of-Rows layout. */
    val rows: List<List<Shortcut>> = all.chunked(COLUMNS)

    fun byId(id: String): Shortcut? = all.firstOrNull { it.id == id }

    init {
        require(all.size == ROWS * COLUMNS) {
            "Launcher grid must hold exactly ${ROWS * COLUMNS} shortcuts, found ${all.size}"
        }
    }
}
