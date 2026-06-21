package com.ods.dashboard.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ods.dashboard.ui.theme.OdsColors
import java.io.File

/**
 * User-customisable look: brand accent, background image (+ crimson overlay strength),
 * header logo, and per-connection tile icons. Images chosen from the gallery are copied
 * into the app's private storage so no persistable URI permission is needed; everything
 * else is small strings in plain prefs.
 */
data class Appearance(
    val accent: Color = OdsColors.Crimson,
    val accentHex: String = "#D42B2B",
    val backgroundPath: String? = null,   // custom background image, else null
    val useDefaultHero: Boolean = true,   // when no custom image, show the bundled hero
    val overlayAlpha: Float = 0.55f,      // crimson scrim strength over the background
    val logoPath: String? = null,         // custom header logo, else the ODS cube
    val iconPaths: Map<String, String> = emptyMap(), // connectionId -> tile icon image
)

/** Appearance is read high in the tree and provided to the whole UI. */
val LocalAppearance = staticCompositionLocalOf { Appearance() }

/** Decode a private-storage image path into an ImageBitmap (cached per path). */
@Composable
fun rememberFileBitmap(path: String?): ImageBitmap? = remember(path) {
    if (path.isNullOrBlank()) null
    else runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
}

class AppearanceStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("ods_appearance", Context.MODE_PRIVATE)

    fun load(): Appearance {
        val accentHex = prefs.getString(KEY_ACCENT, null)
        val icons = prefs.all.entries
            .filter { it.key.startsWith(ICON_PREFIX) && it.value is String }
            .associate { it.key.removePrefix(ICON_PREFIX) to it.value as String }
        return Appearance(
            accent = accentHex?.let { parseColor(it) } ?: OdsColors.Crimson,
            accentHex = accentHex ?: "#D42B2B",
            backgroundPath = prefs.getString(KEY_BG, null),
            useDefaultHero = prefs.getBoolean(KEY_USE_HERO, true),
            overlayAlpha = prefs.getFloat(KEY_OVERLAY, 0.55f),
            logoPath = prefs.getString(KEY_LOGO, null),
            iconPaths = icons,
        )
    }

    fun setAccent(hex: String?) = prefs.edit().putString(KEY_ACCENT, hex?.takeIf { isValidColor(it) }).apply()
    fun setOverlay(alpha: Float) = prefs.edit().putFloat(KEY_OVERLAY, alpha.coerceIn(0f, 1f)).apply()
    fun setUseDefaultHero(enabled: Boolean) = prefs.edit().putBoolean(KEY_USE_HERO, enabled).apply()
    fun setBackground(path: String?) = prefs.edit().putString(KEY_BG, path).apply()
    fun setLogo(path: String?) = prefs.edit().putString(KEY_LOGO, path).apply()
    fun setIcon(connectionId: String, path: String?) =
        prefs.edit().apply { if (path == null) remove(ICON_PREFIX + connectionId) else putString(ICON_PREFIX + connectionId, path) }.apply()

    fun resetAll() {
        File(app.filesDir, IMG_DIR).deleteRecursively()
        prefs.edit().clear().apply()
    }

    /** Copy a picked image into private storage; returns the absolute path or null. */
    fun importImage(uri: Uri, baseName: String): String? = runCatching {
        val dir = File(app.filesDir, IMG_DIR).apply { mkdirs() }
        val out = File(dir, "$baseName-${System.currentTimeMillis()}")
        app.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        } ?: return null
        out.absolutePath
    }.getOrNull()

    private fun parseColor(hex: String): Color =
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(OdsColors.Crimson)

    private fun isValidColor(hex: String): Boolean =
        runCatching { android.graphics.Color.parseColor(hex); true }.getOrDefault(false)

    companion object {
        private const val KEY_ACCENT = "accent_hex"
        private const val KEY_BG = "bg_path"
        private const val KEY_USE_HERO = "use_hero"
        private const val KEY_OVERLAY = "overlay_alpha"
        private const val KEY_LOGO = "logo_path"
        private const val ICON_PREFIX = "icon_"
        private const val IMG_DIR = "appearance"
    }
}
