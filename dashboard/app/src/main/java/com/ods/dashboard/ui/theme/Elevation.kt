package com.ods.dashboard.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/**
 * Drop-in tile styling that reproduces the site's .elev-card: a graphite panel that
 * reads as floating above the charcoal void — soft drop shadow, a faint crimson glow,
 * and a hairline top highlight for edge thickness.
 *
 * Usage: Box(modifier = Modifier.odsTile().padding(16.dp)) { ... }
 */
fun Modifier.odsTile(): Modifier = this
    .shadow(
        elevation = 24.dp,
        shape = OdsShapes.Tile,
        ambientColor = OdsColors.CrimsonGlow,
        spotColor = OdsColors.CrimsonGlow,
    )
    .clip(OdsShapes.Tile)
    .background(OdsColors.Graphite)
    .border(width = 1.dp, color = OdsColors.HairlineLight, shape = OdsShapes.Tile)
