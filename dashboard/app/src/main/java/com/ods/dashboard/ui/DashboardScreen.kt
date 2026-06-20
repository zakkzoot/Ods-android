package com.ods.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ods.dashboard.data.ConnectionRepository
import com.ods.dashboard.model.Band
import com.ods.dashboard.model.Connection
import com.ods.dashboard.model.Connections
import com.ods.dashboard.ui.theme.OdsColors

/**
 * Full homepage-size dashboard. A scrollable grid of every connection, grouped into
 * three labelled bands. Two-stage tap: first tap expands the status popup; second tap
 * (on the already-expanded tile) opens the site/link.
 */
@Composable
fun DashboardScreen(
    repository: ConnectionRepository,
    focusId: String? = null,
    onOpen: (Connection) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val statuses by repository.statuses.collectAsState(initial = emptyMap())
    var expandedId by remember { mutableStateOf(focusId) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = Modifier.fillMaxSize().background(OdsColors.Charcoal),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        header("OUTLINED DESIGN SOLUTIONS · CONNECTIONS", onOpenSettings)

        band("SOCIALS", Band.SOCIALS, statuses, expandedId,
            onTap = { c -> expandedId = toggle(expandedId, c, onOpen) })
        band("SYSTEMS", Band.SYSTEMS, statuses, expandedId,
            onTap = { c -> expandedId = toggle(expandedId, c, onOpen) })
        band("ACCOUNTS", Band.ACCOUNTS, statuses, expandedId,
            onTap = { c -> expandedId = toggle(expandedId, c, onOpen) })
    }
}

/** First tap expands; tapping the already-expanded tile opens it. */
private fun toggle(current: String?, c: Connection, onOpen: (Connection) -> Unit): String? =
    if (current == c.id) { onOpen(c); current } else c.id

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.header(
    text: String,
    onOpenSettings: () -> Unit,
) {
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    "live status of every ODS surface",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            }
            androidx.compose.material3.IconButton(onClick = onOpenSettings) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = OdsColors.Silver,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.band(
    title: String,
    band: Band,
    statuses: Map<String, com.ods.dashboard.data.ConnectionStatus>,
    expandedId: String?,
    onTap: (Connection) -> Unit,
) {
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
        androidx.compose.material3.Text(
            title,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
        )
    }
    items(Connections.inBand(band), key = { it.id }) { c ->
        ConnectionTile(
            connection = c,
            status = statuses[c.id],
            expanded = expandedId == c.id,
            onTap = { onTap(c) },
        )
    }
}
