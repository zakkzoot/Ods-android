package com.ods.dashboard.data

import android.content.Context
import com.ods.dashboard.model.Connections
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Orchestrates a refresh of every connection's status and persists the result.
 * Probes run concurrently; the cache is updated atomically when all settle.
 */
class ConnectionRepository(context: Context) {
    private val appContext = context.applicationContext
    private val store = StatusStore(appContext)
    private val provider = StatusProvider(appContext, SecureConfig(appContext))

    val statuses = store.statuses

    suspend fun refreshAll(): Map<String, ConnectionStatus> = coroutineScope {
        val results = Connections.all
            .map { c -> async { provider.check(c) } }
            .map { it.await() }
            .associateBy { it.id }
        store.save(results)
        results
    }
}
