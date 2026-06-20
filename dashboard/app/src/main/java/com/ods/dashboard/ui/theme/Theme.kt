package com.ods.dashboard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * ODS dark theme. The site is dark-only (charcoal void) — there is no light mode.
 * Crimson is the single accent; everything else is charcoal / graphite / silver.
 */
private val OdsColorScheme = darkColorScheme(
    primary = OdsColors.Crimson,
    onPrimary = OdsColors.Silver,
    background = OdsColors.Charcoal,
    onBackground = OdsColors.Silver,
    surface = OdsColors.Graphite,
    onSurface = OdsColors.Silver,
    surfaceVariant = OdsColors.Graphite,
    onSurfaceVariant = OdsColors.SilverDim,
    outline = OdsColors.SilverFaint,
    error = OdsColors.Crimson,
)

@Composable
fun OdsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OdsColorScheme,
        typography = OdsTypography,
        content = content,
    )
}

/** Tile corner radius + the "floating above a void" elevation, mirroring the site's .elev-card. */
object OdsShapes {
    val Tile = RoundedCornerShape(14.dp)
    val Bubble = RoundedCornerShape(50)
}
