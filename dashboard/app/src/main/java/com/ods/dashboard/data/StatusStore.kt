package com.ods.dashboard.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "ods_status_cache")

/**
 * Persists the last-known status map as JSON so both the Activity and the Glance
 * widget render instantly from cache and never block on the network.
 */
class StatusStore(private val context: Context) {
    private val key = stringPreferencesKey("status_json")
    private val json = Json { ignoreUnknownKeys = true }

    val statuses: Flow<Map<String, ConnectionStatus>> = context.dataStore.data.map { prefs ->
        prefs[key]?.let { decode(it) } ?: emptyMap()
    }

    suspend fun snapshot(): Map<String, ConnectionStatus> {
        val raw = context.dataStore.data.first()[key]
        return raw?.let { decode(it) } ?: emptyMap()
    }

    suspend fun save(map: Map<String, ConnectionStatus>) {
        context.dataStore.edit { it[key] = json.encodeToString(map.values.toList()) }
    }

    private fun decode(s: String): Map<String, ConnectionStatus> = runCatching {
        json.decodeFromString<List<ConnectionStatus>>(s).associateBy { it.id }
    }.getOrDefault(emptyMap())
}
