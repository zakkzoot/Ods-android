package com.ods.dashboard.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ods.dashboard.launcher.LauncherGrid
import com.ods.dashboard.launcher.LauncherStore
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.odsTile
import com.ods.dashboard.widget.OdsLauncherWidget
import kotlinx.coroutines.launch

/**
 * Edit the home-screen launcher grid: swap any slot's mark for your own image and point
 * any slot somewhere else. Defaults live in [LauncherGrid]; everything here is an
 * override, so "Reset" always gets you back to a working grid.
 *
 * Every change redraws the placed widgets immediately — no re-pinning.
 */
@Composable
fun LauncherScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LauncherStore(context) }
    val scope = rememberCoroutineScope()

    // Local mirror of the store so the fields stay editable between saves.
    val urls = remember { mutableStateMapOf<String, String>() }
    val icons = remember { mutableStateMapOf<String, String?>() }
    var pendingIconId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        LauncherGrid.all.forEach { s ->
            urls[s.id] = store.urlOf(s)
            icons[s.id] = store.iconPath(s.id)
        }
    }

    fun redraw() = scope.launch { runCatching { OdsLauncherWidget.refresh(context) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val id = pendingIconId
        pendingIconId = null
        if (uri != null && id != null) {
            val path = store.importIcon(uri, id)
            if (path != null) {
                store.setIcon(id, path)
                icons[id] = path
                redraw()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OdsColors.Charcoal)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OdsColors.Silver)
            }
            Text("LAUNCHER GRID", style = MaterialTheme.typography.labelLarge)
        }

        Text(
            "Twelve slots, four across and three down. Long-press the home screen → " +
                "Widgets → ODS Dashboard → ODS Launcher to place it.",
            style = MaterialTheme.typography.labelMedium,
        )

        LauncherGrid.rows.forEachIndexed { rowIndex, row ->
            Column(
                modifier = Modifier.fillMaxWidth().odsTile().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("ROW ${rowIndex + 1}", style = MaterialTheme.typography.labelLarge)
                row.forEach { s ->
                    val customPath = icons[s.id]
                    val customBitmap = remember(customPath) { LauncherStore.decodeIcon(customPath) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (customBitmap != null) {
                            Image(
                                bitmap = customBitmap.asImageBitmap(),
                                contentDescription = s.label,
                                modifier = Modifier.size(34.dp),
                            )
                        } else {
                            Image(
                                painter = painterResource(s.iconRes),
                                contentDescription = s.label,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(s.label, style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = urls[s.id] ?: s.url,
                                onValueChange = { urls[s.id] = it },
                                label = { Text("Opens") },
                                singleLine = true,
                                colors = launcherFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    store.setUrl(s.id, urls[s.id])
                                    redraw()
                                }) { Text("Save link") }
                                OutlinedButton(onClick = { pendingIconId = s.id; picker.launch("image/*") }) {
                                    Text(if (customPath != null) "Change logo" else "Set logo")
                                }
                                if (customPath != null) {
                                    OutlinedButton(onClick = {
                                        store.setIcon(s.id, null)
                                        icons[s.id] = null
                                        redraw()
                                    }) { Text("Clear") }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = {
                store.resetAll()
                LauncherGrid.all.forEach { s -> urls[s.id] = s.url; icons[s.id] = null }
                redraw()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset launcher to defaults") }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun launcherFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OdsColors.Crimson,
    unfocusedBorderColor = OdsColors.SilverFaint,
    focusedTextColor = OdsColors.Silver,
    unfocusedTextColor = OdsColors.Silver,
    focusedLabelColor = OdsColors.Crimson,
    unfocusedLabelColor = OdsColors.SilverFaint,
    cursorColor = OdsColors.Crimson,
)
