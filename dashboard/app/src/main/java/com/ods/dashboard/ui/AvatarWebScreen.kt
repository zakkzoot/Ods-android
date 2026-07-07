package com.ods.dashboard.ui

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ods.dashboard.data.AssistClient
import com.ods.dashboard.data.SecureConfig
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.odsTile

/**
 * ODS Assist's 3D avatar chat (`AvatarStage.tsx` on the deployed console), embedded via WebView
 * rather than reimplemented natively — the console already owns the three.js/WebGL surface and
 * the Supabase session; this screen just points a WebView at it, reusing the same console URL
 * and cookie-based auth [AssistScreen] already established.
 */
@Composable
fun AvatarWebScreen(config: SecureConfig, onBack: () -> Unit) {
    val assistClient = remember { AssistClient(config) }
    val consoleUrl = assistClient.consoleUrl

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Step back through the avatar chat's in-page history before leaving the screen.
    BackHandler(enabled = true) {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(OdsColors.Charcoal)) {
        if (consoleUrl == null || consoleUrl.isBlank()) {
            AvatarMessage(
                headline = "Assist console not configured",
                detail = "Sign in to ODS Assist first — the avatar reuses that same console URL, " +
                    "there's no separate setup for it.",
                onBack = onBack,
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // So window.speechSynthesis can speak without a prior tap on the page.
                        settings.mediaPlaybackRequiresUserGesture = false

                        // The console's client-side @supabase/ssr createBrowserClient writes the
                        // session to document.cookie; without this the /login -> /avatar cookie
                        // never survives the navigation inside this WebView instance. First-party
                        // only — every navigation here is same-origin (the allowlist below), so
                        // third-party cookies would only widen the jar to whatever the console
                        // page embeds, never help the login redirect.
                        CookieManager.getInstance().setAcceptCookie(true)

                        val allowedHost = Uri.parse(consoleUrl).host

                        webViewClient = object : WebViewClient() {
                            // Same-origin allowlist: anything that isn't the console host is
                            // refused rather than navigated, so this can never become an open
                            // browser to arbitrary sites.
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean = request.url.host != allowedHost

                            override fun onPageFinished(view: WebView, url: String?) {
                                isLoading = false
                                loadError = null

                                // app/login/page.tsx does router.replace('/') on success (no
                                // redirect-param support), so the bounce back to /avatar has to
                                // be driven from here, after the auth cookie is set, so page.tsx
                                // renders AvatarStage instead of redirecting again. Checked on
                                // EVERY page load (not a one-shot flag): a mid-session cookie
                                // expiry can bounce back to /login and then home a second time,
                                // and this must recover every time, not just the first — landing
                                // back on "/" always means "not there yet, keep navigating".
                                val path = url?.let { Uri.parse(it).path }
                                val landedOnHome = url != null &&
                                    Uri.parse(url).host == allowedHost &&
                                    (path.isNullOrEmpty() || path == "/")
                                if (landedOnHome) view.loadUrl("$consoleUrl/avatar")
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                // Ignore sub-resource failures (a broken avatar asset shouldn't
                                // blank the whole screen) — only the top-level navigation counts.
                                if (request.isForMainFrame) {
                                    isLoading = false
                                    loadError = error.description?.toString() ?: "unknown error"
                                }
                            }
                        }

                        // Voice INPUT (webkitSpeechRecognition, used by AvatarStage.tsx) is
                        // deliberately NOT wired here: AssistScreen's own chat screen talks to
                        // ODS Assist by voice with zero sensitive permissions (system
                        // RecognizerIntent activity holds the mic, not the app — see
                        // AssistScreen.kt), and this screen keeps that same permission-free
                        // footprint rather than requesting RECORD_AUDIO just because the
                        // embedded page happens to use the in-browser Web Speech API instead.
                        // Denying the mic request here is a graceful degrade, not a bug: the
                        // console's own canSpeech check hides the mic button when the API is
                        // unavailable, and a request that's denied mid-call just stops listening
                        // (AvatarStage.tsx's onerror handler) — typing and voice OUTPUT
                        // (window.speechSynthesis, plain audio playback, no permission needed)
                        // keep working either way.
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest) = request.deny()
                        }

                        // Hardware acceleration (needed for the three.js/WebGL avatar to render
                        // at all) is assumed at its Android default (enabled) — the manifest facts
                        // didn't enumerate <application> attributes, so this should be confirmed
                        // on-device.
                        loadUrl("$consoleUrl/avatar")
                    }
                },
                update = { view -> webView = view },
                modifier = Modifier.fillMaxSize(),
            )

            if (isLoading && loadError == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = OdsColors.Crimson)
                        Text(
                            "Loading avatar…",
                            color = OdsColors.Silver,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            loadError?.let { err ->
                AvatarMessage(
                    headline = "Couldn't load avatar",
                    detail = err,
                    onBack = onBack,
                    onRetry = { webView?.loadUrl("$consoleUrl/avatar") },
                )
            }
        }
    }
}

@Composable
private fun AvatarMessage(
    headline: String,
    detail: String,
    onBack: () -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .odsTile()
                .padding(20.dp),
        ) {
            Text(headline, style = MaterialTheme.typography.titleLarge, color = OdsColors.Crimson)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = OdsColors.SilverDim,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = 16.dp)) {
                if (onRetry != null) {
                    OutlinedButton(onClick = onRetry) { Text("RETRY", color = OdsColors.Crimson) }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                OutlinedButton(onClick = onBack) { Text("BACK", color = OdsColors.Silver) }
            }
        }
    }
}
