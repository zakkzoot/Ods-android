package com.ods.dashboard.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ods.dashboard.data.AssistClient
import com.ods.dashboard.data.AssistEnvelope
import com.ods.dashboard.data.SecureConfig
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.odsTile
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ODS Assist — the governed assistant, on the phone. Sign in once (Supabase Auth against the
 * ODS IdP), then talk. Each reply is tagged with HOW it was answered: `command · <intent>` means
 * the deterministic layer answered from real OiS data with no model call; `delegated` means the
 * governed LLM loop composed it. The governance line under the reply is the envelope the turn ran
 * under — presentation never changes authority.
 */

private data class AssistMsg(
    val fromUser: Boolean,
    val text: String,
    val route: String? = null,
    val intent: String? = null,
    val envelope: AssistEnvelope? = null,
)

@Composable
fun AssistScreen(config: SecureConfig, onBack: () -> Unit) {
    val client = remember { AssistClient(config) }
    var signedIn by remember { mutableStateOf(client.signedIn) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OdsColors.Charcoal)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OdsColors.Silver)
            }
            Text("ODS Assist", style = MaterialTheme.typography.titleLarge, color = OdsColors.Silver)
            Spacer(Modifier.weight(1f))
            if (signedIn) {
                TextButton(onClick = { client.signOut(); signedIn = false }) {
                    Text("sign out", color = OdsColors.SilverFaint, fontSize = 12.sp)
                }
            }
        }
        if (signedIn) {
            AssistChat(client, modifier = Modifier.weight(1f))
        } else {
            AssistLogin(client, onSignedIn = { signedIn = true }, modifier = Modifier.weight(1f))
        }
    }
}

// ── Sign-in (first run: endpoints + credentials; after that just credentials) ──

@Composable
private fun AssistLogin(client: AssistClient, onSignedIn: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var consoleUrl by remember { mutableStateOf(client.consoleUrl ?: "") }
    var authUrl by remember { mutableStateOf(client.authUrl ?: "") }
    var anonKey by remember { mutableStateOf(client.anonKey ?: "") }
    var email by remember { mutableStateOf(client.email ?: "") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Sign in to your ODS account. The assistant acts only as you — it can never see " +
                "or do more than your own access allows.",
            color = OdsColors.SilverDim,
            fontSize = 13.sp,
        )
        AssistField(consoleUrl, { consoleUrl = it }, "Assist console URL (https://…)")
        AssistField(authUrl, { authUrl = it }, "Auth URL (https://<ref>.supabase.co)")
        AssistField(anonKey, { anonKey = it }, "Publishable anon key")
        AssistField(email, { email = it }, "Email", keyboard = KeyboardType.Email)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = OdsColors.SilverFaint) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = assistFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = OdsColors.Crimson, fontSize = 13.sp) }
        OutlinedButton(
            enabled = !busy && consoleUrl.isNotBlank() && authUrl.isNotBlank() &&
                anonKey.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            onClick = {
                busy = true
                error = null
                client.saveEndpoints(consoleUrl, authUrl, anonKey)
                scope.launch {
                    client.signIn(email.trim(), password)
                        .onSuccess { onSignedIn() }
                        .onFailure { error = it.message ?: "Sign-in failed" }
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) CircularProgressIndicator(modifier = Modifier.width(18.dp), strokeWidth = 2.dp)
            else Text("Sign in", color = OdsColors.Silver)
        }
    }
}

@Composable
private fun AssistField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Uri,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = OdsColors.SilverFaint) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = true,
        colors = assistFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun assistFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OdsColors.Silver,
    unfocusedTextColor = OdsColors.Silver,
    focusedBorderColor = OdsColors.Crimson,
    unfocusedBorderColor = OdsColors.HairlineLight,
    cursorColor = OdsColors.Crimson,
)

// ── The chat ──────────────────────────────────────────────────────────────────

@Composable
private fun AssistChat(client: AssistClient, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<AssistMsg>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var speak by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // On-device voice out (free, no service). Initialised lazily; shut down with the screen.
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) tts.value?.language = Locale.UK
        }
        tts.value = t
        onDispose { t.shutdown() }
    }

    fun send(text: String) {
        val q = text.trim()
        if (q.isEmpty() || busy) return
        messages += AssistMsg(fromUser = true, text = q)
        input = ""
        busy = true
        scope.launch {
            client.chat(q)
                .onSuccess { r ->
                    messages += AssistMsg(false, r.answer, r.route, r.intent, r.envelope)
                    if (speak) tts.value?.speak(r.answer, TextToSpeech.QUEUE_FLUSH, null, "assist")
                }
                .onFailure { messages += AssistMsg(false, it.message ?: "Request failed") }
            busy = false
        }
    }

    // Voice in via the system speech recogniser (free; no RECORD_AUDIO permission needed —
    // the recogniser activity holds the mic, not this app).
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.let { send(it) }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Ask your OiS. \"source health\", \"recurring complaints\", " +
                            "\"remind me to…\" answer deterministically; anything open-ended " +
                            "goes to the governed model.",
                        color = OdsColors.SilverFaint,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            itemsIndexed(messages) { _, m -> AssistBubble(m) }
            if (busy) {
                item {
                    Row(modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.width(18.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = { speak = !speak }) {
                Icon(
                    if (speak) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = if (speak) "Voice replies on" else "Voice replies off",
                    tint = if (speak) OdsColors.Crimson else OdsColors.SilverFaint,
                )
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask your assistant…", color = OdsColors.SilverFaint) },
                colors = assistFieldColors(),
                modifier = Modifier.weight(1f),
                maxLines = 3,
            )
            IconButton(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to ODS Assist")
                }
                runCatching { speechLauncher.launch(intent) }
            }) {
                Icon(Icons.Filled.Mic, contentDescription = "Speak", tint = OdsColors.Silver)
            }
            IconButton(onClick = { send(input) }, enabled = !busy) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = OdsColors.Crimson)
            }
        }
    }
}

@Composable
private fun AssistBubble(m: AssistMsg) {
    Column(
        horizontalAlignment = if (m.fromUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .odsTile()
                .padding(10.dp),
        ) {
            if (!m.fromUser && m.route != null) {
                Text(
                    if (m.route == "deterministic") "command · ${m.intent ?: "?"}" else "delegated · model",
                    color = if (m.route == "deterministic") OdsColors.Crimson else OdsColors.SilverFaint,
                    fontSize = 10.sp,
                )
            }
            Text(m.text, color = if (m.fromUser) OdsColors.SilverDim else OdsColors.Silver, fontSize = 14.sp)
            m.envelope?.let { e ->
                Text(
                    "${e.orgType} · ${e.membershipRole} · tier ${e.effectiveTier} · outward approval " +
                        (if (e.outwardActionApprovalRequired) "required" else "off"),
                    color = OdsColors.SilverFaint,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
