package com.ods.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ods.dashboard.data.AppearanceStore
import com.ods.dashboard.data.LocalAppearance
import com.ods.dashboard.data.rememberFileBitmap
import com.ods.dashboard.model.Category
import com.ods.dashboard.model.Connections
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.StatNumberStyle
import com.ods.dashboard.ui.theme.odsTile
import com.ods.dashboard.util.balanced
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/**
 * Arrange mode for a category: long-press a tile to drag it into a new order, tap the
 * resize control to toggle a tile between single and double width. Order + spans persist
 * to [AppearanceStore] immediately.
 */
@Composable
fun CategoryArrange(
    category: Category,
    store: AppearanceStore,
    onChanged: () -> Unit,
    onDone: () -> Unit,
) {
    val appearance = LocalAppearance.current
    val baseIds = remember(category) { Connections.inCategory(category).map { it.id } }
    var ids by remember(category) { mutableStateOf(appearance.orderedTiles(category.name, baseIds)) }
    var spans by remember(category) { mutableStateOf(baseIds.associateWith { appearance.spanOf(it) }) }

    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        ids = ids.toMutableList().apply { add(to.index, removeAt(from.index)) }
        store.setTileOrder(category.name, ids)
        onChanged()
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Done", tint = OdsColors.Silver)
            }
            Text(
                "ARRANGE · ${category.title}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { store.resetLayout(); onChanged(); onDone() }) { Text("Reset") }
        }
        Text(
            "Long-press a tile to drag it. Tap the ⤢ corner to resize.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 104.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ids, key = { it }, span = { GridItemSpan((spans[it] ?: 1).coerceIn(1, 2)) }) { id ->
                val c = Connections.byId(id) ?: return@items
                ReorderableItem(reorderState, key = id) { isDragging ->
                    val icon = rememberFileBitmap(appearance.iconPaths[id])
                    ArrangeTile(
                        label = c.label,
                        monogram = c.monogram,
                        icon = icon,
                        dragging = isDragging,
                        modifier = Modifier.longPressDraggableHandle(),
                        onResize = {
                            val next = if ((spans[id] ?: 1) >= 2) 1 else 2
                            spans = spans.toMutableMap().apply { put(id, next) }
                            store.setTileSpan(id, next)
                            onChanged()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArrangeTile(
    label: String,
    monogram: String,
    icon: ImageBitmap?,
    dragging: Boolean,
    modifier: Modifier,
    onResize: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .odsTile()
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OdsColors.Charcoal),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    androidx.compose.foundation.Image(
                        bitmap = icon, contentDescription = label,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(monogram, style = StatNumberStyle.copy(fontSize = 18.sp))
                }
            }
            Text(
                balanced(label),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Resize control (top-end)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OdsColors.Graphite)
                .clickable { onResize() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.OpenInFull,
                contentDescription = "Resize",
                tint = LocalAppearance.current.accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
