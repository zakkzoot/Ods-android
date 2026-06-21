package com.ods.dashboard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * ODS dark theme. The site is dark-only (charcoal void) — there is no light mode.
 * Crimson is the single accent by default; it can be overridden by the user's chosen
 * [accent] in the Customise screen. Everything else stays charcoal / graphite / silver.
 */
@Composable
fun OdsTheme(accent: Color = OdsColors.Crimson, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = OdsColors.Silver,
            background = OdsColors.Charcoal,
            onBackground = OdsColors.Silver,
            surface = OdsColors.Graphite,
            onSurface = OdsColors.Silver,
            surfaceVariant = OdsColors.Graphite,
            onSurfaceVariant = OdsColors.SilverDim,
            outline = OdsColors.SilverFaint,
            error = OdsColors.Crimson,
        ),
        typography = OdsTypography,
        content = content,
    )
}

/** Tile corner radius + the "floating above a void" elevation, mirroring the site's .elev-card. */
object OdsShapes {
    val Tile = RoundedCornerShape(14.dp)
    val Bubble = RoundedCornerShape(50)
}
