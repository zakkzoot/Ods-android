package com.ods.dashboard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ods.dashboard.R

/**
 * ODS type system — mirrors outlined-design.com:
 *   Audiowide       -> display / headings / numerals   (.font-display)
 *   Share Tech Mono -> kickers / labels (UPPERCASE, wide tracking)  (.font-mono-kicker)
 *   Sora            -> body / UI (default)
 *
 * The fonts are BUNDLED as OFL TrueType files in res/font/ (not downloaded at runtime),
 * so they render identically on every device with no dependency on Google Play Services.
 * Sora ships as a single variable font; each weight is selected via its `wght` axis.
 */

/** One weight of the Sora variable font, pinned on the `wght` axis. */
private fun sora(weight: Int) = Font(
    resId = R.font.sora_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Audiowide = FontFamily(Font(R.font.audiowide_regular))
val ShareTechMono = FontFamily(Font(R.font.sharetechmono_regular))
val Sora = FontFamily(
    sora(300), // Light
    sora(400), // Normal
    sora(500), // Medium
    sora(600), // SemiBold
    sora(700), // Bold
)

/** Mono kicker: UPPERCASE, wide tracking. Apply text = "label".uppercase() at call sites. */
val KickerStyle = TextStyle(
    fontFamily = ShareTechMono,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 3.2.sp, // ~0.3em feel
    color = OdsColors.SilverFaint,
)

/** Big stat numerals (status figures, counts) — Audiowide. */
val StatNumberStyle = TextStyle(
    fontFamily = Audiowide,
    fontSize = 28.sp,
    lineHeight = 30.sp,
    color = OdsColors.Silver,
)

val OdsTypography = Typography(
    // Display / headings -> Audiowide
    displayLarge = TextStyle(fontFamily = Audiowide, fontSize = 40.sp, lineHeight = 44.sp, color = OdsColors.Silver),
    headlineMedium = TextStyle(fontFamily = Audiowide, fontSize = 24.sp, lineHeight = 28.sp, color = OdsColors.Silver),
    titleLarge = TextStyle(fontFamily = Audiowide, fontSize = 18.sp, lineHeight = 22.sp, color = OdsColors.Silver),

    // Body / UI -> Sora
    bodyLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, color = OdsColors.Silver),
    bodyMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, color = OdsColors.SilverDim),

    // Labels / kickers -> Share Tech Mono
    labelLarge = KickerStyle,
    labelMedium = KickerStyle.copy(fontSize = 10.sp, letterSpacing = 2.6.sp),
)
