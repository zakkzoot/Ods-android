package com.ods.wallpaper

import android.app.WallpaperManager
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Minimal setup screen: explains the wallpaper and opens the system live-wallpaper
 * picker pre-selected on the ODS spotlight, where you apply it to lock + home screen.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0E0E10"))
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "ODS SPOTLIGHT"
            setTextColor(Color.parseColor("#C9CDD4"))
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "A live wallpaper that reveals the crimson intelligence layer " +
                "beneath a blurred backdrop — the spotlight follows your finger. " +
                "Apply it to the lock and home screen."
            setTextColor(Color.parseColor("#99C9CDD4"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 48)
        }
        val apply = Button(this).apply {
            text = "Set wallpaper"
            setOnClickListener { openPicker() }
        }

        root.addView(title, lp())
        root.addView(sub, lp())
        root.addView(apply, lp())
        setContentView(root)
    }

    private fun lp() = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

    private fun openPicker() {
        val component = ComponentName(this, SpotlightWallpaperService::class.java)
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        }
        runCatching { startActivity(intent) }.onFailure {
            // Fallback to the generic live wallpaper chooser.
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}
