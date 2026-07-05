package com.ods.dashboard.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * ODS Assist client — the phone side of the governed assistant.
 *
 * Auth model: the user signs in with email/password against the ODS Supabase Auth IdP
 * (GoTrue REST; the anon key is publishable by design). The access/refresh tokens are held in
 * [SecureConfig] (encrypted at rest). Every chat turn calls the deployed Assist console's
 * /api/chat with `Authorization: Bearer <access_token>` — the console verifies the JWT
 * server-side and runs the governed turn (deterministic command layer first, the LLM only on
 * the ask_memory fallthrough). NO model key ever lives on the device; the phone only ever
 * holds the user's OWN session, so it can never see more than the user may.
 *
 * SecureConfig keys:
 *  - assist_console_url : https://<the deployed ods-assistant console>
 *  - assist_auth_url    : https://<ref>.supabase.co  (the IdP project)
 *  - assist_anon_key    : that project's anon (publishable) key
 *  - assist_access / assist_refresh / assist_email : the session
 */
class AssistClient(private val config: SecureConfig) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // Governed turns can retrieve + compose; give the round trip real headroom.
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    val consoleUrl: String? get() = config.get(KEY_CONSOLE_URL)?.trimEnd('/')
    val authUrl: String? get() = config.get(KEY_AUTH_URL)?.trimEnd('/')
    val anonKey: String? get() = config.get(KEY_ANON)
    val email: String? get() = config.get(KEY_EMAIL)
    val signedIn: Boolean get() = config.has(KEY_ACCESS)
    val configured: Boolean get() = consoleUrl != null && authUrl != null && anonKey != null

    fun saveEndpoints(consoleUrl: String, authUrl: String, anonKey: String) {
        config.set(KEY_CONSOLE_URL, consoleUrl.filterNot { it.isWhitespace() }.ifBlank { null })
        config.set(KEY_AUTH_URL, authUrl.filterNot { it.isWhitespace() }.ifBlank { null })
        config.set(KEY_ANON, sanitizeKey(anonKey).ifBlank { null })
    }

    /**
     * Keys and URLs arrive by paste, and paste accidents are the #1 sign-in failure: a trailing
     * newline makes the value illegal as an HTTP header (okhttp: "Unexpected char 0x0a"), and a
     * double-tap paste doubles the key. Strip ALL whitespace, then collapse an exact double-paste
     * (first half == second half) back to a single copy. Deterministic; never alters a valid key.
     */
    private fun sanitizeKey(raw: String): String {
        val k = raw.filterNot { it.isWhitespace() }
        val half = k.length / 2
        return if (k.length % 2 == 0 && half > 0 && k.substring(0, half) == k.substring(half)) {
            k.substring(0, half)
        } else k
    }

    fun signOut() {
        config.set(KEY_ACCESS, null)
        config.set(KEY_REFRESH, null)
        config.set(KEY_EMAIL, null)
    }

    /** Email/password sign-in (GoTrue password grant). Also SIGNS UP transparently is NOT done —
     *  accounts are provisioned by the owner on the IdP project; unknown emails simply fail. */
    suspend fun signIn(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = authUrl ?: error("Set the auth URL first")
            val anon = anonKey ?: error("Set the anon key first")
            val body = json.encodeToString(
                PasswordGrant.serializer(),
                PasswordGrant(email = email, password = password),
            )
            val req = Request.Builder()
                .url("$auth/auth/v1/token?grant_type=password")
                .header("apikey", anon)
                .header("Content-Type", "application/json")
                .post(body.toRequestBody(JSON_MEDIA))
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error(gotrueError(text, resp.code))
                val session = json.decodeFromString(GotrueSession.serializer(), text)
                config.set(KEY_ACCESS, session.accessToken)
                config.set(KEY_REFRESH, session.refreshToken)
                config.set(KEY_EMAIL, email)
            }
        }
    }

    private fun refreshBlocking(): Boolean {
        val auth = authUrl ?: return false
        val anon = anonKey ?: return false
        val refresh = config.get(KEY_REFRESH) ?: return false
        val body = json.encodeToString(RefreshGrant.serializer(), RefreshGrant(refreshToken = refresh))
        val req = Request.Builder()
            .url("$auth/auth/v1/token?grant_type=refresh_token")
            .header("apikey", anon)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA))
            .build()
        return runCatching {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val session = json.decodeFromString(GotrueSession.serializer(), resp.body?.string().orEmpty())
                config.set(KEY_ACCESS, session.accessToken)
                config.set(KEY_REFRESH, session.refreshToken)
                true
            }
        }.getOrDefault(false)
    }

    /** One governed turn. Retries exactly once through a token refresh on a 401. */
    suspend fun chat(query: String): Result<AssistReply> = withContext(Dispatchers.IO) {
        runCatching {
            val console = consoleUrl ?: error("Set the Assist console URL first")
            fun call(token: String): Pair<Int, String> {
                val body = json.encodeToString(ChatRequest.serializer(), ChatRequest(query = query))
                val req = Request.Builder()
                    .url("$console/api/chat")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .post(body.toRequestBody(JSON_MEDIA))
                    .build()
                http.newCall(req).execute().use { resp -> return resp.code to resp.body?.string().orEmpty() }
            }

            var token = config.get(KEY_ACCESS) ?: error("Signed out")
            var (code, text) = call(token)
            if (code == 401 && refreshBlocking()) {
                token = config.get(KEY_ACCESS) ?: error("Signed out")
                val retry = call(token)
                code = retry.first
                text = retry.second
            }
            if (code == 401) {
                signOut()
                error("Session expired — sign in again")
            }
            if (code !in 200..299) {
                val err = runCatching { json.decodeFromString(ApiError.serializer(), text).error }.getOrNull()
                error(err ?: "Assist error (HTTP $code)")
            }
            json.decodeFromString(AssistReply.serializer(), text)
        }
    }

    private fun gotrueError(bodyText: String, code: Int): String =
        runCatching { json.decodeFromString(GotrueError.serializer(), bodyText) }
            .getOrNull()?.let { it.errorDescription ?: it.msg ?: it.error }
            ?: "Sign-in failed (HTTP $code)"

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        const val KEY_CONSOLE_URL = "assist_console_url"
        const val KEY_AUTH_URL = "assist_auth_url"
        const val KEY_ANON = "assist_anon_key"
        const val KEY_ACCESS = "assist_access"
        const val KEY_REFRESH = "assist_refresh"
        const val KEY_EMAIL = "assist_email"
    }
}

// ── Wire shapes ───────────────────────────────────────────────────────────────

@Serializable
private data class PasswordGrant(val email: String, val password: String)

@Serializable
private data class RefreshGrant(@SerialName("refresh_token") val refreshToken: String)

@Serializable
private data class GotrueSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
private data class GotrueError(
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    val msg: String? = null,
)

@Serializable
private data class ApiError(val error: String? = null)

@Serializable
private data class ChatRequest(val query: String)

/** Mirrors the console's ChatResponse (console/lib/types.ts). */
@Serializable
data class AssistReply(
    val answer: String = "",
    val outcome: String = "",
    /** 'deterministic' = the command layer answered with a real query; 'delegate' = the governed LLM loop. */
    val route: String? = null,
    val intent: String? = null,
    val envelope: AssistEnvelope? = null,
)

/** The persona-independent governance facts surfaced per turn. */
@Serializable
data class AssistEnvelope(
    val orgId: String = "",
    val orgType: String = "",
    val membershipRole: String = "",
    val effectiveTier: String = "",
    val allowFlirtation: Boolean = false,
    val outwardActionApprovalRequired: Boolean = true,
)
