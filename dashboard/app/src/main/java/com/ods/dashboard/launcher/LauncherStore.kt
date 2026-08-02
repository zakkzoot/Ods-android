package com.ods.dashboard.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * Per-slot overrides for the launcher grid: a replacement destination URL and/or a
 * replacement icon image. Nothing here is required — every slot falls back to the
 * bundled default in [LauncherGrid] — so a wrong default costs one edit, not a rebuild.
 *
 * Icons picked from the gallery are copied into private storage (same approach as
 * [com.ods.dashboard.data.AppearanceStore]) so no persistable URI permission is needed.
 */
class LauncherStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("ods_launcher", Context.MODE_PRIVATE)

    /** The grid with user overrides applied, in render order. */
    fun resolved(): List<ResolvedShortcut> = LauncherGrid.all.map { s ->
        ResolvedShortcut(
            shortcut = s,
            url = urlOf(s),
            iconPath = iconPath(s.id),
        )
    }

    fun urlOf(shortcut: Shortcut): String =
        prefs.getString(URL_PREFIX + shortcut.id, null)?.takeIf { it.isNotBlank() } ?: shortcut.url

    fun iconPath(id: String): String? = prefs.getString(ICON_PREFIX + id, null)

    /** Pass null (or blank) to fall back to the bundled default. */
    fun setUrl(id: String, url: String?) = prefs.edit().apply {
        val clean = url?.trim().orEmpty()
        if (clean.isEmpty()) remove(URL_PREFIX + id) else putString(URL_PREFIX + id, clean)
    }.apply()

    fun setIcon(id: String, path: String?) = prefs.edit().apply {
        if (path == null) remove(ICON_PREFIX + id) else putString(ICON_PREFIX + id, path)
    }.apply()

    fun resetAll() {
        File(app.filesDir, IMG_DIR).deleteRecursively()
        prefs.edit().clear().apply()
    }

    /** Copy a picked image into private storage; returns the absolute path or null. */
    fun importIcon(uri: Uri, id: String): String? = runCatching {
        val dir = File(app.filesDir, IMG_DIR).apply { mkdirs() }
        val out = File(dir, "$id-${System.currentTimeMillis()}")
        app.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        } ?: return null
        out.absolutePath
    }.getOrNull()

    companion object {
        private const val URL_PREFIX = "url_"
        private const val ICON_PREFIX = "icon_"
        private const val IMG_DIR = "launcher"

        /**
         * Decode a custom icon down to widget size. Widget bitmaps cross a Binder
         * transaction, so they must stay small — twelve full-resolution photos would blow
         * the ~1 MB RemoteViews budget and the widget would render empty. At 96 px a cell
         * icon is still sharp at its 34 dp draw size, and twelve of them fit comfortably.
         */
        fun decodeIcon(path: String?, maxPx: Int = 96): Bitmap? {
            if (path.isNullOrBlank()) return null
            return runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                val largest = maxOf(bounds.outWidth, bounds.outHeight)
                if (largest <= 0) return null
                var sample = 1
                while (largest / sample > maxPx) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()
        }
    }
}

/** A grid slot with its overrides already applied. */
data class ResolvedShortcut(
    val shortcut: Shortcut,
    val url: String,
    val iconPath: String?,
) {
    val id: String get() = shortcut.id
    val label: String get() = shortcut.label
    val iconRes: Int get() = shortcut.iconRes
}
