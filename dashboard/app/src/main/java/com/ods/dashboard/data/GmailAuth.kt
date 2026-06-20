package com.ods.dashboard.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Gmail OAuth for reading unread counts, per account, via AppAuth (installed-app flow
 * with PKCE). The Google OAuth client ID is supplied by the user in Settings and stored
 * in [SecureConfig]; the granted AuthState (incl. refresh token) is persisted per
 * connection id. See SETUP_GOOGLE_OAUTH.md for the one-time Google Cloud setup.
 */
object GmailAuth {

    private const val REDIRECT_URI = "com.ods.dashboard:/oauth2redirect"

    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
        Uri.parse("https://oauth2.googleapis.com/token"),
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun isConfigured(config: SecureConfig) = config.has(SecureConfig.GOOGLE_CLIENT_ID)

    fun isConnected(config: SecureConfig, connectionId: String) =
        config.has(SecureConfig.gmailStateKey(connectionId))

    /** Builds the authorization request for one account (prefilled with its address). */
    fun authRequest(clientId: String, loginHint: String): AuthorizationRequest =
        AuthorizationRequest.Builder(
            serviceConfig, clientId, ResponseTypeValues.CODE, Uri.parse(REDIRECT_URI),
        )
            .setScope(Defaults.GMAIL_SCOPE)
            .setLoginHint(loginHint)
            .setAdditionalParameters(mapOf("access_type" to "offline", "prompt" to "consent"))
            .build()

    /** Exchanges the auth code for tokens and persists the AuthState. */
    fun completeAuthorization(
        service: AuthorizationService,
        response: AuthorizationResponse,
        config: SecureConfig,
        connectionId: String,
        onResult: (Boolean) -> Unit,
    ) {
        service.performTokenRequest(response.createTokenExchangeRequest()) { tokenResp, ex ->
            if (tokenResp != null) {
                val state = AuthState(response, tokenResp, ex)
                config.set(SecureConfig.gmailStateKey(connectionId), state.jsonSerializeString())
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun disconnect(config: SecureConfig, connectionId: String) =
        config.set(SecureConfig.gmailStateKey(connectionId), null)

    /** Unread count for an account's inbox, or null if not connected / unavailable. */
    suspend fun unreadCount(context: Context, config: SecureConfig, connectionId: String): Int? {
        val raw = config.get(SecureConfig.gmailStateKey(connectionId)) ?: return null
        val state = runCatching { AuthState.jsonDeserialize(raw) }.getOrNull() ?: return null
        val service = AuthorizationService(context)
        try {
            val token = freshToken(service, state) ?: return null
            // Persist any refreshed token material.
            config.set(SecureConfig.gmailStateKey(connectionId), state.jsonSerializeString())
            val req = Request.Builder()
                .url("https://gmail.googleapis.com/gmail/v1/users/me/labels/INBOX")
                .header("Authorization", "Bearer $token")
                .build()
            http.newCall(req).execute().use { resp ->
                if (resp.code !in 200..299) return null
                val body = resp.body?.string().orEmpty()
                return JSONObject(body).optInt("messagesUnread", 0)
            }
        } finally {
            service.dispose()
        }
    }

    private suspend fun freshToken(service: AuthorizationService, state: AuthState): String? =
        suspendCancellableCoroutine { cont ->
            state.performActionWithFreshTokens(service) { accessToken, _, _ ->
                cont.resume(accessToken)
            }
        }
}
