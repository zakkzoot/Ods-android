package com.ods.dashboard.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ods.dashboard.data.AppearanceStore
import com.ods.dashboard.data.LocalAppearance
import com.ods.dashboard.model.Connections
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.odsTile

private sealed interface PickTarget {
    data object Background : PickTarget
    data object Logo : PickTarget
    data class Icon(val id: String) : PickTarget
}

private val accentPresets = listOf(
    "ODS Crimson" to "#D42B2B",
    "Amber" to "#D9A23B",
    "Green" to "#3FB950",
    "Blue" to "#3B82F6",
    "Violet" to "#8B5CF6",
    "Silver" to "#C9CDD4",
)

/**
 * Make it yours: brand accent, background image + crimson-overlay strength, header logo,
 * and per-connection tile icons. Changes write to [AppearanceStore] and call [onChanged]
 * so the host reloads the live [com.ods.dashboard.data.Appearance].
 */
@Composable
fun CustomizeScreen(
    store: AppearanceStore,
    onBack: () -> Unit,
    onChanged: () -> Unit,
) {
    val appearance = LocalAppearance.current
    var accentText by remember { mutableStateOf(appearance.accentHex) }
    var pending by remember { mutableStateOf<PickTarget?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = pending
        pending = null
        if (uri != null && target != null) {
            val name = when (target) {
                PickTarget.Background -> "bg"
                PickTarget.Logo -> "logo"
                is PickTarget.Icon -> "icon_${target.id}"
            }
            val path = store.importImage(uri, name)
            if (path != null) {
                when (target) {
                    PickTarget.Background -> { store.setBackground(path); store.setUseDefaultHero(false) }
                    PickTarget.Logo -> store.setLogo(path)
                    is PickTarget.Icon -> store.setIcon(target.id, path)
                }
                onChanged()
            }
        }
    }

    fun pick(target: PickTarget) { pending = target; picker.launch("image/*") }

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
            Text("CUSTOMISE", style = MaterialTheme.typography.labelLarge)
        }

        // --- Accent ---
        Section("ACCENT COLOUR") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                accentPresets.forEach { (_, hex) ->
                    val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(OdsColors.Crimson)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(2.dp, OdsColors.HairlineLight, CircleShape)
                            .clickable { accentText = hex; store.setAccent(hex); onChanged() },
                    )
                }
            }
            OutlinedTextField(
                value = accentText,
                onValueChange = { accentText = it },
                label = { Text("Hex (e.g. #D42B2B)") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { store.setAccent(accentText.trim()); onChanged() },
                colors = ButtonDefaults.buttonColors(containerColor = OdsColors.Crimson, contentColor = OdsColors.Silver),
            ) { Text("Apply colour") }
        }

        // --- Background ---
        Section("BACKGROUND") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Use bundled ODS hero", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = appearance.useDefaultHero && appearance.backgroundPath == null,
                    onCheckedChange = { on -> store.setUseDefaultHero(on); if (on) store.setBackground(null); onChanged() },
                    colors = SwitchDefaults.colors(checkedTrackColor = OdsColors.Crimson),
                )
            }
            Text("Crimson overlay strength", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = appearance.overlayAlpha,
                onValueChange = { store.setOverlay(it); onChanged() },
                valueRange = 0f..1f,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { pick(PickTarget.Background) },
                    colors = ButtonDefaults.buttonColors(containerColor = OdsColors.Crimson, contentColor = OdsColors.Silver),
                ) { Text("Choose image") }
                OutlinedButton(onClick = { store.setBackground(null); store.setUseDefaultHero(true); onChanged() }) {
                    Text("Clear")
                }
            }
        }

        // --- Logo ---
        Section("LOGO") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { pick(PickTarget.Logo) },
                    colors = ButtonDefaults.buttonColors(containerColor = OdsColors.Crimson, contentColor = OdsColors.Silver),
                ) { Text("Choose logo") }
                OutlinedButton(onClick = { store.setLogo(null); onChanged() }) { Text("Use ODS mark") }
            }
        }

        // --- Per-connection icons ---
        Section("TILE ICONS") {
            Connections.all.forEach { c ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(c.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    val hasIcon = appearance.iconPaths.containsKey(c.id)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { pick(PickTarget.Icon(c.id)) }) { Text(if (hasIcon) "Change" else "Set") }
                        if (hasIcon) OutlinedButton(onClick = { store.setIcon(c.id, null); onChanged() }) { Text("Clear") }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { store.resetAll(); accentText = "#D42B2B"; onChanged() }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset all customisation")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().odsTile().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        content()
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OdsColors.Crimson,
    unfocusedBorderColor = OdsColors.SilverFaint,
    focusedTextColor = OdsColors.Silver,
    unfocusedTextColor = OdsColors.Silver,
    focusedLabelColor = OdsColors.Crimson,
    unfocusedLabelColor = OdsColors.SilverFaint,
    cursorColor = OdsColors.Crimson,
)
