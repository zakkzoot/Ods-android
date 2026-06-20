package com.ods.dashboard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ods.dashboard.R

/**
 * ODS type system — mirrors outlined-design.com:
 *   Audiowide       -> display / headings / numerals   (.font-display)
 *   Share Tech Mono -> kickers / labels (UPPERCASE, wide tracking)  (.font-mono-kicker)
 *   Sora            -> body / UI (default)
 *
 * These are all Google Fonts. The cleanest path is the downloadable-fonts provider
 * below (no binaries to ship). If you prefer bundling, drop the .ttf files in
 * res/font/ and swap each FontFamily for Font(R.font.<name>).
 *
 * Downloadable-fonts setup (one-time):
 *  1) build.gradle: implementation("androidx.compose.ui:ui-text-google-fonts:<ver>")
 *  2) res/values/font_certs.xml + res/font/font_provider.xml per the AndroidX
 *     "Downloadable fonts" guide, then reference R.array.com_google_android_gms_fonts_certs
 *     in the provider below.
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val Audiowide = FontFamily(Font(googleFont = GoogleFont("Audiowide"), fontProvider = provider))
val ShareTechMono = FontFamily(Font(googleFont = GoogleFont("Share Tech Mono"), fontProvider = provider))
val Sora = FontFamily(
    Font(googleFont = GoogleFont("Sora"), fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("Sora"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Sora"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Sora"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Sora"), fontProvider = provider, weight = FontWeight.Bold),
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
