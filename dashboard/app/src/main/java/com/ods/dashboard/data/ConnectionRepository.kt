package com.ods.dashboard.data

import android.content.Context
import com.ods.dashboard.model.Connections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Orchestrates a refresh of every connection's status and persists the result.
 * Probes run concurrently; the cache is updated atomically when all settle. Site tiles are
 * enriched from ODS Pulse (one call) when a Supabase anon key is configured.
 */
class ConnectionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val config = SecureConfig(appContext)
    private val store = StatusStore(appContext)
    private val provider = StatusProvider(appContext, config)
    private val pulse = PulseClient(config)

    val statuses = store.statuses

    suspend fun refreshAll(): Map<String, ConnectionStatus> = coroutineScope {
        val pulseSites = withContext(Dispatchers.IO) { pulse.fetch() }
        val results = Connections.all
            .map { c -> async { provider.check(c, pulseSites[c.id]) } }
            .map { it.await() }
            .associateBy { it.id }
        store.save(results)
        results
    }
}
