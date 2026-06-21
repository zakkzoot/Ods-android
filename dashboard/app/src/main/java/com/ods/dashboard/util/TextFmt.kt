package com.ods.dashboard.util

import kotlin.math.abs

/**
 * Balanced wrap for popups / dropdowns / tile labels: if a string is longer than [max]
 * characters, split it into two roughly equal lines at the space nearest the midpoint
 * (or hard-split at the midpoint when there's no space). Strings <= [max] are untouched.
 *
 * Newlines stand in for the requested `<br>`; Compose `Text` and Glance `Text` both honour
 * `\n`. Width then naturally tracks the longest resulting line.
 */
fun balanced(text: String, max: Int = 15): String {
    val t = text.trim()
    if (t.length <= max) return t
    val mid = t.length / 2
    var best = -1
    t.forEachIndexed { i, ch ->
        if (ch == ' ' && (best == -1 || abs(i - mid) < abs(best - mid))) best = i
    }
    return if (best > 0) {
        t.substring(0, best).trimEnd() + "\n" + t.substring(best + 1).trimStart()
    } else {
        t.substring(0, mid) + "\n" + t.substring(mid)
    }
}
