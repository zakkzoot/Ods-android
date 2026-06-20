package com.ods.dashboard.data

import kotlinx.serialization.Serializable

/** Health classification used to color the status dot. */
enum class Health { UP, DEGRADED, DOWN, UNKNOWN }

/**
 * The live (or last-cached) status of a single connection.
 *
 * @param health       coarse up/degraded/down/unknown
 * @param badge        notification-bubble count (unread, incidents, alerts); 0 = hidden
 * @param latencyMs    last probe latency, or null
 * @param checkedAtMs  epoch ms of the last successful check
 * @param figures      ordered label -> value pairs shown in the expanded popup
 */
@Serializable
data class ConnectionStatus(
    val id: String,
    val health: Health = Health.UNKNOWN,
    val badge: Int = 0,
    val latencyMs: Long? = null,
    val checkedAtMs: Long = 0L,
    val figures: List<Figure> = emptyList(),
)

@Serializable
data class Figure(val label: String, val value: String)
