package com.ods.dashboard.data

import com.ods.dashboard.model.Category
import com.ods.dashboard.model.Connections

/** A category's rolled-up state for the collective-inbox cards (app + widget). */
data class CategoryRollup(val health: Health, val totalBadge: Int, val items: List<String>)

/** Worst-case light, summed unread, and recent headlines across a category's members. */
fun rollupCategory(category: Category, statuses: Map<String, ConnectionStatus>): CategoryRollup {
    val members = Connections.inCategory(category)
    var badge = 0
    val healths = ArrayList<Health>()
    val items = ArrayList<String>()
    members.forEach { c ->
        val s = statuses[c.id]
        badge += s?.badge ?: 0
        healths += s?.health ?: Health.UNKNOWN
        when {
            s != null && s.items.isNotEmpty() -> s.items.take(3).forEach { items += it }
            (s?.badge ?: 0) > 0 -> items += "${c.label}: ${s!!.badge} new"
        }
    }
    val health = when {
        healths.any { it == Health.DOWN } -> Health.DOWN
        healths.any { it == Health.DEGRADED } -> Health.DEGRADED
        healths.any { it == Health.UP } -> Health.UP
        else -> Health.UNKNOWN
    }
    return CategoryRollup(health, badge, items)
}
