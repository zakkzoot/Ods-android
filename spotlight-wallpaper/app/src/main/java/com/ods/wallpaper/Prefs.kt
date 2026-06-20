package com.ods.wallpaper

import android.content.Context

/** Lightweight tunables for the wallpaper, editable from the setup screen. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("ods_wallpaper", Context.MODE_PRIVATE)

    /** Spotlight radius as a fraction of the screen's shorter edge. */
    var radiusFraction: Float
        get() = sp.getFloat(KEY_RADIUS, 0.34f)
        set(v) = sp.edit().putFloat(KEY_RADIUS, v).apply()

    /** How heavily the backdrop is blurred (1–4 downscale passes). */
    var blurPasses: Int
        get() = sp.getInt(KEY_BLUR, 2)
        set(v) = sp.edit().putInt(KEY_BLUR, v).apply()

    private companion object {
        const val KEY_RADIUS = "radius_fraction"
        const val KEY_BLUR = "blur_passes"
    }
}
