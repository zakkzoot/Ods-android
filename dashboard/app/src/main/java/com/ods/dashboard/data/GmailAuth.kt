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
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Gmail OAuth for reading unread counts, per account, via AppAuth (installed-app flow
 * with PKCE). The Google OAuth client ID is supplied by the user in Settings and stored
 * in [SecureConfig]; the granted AuthState (incl. refresh token) is persisted per
 * connection id. See SETUP_GOOGLE_OAUTH.md for the one-time Google Cloud setup.
 */
/** Unread count + a few recent message lines ("From — Subject") for a mailbox. */
data class MailSummary(val unread: Int, val subjects: List<String>)

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

    /** Unread inbox count + recent unread subjects for an account, or null if unavailable. */
    suspend fun unreadCount(context: Context, config: SecureConfig, connectionId: String): Int? =
        inboxSummary(context, config, connectionId)?.unread

    /** Count of unread inbox messages plus a few recent subjects for the collective inbox. */
    suspend fun inboxSummary(
        context: Context,
        config: SecureConfig,
        connectionId: String,
        limit: Int = 5,
    ): MailSummary? {
        val raw = config.get(SecureConfig.gmailStateKey(connectionId)) ?: return null
        val state = runCatching { AuthState.jsonDeserialize(raw) }.getOrNull() ?: return null
        val service = AuthorizationService(context)
        try {
            val token = freshToken(service, state) ?: return null
            // Persist any refreshed token material.
            config.set(SecureConfig.gmailStateKey(connectionId), state.jsonSerializeString())

            val unread = http.newCall(
                Request.Builder()
                    .url("https://gmail.googleapis.com/gmail/v1/users/me/labels/INBOX")
                    .header("Authorization", "Bearer $token").build(),
            ).execute().use { resp ->
                if (resp.code !in 200..299) return null
                JSONObject(resp.body?.string().orEmpty()).optInt("messagesUnread", 0)
            }

            val ids = runCatching {
                http.newCall(
                    Request.Builder()
                        .url("https://gmail.googleapis.com/gmail/v1/users/me/messages?q=is:unread+in:inbox&maxResults=$limit")
                        .header("Authorization", "Bearer $token").build(),
                ).execute().use { resp ->
                    val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("messages") ?: JSONArray()
                    (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("id") }
                }
            }.getOrDefault(emptyList())

            val subjects = ids.mapNotNull { id ->
                runCatching {
                    http.newCall(
                        Request.Builder()
                            .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata&metadataHeaders=Subject&metadataHeaders=From")
                            .header("Authorization", "Bearer $token").build(),
                    ).execute().use { resp ->
                        val headers = JSONObject(resp.body?.string().orEmpty())
                            .optJSONObject("payload")?.optJSONArray("headers") ?: return@use null
                        var subject: String? = null
                        var from: String? = null
                        for (i in 0 until headers.length()) {
                            val h = headers.optJSONObject(i) ?: continue
                            when (h.optString("name")) {
                                "Subject" -> subject = h.optString("value")
                                "From" -> from = h.optString("value").substringBefore("<").trim().ifBlank { h.optString("value") }
                            }
                        }
                        listOfNotNull(from, subject).filter { it.isNotBlank() }.joinToString(" — ").ifBlank { null }
                    }
                }.getOrNull()
            }
            return MailSummary(unread, subjects)
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
