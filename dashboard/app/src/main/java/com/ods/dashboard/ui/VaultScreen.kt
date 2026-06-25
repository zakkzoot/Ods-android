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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ods.dashboard.data.KbClient
import com.ods.dashboard.data.KbClientRef
import com.ods.dashboard.data.KbData
import com.ods.dashboard.data.KbEntry
import com.ods.dashboard.data.LocalAppearance
import com.ods.dashboard.data.SecureConfig
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.odsTile
import com.ods.dashboard.util.balanced
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Knowledge Base browser + editor. Reads the ODS Vault `kb` function and, when an admin
 * token is configured (Settings → Supabase admin token), lets the operator create, edit,
 * archive, and restore entries via the admin-gated `kb-write` function.
 */
@Composable
fun VaultScreen(config: SecureConfig, onBack: () -> Unit) {
    val kb = remember { KbClient(config) }
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<KbData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var editorFor by remember { mutableStateOf<KbEntry?>(null) } // entry to edit
    var creating by remember { mutableStateOf(false) }
    val canEdit = remember(refreshKey) { kb.canEdit() }

    LaunchedEffect(refreshKey) {
        loading = true
        data = withContext(Dispatchers.IO) { kb.fetch() }
        loading = false
    }

    val entries = data?.entries.orEmpty().filter { e ->
        (category == null || e.category == category) &&
            (query.isBlank() || listOfNotNull(e.title, e.question, e.answer, e.content).any { it.contains(query, ignoreCase = true) })
    }

    Box(modifier = Modifier.fillMaxSize().background(OdsColors.Charcoal)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OdsColors.Silver)
                }
                Text("KNOWLEDGE BASE", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text("${data?.entries?.size ?: 0}", style = MaterialTheme.typography.labelMedium, color = OdsColors.SilverFaint)
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
                data?.error == "no_key" -> VaultMessage("Add your Supabase anon key in Settings (or import a .env) to load the knowledge base.")
                data?.error != null -> VaultMessage("Couldn't load the knowledge base (${data?.error}). Tap the refresh icon to retry.")
                entries.isEmpty() -> VaultMessage(if (query.isBlank() && category == null) "No entries yet." else "No matching entries.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        KbCard(
                            entry = entry,
                            expanded = expandedId == entry.id,
                            canEdit = canEdit,
                            onClick = { expandedId = if (expandedId == entry.id) null else entry.id },
                            onEdit = { editorFor = entry },
                            onArchive = {
                                scope.launch {
                                    val r = withContext(Dispatchers.IO) { kb.archive(entry.id) }
                                    if (r.ok) refreshKey++
                                }
                            },
                        )
                    }
                }
            }
        }

        if (canEdit && !loading) {
            FloatingActionButton(
                onClick = { creating = true },
                containerColor = LocalAppearance.current.accent,
                contentColor = OdsColors.Silver,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New entry")
            }
        }
    }

    if (creating) {
        KbEditor(
            entry = null,
            clients = data?.clients.orEmpty(),
            kb = kb,
            onDismiss = { creating = false },
            onSaved = { creating = false; refreshKey++ },
        )
    }
    editorFor?.let { entry ->
        KbEditor(
            entry = entry,
            clients = data?.clients.orEmpty(),
            kb = kb,
            onDismiss = { editorFor = null },
            onSaved = { editorFor = null; refreshKey++ },
        )
    }
}

@Composable
private fun KbCard(
    entry: KbEntry,
    expanded: Boolean,
    canEdit: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
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
            preview?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, color = OdsColors.SilverDim) }
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                entry.question?.let { Field("QUESTION", it) }
                entry.answer?.let { Field("ANSWER", it) }
                entry.content?.let { Field("CONTENT", it) }
                if (canEdit) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onEdit) { Text("EDIT", color = LocalAppearance.current.accent) }
                        TextButton(onClick = onArchive) { Text("ARCHIVE", color = OdsColors.SilverDim) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KbEditor(
    entry: KbEntry?,
    clients: List<KbClientRef>,
    kb: KbClient,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val isCreate = entry == null
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(entry?.title ?: "") }
    var category by remember { mutableStateOf(entry?.category?.takeIf { it != "Uncategorised" } ?: "") }
    var question by remember { mutableStateOf(entry?.question ?: "") }
    var answer by remember { mutableStateOf(entry?.answer ?: "") }
    var content by remember { mutableStateOf(entry?.content ?: "") }
    var status by remember { mutableStateOf(entry?.status ?: "active") }
    var clientId by remember { mutableStateOf(clients.firstOrNull()?.id) }
    var clientMenu by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSave = title.isNotBlank() && (!isCreate || clientId != null) && !saving

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    saving = true
                    error = null
                    val fields = mapOf(
                        "title" to title.trim(),
                        "category" to category.trim().ifBlank { null },
                        "question" to question.trim().ifBlank { null },
                        "answer" to answer.trim().ifBlank { null },
                        "content" to content.trim().ifBlank { null },
                        "status" to status.trim().ifBlank { null },
                    )
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            if (isCreate) kb.create(clientId!!, fields) else kb.update(entry!!.id, fields)
                        }
                        saving = false
                        if (r.ok) onSaved() else error = r.error ?: "save failed"
                    }
                },
            ) { Text(if (saving) "SAVING…" else "SAVE", color = LocalAppearance.current.accent) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("CANCEL", color = OdsColors.SilverDim) } },
        title = { Text(if (isCreate) "New entry" else "Edit entry", style = MaterialTheme.typography.titleLarge) },
        containerColor = OdsColors.Graphite,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = OdsColors.Crimson) }
                if (isCreate && clients.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { clientMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            val sel = clients.firstOrNull { it.id == clientId }
                            Text("${sel?.emoji ?: ""} ${sel?.name ?: "Select client"}".trim(), color = OdsColors.Silver)
                        }
                        DropdownMenu(expanded = clientMenu, onDismissRequest = { clientMenu = false }) {
                            clients.forEach { c ->
                                DropdownMenuItem(text = { Text("${c.emoji ?: ""} ${c.name}".trim()) }, onClick = { clientId = c.id; clientMenu = false })
                            }
                        }
                    }
                }
                EditorField("Title", title, { title = it })
                EditorField("Category", category, { category = it })
                EditorField("Question", question, { question = it })
                EditorField("Answer", answer, { answer = it })
                EditorField("Content", content, { content = it })
                EditorField("Status", status, { status = it })
            }
        },
    )
}

@Composable
private fun EditorField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        colors = vaultFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
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
