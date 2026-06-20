package com.ods.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ods.dashboard.data.ConnectionStatus
import com.ods.dashboard.data.Figure
import com.ods.dashboard.data.Health
import com.ods.dashboard.model.Connection
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.OdsShapes
import com.ods.dashboard.ui.theme.StatNumberStyle
import com.ods.dashboard.ui.theme.odsTile

fun Health.dot(): Color = when (this) {
    Health.UP -> OdsColors.StatusUp
    Health.DEGRADED -> OdsColors.StatusDegraded
    Health.DOWN -> OdsColors.StatusDown
    Health.UNKNOWN -> OdsColors.StatusUnknown
}

/**
 * A connection tile. First tap toggles the inline status popup (handled by parent
 * via [expanded] + [onTap]); when expanded, the figures + an "open" affordance show.
 * The parent navigates on the second tap.
 */
@Composable
fun ConnectionTile(
    connection: Connection,
    status: ConnectionStatus?,
    expanded: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .odsTile()
            .clickable { onTap() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            // Monogram logo chip (swap for a real drawable via connection.iconRes)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OdsColors.Charcoal),
                contentAlignment = Alignment.Center,
            ) {
                Text(connection.monogram, style = StatNumberStyle.copy(fontSize = 18.sp))
            }
            // Status dot (top-start)
            status?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(it.health.dot()),
                )
            }
            // Notification bubble (top-end), hidden when zero
            val badge = status?.badge ?: 0
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(OdsShapes.Bubble)
                        .background(OdsColors.Crimson)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        if (badge > 99) "99+" else badge.toString(),
                        color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Text(
            connection.label,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = expanded) {
            StatusPopup(status)
        }
    }
}

@Composable
private fun StatusPopup(status: ConnectionStatus?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(OdsColors.Charcoal)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val figures: List<Figure> = status?.figures ?: listOf(Figure("status", "no data yet"))
        figures.forEach { f ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    f.label.uppercase(),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
                Text(f.value, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            "tap again to open",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = OdsColors.Crimson,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
