package com.ods.dashboard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ODS locked palette — mirrors outlined-design.com (tailwind.config.js).
 * Crimson is the ONLY accent. Spend it in one place per view; keep the rest quiet.
 */
object OdsColors {
    val Charcoal = Color(0xFF0E0E10) // base background (the "void")
    val Graphite = Color(0xFF1A1A1E) // cards / panels / tiles
    val Silver = Color(0xFFC9CDD4)   // body text
    val Crimson = Color(0xFFD42B2B)  // single accent — bubbles, active, healthy glow

    // Derived / functional tones (kept on-brand, used with restraint)
    val SilverDim = Color(0xCCC9CDD4)          // 80% silver — secondary text
    val SilverFaint = Color(0x80C9CDD4)        // 50% silver — labels / kickers
    val HairlineLight = Color(0x12FFFFFF)      // inset top highlight (~7% white)
    val HairlineDark = Color(0x8C000000)       // inset bottom shade (~55% black)
    val CrimsonGlow = Color(0x1AD42B2B)        // ~10% crimson — soft elevation glow

    // Status dots for the SYSTEMS band (semantic; green/amber stay muted, down = crimson)
    val StatusUp = Color(0xFF3FB950)       // up
    val StatusDegraded = Color(0xFFD9A23B) // degraded / slow
    val StatusDown = Crimson               // down — reuse the brand accent
    val StatusUnknown = Color(0xFF5A5A60)  // unknown / unconfigured
}
