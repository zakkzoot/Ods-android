package com.ods.dashboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ods.dashboard.R
import com.ods.dashboard.data.ConnectionRepository
import com.ods.dashboard.data.ConnectionStatus
import com.ods.dashboard.data.Health
import com.ods.dashboard.data.LocalAppearance
import com.ods.dashboard.data.rememberFileBitmap
import com.ods.dashboard.model.Category
import com.ods.dashboard.model.Connection
import com.ods.dashboard.model.Connections
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.odsTile

/** A category's rolled-up state: worst-case light, total unread, and inbox headlines. */
private data class Rollup(val health: Health, val totalBadge: Int, val items: List<String>)

private fun rollup(category: Category, statuses: Map<String, ConnectionStatus>): Rollup {
    val members = Connections.inCategory(category)
    var badge = 0
    val healths = ArrayList<Health>()
    val items = ArrayList<String>()
    members.forEach { c ->
        val s = statuses[c.id]
        badge += s?.badge ?: 0
        healths += s?.health ?: Health.UNKNOWN
        when {
            s != null && s.items.isNotEmpty() -> s.items.take(3).forEach { items += it }
            (s?.badge ?: 0) > 0 -> items += "${c.label}: ${s!!.badge} new"
        }
    }
    val health = when {
        healths.any { it == Health.DOWN } -> Health.DOWN
        healths.any { it == Health.DEGRADED } -> Health.DEGRADED
        healths.any { it == Health.UP } -> Health.UP
        else -> Health.UNKNOWN
    }
    return Rollup(health, badge, items)
}

/**
 * Home = four category cards, each a collective inbox for its connections. Tapping a card
 * drills into a grid of that category's tiles (status light + notification bubble each).
 * The crimson hero sits behind everything; the ODS logo brands the header.
 */
@Composable
fun DashboardScreen(
    repository: ConnectionRepository,
    focusId: String? = null,
    onOpen: (Connection) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val statuses by repository.statuses.collectAsState(initial = emptyMap())
    var selected by remember { mutableStateOf(focusId?.let { Connections.byId(it)?.category }) }
    var expandedId by remember { mutableStateOf(focusId) }

    DashboardBackground {
        val cat = selected
        if (cat == null) {
            HomeCategories(statuses = statuses, onOpenSettings = onOpenSettings, onOpen = { selected = it })
        } else {
            CategoryDetail(
                category = cat,
                statuses = statuses,
                expandedId = expandedId,
                onBack = { selected = null; expandedId = null },
                onTap = { c -> expandedId = toggle(expandedId, c, onOpen) },
            )
        }
    }
}

/** First tap expands; tapping the already-expanded tile opens it. */
private fun toggle(current: String?, c: Connection, onOpen: (Connection) -> Unit): String? =
    if (current == c.id) { onOpen(c); current } else c.id

@Composable
private fun DashboardBackground(content: @Composable BoxScope.() -> Unit) {
    val appearance = LocalAppearance.current
    val customBg = rememberFileBitmap(appearance.backgroundPath)
    Box(modifier = Modifier.fillMaxSize().background(OdsColors.Charcoal)) {
        when {
            customBg != null -> Image(
                bitmap = customBg, contentDescription = null,
                modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop, alpha = 0.9f,
            )
            appearance.useDefaultHero -> Image(
                painter = painterResource(R.drawable.ods_hero), contentDescription = null,
                modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop, alpha = 0.9f,
            )
        }
        // Crimson scrim over the background, behind the UI — the site's red overlay.
        Box(
            modifier = Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    listOf(
                        OdsColors.Charcoal.copy(alpha = 0.86f),
                        appearance.accent.copy(alpha = appearance.overlayAlpha * 0.45f),
                        OdsColors.Charcoal.copy(alpha = 0.94f),
                    ),
                ),
            ),
        )
        content()
    }
}

@Composable
private fun HomeCategories(
    statuses: Map<String, ConnectionStatus>,
    onOpenSettings: () -> Unit,
    onOpen: (Category) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(onOpenSettings)
        Category.entries.forEach { cat ->
            CategoryCard(category = cat, rollup = rollup(cat, statuses), onClick = { onOpen(cat) })
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    val logo = rememberFileBitmap(LocalAppearance.current.logoPath)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (logo != null) {
            Image(bitmap = logo, contentDescription = "ODS", modifier = Modifier.size(40.dp))
        } else {
            Image(painter = painterResource(R.drawable.ic_ods_logo), contentDescription = "ODS", modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("OUTLINED DESIGN", style = MaterialTheme.typography.titleLarge)
            Text("CONNECTIONS", style = MaterialTheme.typography.labelLarge)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = OdsColors.Silver)
        }
    }
}

@Composable
private fun CategoryCard(category: Category, rollup: Rollup, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .odsTile()
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusLight(rollup.health, size = 12)
                Text(category.title, style = MaterialTheme.typography.headlineMedium)
            }
            NotificationBubble(rollup.totalBadge)
        }
        Text(
            Connections.inCategory(category).joinToString("  ·  ") { it.label },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
        )
        if (rollup.items.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                rollup.items.take(3).forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CategoryDetail(
    category: Category,
    statuses: Map<String, ConnectionStatus>,
    expandedId: String?,
    onBack: () -> Unit,
    onTap: (Connection) -> Unit,
) {
    val roll = rollup(category, statuses)
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OdsColors.Silver)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                StatusLight(roll.health, size = 12)
                Text(category.title, style = MaterialTheme.typography.headlineMedium)
            }
            NotificationBubble(roll.totalBadge)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (roll.items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InboxPanel(roll.items)
                }
            }
            items(Connections.inCategory(category), key = { it.id }) { c ->
                ConnectionTile(
                    connection = c,
                    status = statuses[c.id],
                    expanded = expandedId == c.id,
                    onTap = { onTap(c) },
                )
            }
        }
    }
}

@Composable
private fun InboxPanel(items: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth().odsTile().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("INBOX", style = MaterialTheme.typography.labelLarge)
        items.take(8).forEach { line ->
            Text("• $line", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}
