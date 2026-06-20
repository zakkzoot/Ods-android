package com.ods.dashboard.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ods.dashboard.data.GmailAuth
import com.ods.dashboard.data.SecureConfig
import com.ods.dashboard.model.CheckType
import com.ods.dashboard.model.Connections
import com.ods.dashboard.ui.theme.OdsColors
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

/**
 * Token entry + Gmail account linking. Token values are written to the encrypted
 * [SecureConfig]; Gmail uses AppAuth OAuth (see SETUP_GOOGLE_OAUTH.md). Saving triggers
 * an immediate status refresh.
 */
@Composable
fun SettingsScreen(
    config: SecureConfig,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current

    var vercel by remember { mutableStateOf(config.get(SecureConfig.VERCEL_TOKEN).orEmpty()) }
    var github by remember { mutableStateOf(config.get(SecureConfig.GITHUB_PAT).orEmpty()) }
    var supaKey by remember { mutableStateOf(config.get(SecureConfig.SUPABASE_ANON_KEY).orEmpty()) }
    var meta by remember { mutableStateOf(config.get(SecureConfig.META_PAGE_TOKEN).orEmpty()) }
    var clientId by remember { mutableStateOf(config.get(SecureConfig.GOOGLE_CLIENT_ID).orEmpty()) }

    // AppAuth service, disposed with the screen.
    val authService = remember { AuthorizationService(context) }
    DisposableEffect(Unit) { onDispose { authService.dispose() } }

    val gmailAccounts = remember { Connections.all.filter { it.check == CheckType.GMAIL } }
    val connected = remember {
        mutableStateMapOf<String, Boolean>().apply {
            gmailAccounts.forEach { put(it.id, GmailAuth.isConnected(config, it.id)) }
        }
    }
    var connectingId by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        val id = connectingId
        val resp = data?.let { AuthorizationResponse.fromIntent(it) }
        val ex = data?.let { AuthorizationException.fromIntent(it) }
        if (resp != null && id != null) {
            GmailAuth.completeAuthorization(authService, resp, config, id) { ok ->
                connected[id] = ok
                if (ok) onSaved()
            }
        }
        connectingId = null
        if (ex != null && resp == null) { /* user cancelled or error — leave as-is */ }
    }

    fun connect(connectionId: String, email: String) {
        val cid = clientId.trim()
        if (cid.isEmpty()) return
        config.set(SecureConfig.GOOGLE_CLIENT_ID, cid) // ensure latest id is used
        connectingId = connectionId
        val intent = authService.getAuthorizationRequestIntent(GmailAuth.authRequest(cid, email))
        launcher.launch(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OdsColors.Charcoal)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = OdsColors.Silver)
            }
            Text("CONNECTION SETUP", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }
        Text(
            "Stored encrypted on this device. Each is optional — a connection still " +
                "deep-links without it; it just shows an unknown dot until set.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )

        Secret("Vercel API token", vercel) { vercel = it }
        Secret("GitHub personal access token", github) { github = it }
        Secret("Supabase anon key (optional — health works without it)", supaKey) { supaKey = it }
        Secret("Meta page access token (Facebook / Instagram)", meta) { meta = it }

        Spacer(Modifier.height(4.dp))
        Text("EMAIL (GMAIL)", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Text(
            "Enter your Google OAuth client ID (see SETUP_GOOGLE_OAUTH.md), then connect " +
                "each account to show unread counts.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
        Plain("Google OAuth client ID", clientId) { clientId = it }

        gmailAccounts.forEach { acct ->
            val email = acct.url.removePrefix("mailto:")
            val isConnected = connected[acct.id] == true
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(email, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    Text(
                        if (isConnected) "connected" else "not connected",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = if (isConnected) OdsColors.StatusUp else OdsColors.SilverFaint,
                    )
                }
                if (isConnected) {
                    OutlinedButton(onClick = {
                        GmailAuth.disconnect(config, acct.id); connected[acct.id] = false
                    }) { Text("Disconnect") }
                } else {
                    Button(
                        onClick = { connect(acct.id, email) },
                        enabled = clientId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OdsColors.Crimson, contentColor = OdsColors.Silver,
                        ),
                    ) { Text("Connect") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                config.set(SecureConfig.VERCEL_TOKEN, vercel.trim())
                config.set(SecureConfig.GITHUB_PAT, github.trim())
                config.set(SecureConfig.SUPABASE_ANON_KEY, supaKey.trim())
                config.set(SecureConfig.META_PAGE_TOKEN, meta.trim())
                config.set(SecureConfig.GOOGLE_CLIENT_ID, clientId.trim())
                onSaved()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = OdsColors.Crimson, contentColor = OdsColors.Silver,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save and refresh") }
    }
}

@Composable
private fun Secret(label: String, value: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null, tint = OdsColors.SilverFaint,
                )
            }
        },
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Plain(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
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
