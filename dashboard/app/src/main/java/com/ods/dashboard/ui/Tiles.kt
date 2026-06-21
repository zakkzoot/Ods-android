package com.ods.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ods.dashboard.data.ConnectionStatus
import com.ods.dashboard.data.Figure
import com.ods.dashboard.data.Health
import com.ods.dashboard.data.LocalAppearance
import com.ods.dashboard.data.rememberFileBitmap
import com.ods.dashboard.model.Connection
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.util.balanced
import com.ods.dashboard.ui.theme.OdsShapes
import com.ods.dashboard.ui.theme.StatNumberStyle
import com.ods.dashboard.ui.theme.odsTile

fun Health.dot(): Color = when (this) {
    Health.UP -> OdsColors.StatusUp
    Health.DEGRADED -> OdsColors.StatusDegraded
    Health.DOWN -> OdsColors.StatusDown
    Health.UNKNOWN -> OdsColors.StatusUnknown
}

/** The red/amber/green status light. */
@Composable
fun StatusLight(health: Health, size: Int = 10) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(health.dot()),
    )
}

/** A normal-app notification bubble (brand accent), hidden when count is zero. */
@Composable
fun NotificationBubble(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val accent = LocalAppearance.current.accent
    Box(
        modifier = modifier
            .clip(OdsShapes.Bubble)
            .background(accent)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center,
        )
    }
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
    val customIcon = rememberFileBitmap(LocalAppearance.current.iconPaths[connection.id])
    Column(
        modifier = modifier
            .odsTile()
            .clickable { onTap() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            // Logo chip: custom image > bundled drawable > monogram
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OdsColors.Charcoal),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    customIcon != null -> Image(
                        bitmap = customIcon, contentDescription = connection.label,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    connection.iconRes != 0 -> Image(
                        painter = painterResource(connection.iconRes), contentDescription = connection.label,
                        modifier = Modifier.fillMaxSize(0.7f),
                    )
                    else -> Text(connection.monogram, style = StatNumberStyle.copy(fontSize = 18.sp))
                }
            }
            // Status light (top-start)
            status?.let {
                Box(modifier = Modifier.align(Alignment.TopStart)) { StatusLight(it.health) }
            }
            // Notification bubble (top-end)
            NotificationBubble(
                status?.badge ?: 0,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        Text(
            balanced(connection.label),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
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
    val accent = LocalAppearance.current.accent
    // No fillMaxWidth: the popup wraps its content, so its width tracks the longest line.
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(OdsColors.Charcoal)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val figures: List<Figure> = status?.figures ?: listOf(Figure("status", "no data yet"))
        figures.forEach { f ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(balanced(f.label.uppercase()), style = MaterialTheme.typography.labelMedium)
                Text(balanced(f.value), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            balanced("tap again to open"),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
