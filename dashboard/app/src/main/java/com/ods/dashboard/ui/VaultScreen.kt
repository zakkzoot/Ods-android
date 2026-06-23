package com.ods.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ods.dashboard.data.KbData
import com.ods.dashboard.data.KbEntry
import com.ods.dashboard.data.KbClient
import com.ods.dashboard.data.LocalAppearance
import com.ods.dashboard.data.SecureConfig
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.odsTile
import com.ods.dashboard.util.balanced
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Knowledge Base browser — reads the ODS Vault `kb` edge function and shows entries,
 * filterable by category and free-text search, each expandable to its question / answer /
 * content. The data is served securely from Supabase (the app holds only the anon key).
 */
@Composable
fun VaultScreen(config: SecureConfig, onBack: () -> Unit) {
    var data by remember { mutableStateOf<KbData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        loading = true
        data = withContext(Dispatchers.IO) { KbClient(config).fetch() }
        loading = false
    }

    val entries = data?.entries.orEmpty().filter { e ->
        (category == null || e.category == category) &&
            (query.isBlank() || listOfNotNull(e.title, e.question, e.answer, e.content).any { it.contains(query, ignoreCase = true) })
    }

    Column(modifier = Modifier.fillMaxSize().background(OdsColors.Charcoal).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OdsColors.Silver)
            }
            Text("KNOWLEDGE BASE", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                "${data?.entries?.size ?: 0}",
                style = MaterialTheme.typography.labelMedium,
                color = OdsColors.SilverFaint,
            )
            IconButton(onClick = { refreshKey++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = OdsColors.Silver)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search the knowledge base") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = vaultFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // Category filter chips
        data?.categories?.takeIf { it.isNotEmpty() }?.let { cats ->
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip("All", selected = category == null) { category = null }
                cats.forEach { c ->
                    Chip("${c.category} ${c.count}", selected = category == c.category) {
                        category = if (category == c.category) null else c.category
                    }
                }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LocalAppearance.current.accent)
            }
            data?.error == "no_key" -> VaultMessage(
                "Add your Supabase anon key in Settings (or import a .env) to load the knowledge base.",
            )
            data?.error != null -> VaultMessage("Couldn't load the knowledge base (${data?.error}). Pull to retry with the refresh icon.")
            entries.isEmpty() -> VaultMessage(if (query.isBlank() && category == null) "No entries yet." else "No matching entries.")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    KbCard(
                        entry = entry,
                        expanded = expandedId == entry.id,
                        onClick = { expandedId = if (expandedId == entry.id) null else entry.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun KbCard(entry: KbEntry, expanded: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().odsTile().clickable { onClick() }.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(entry.title, style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.category.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalAppearance.current.accent)
            entry.status?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = OdsColors.SilverFaint) }
        }
        if (!expanded) {
            val preview = entry.question ?: entry.content ?: entry.answer
            preview?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, color = OdsColors.SilverDim)
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                entry.question?.let { Field("QUESTION", it) }
                entry.answer?.let { Field("ANSWER", it) }
                entry.content?.let { Field("CONTENT", it) }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = OdsColors.SilverFaint)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAppearance.current.accent
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else OdsColors.Graphite)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            balanced(label, 24),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) OdsColors.Silver else OdsColors.SilverDim,
        )
    }
}

@Composable
private fun VaultMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OdsColors.SilverDim)
    }
}

@Composable
private fun vaultFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OdsColors.Crimson,
    unfocusedBorderColor = OdsColors.SilverFaint,
    focusedTextColor = OdsColors.Silver,
    unfocusedTextColor = OdsColors.Silver,
    focusedLabelColor = OdsColors.Crimson,
    unfocusedLabelColor = OdsColors.SilverFaint,
    cursorColor = OdsColors.Crimson,
)
