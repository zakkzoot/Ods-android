package com.ods.dashboard.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible trampoline for launcher-widget taps. The widget hands it a slot id; it looks
 * the destination up through [LauncherStore] (so an edited URL takes effect immediately,
 * without the widget having to be re-pinned) and hands it to whatever app claims it.
 *
 * Going through an activity rather than firing an implicit VIEW intent straight from the
 * widget keeps the resolution logic in one place and lets a missing handler fail with a
 * readable message instead of silently doing nothing.
 */
class LaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.getStringExtra(EXTRA_SHORTCUT_ID)
        val shortcut = id?.let { LauncherGrid.byId(it) }
        if (shortcut == null) {
            finish()
            return
        }

        val url = LauncherStore(this).urlOf(shortcut)
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(view)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Nothing on this phone can open $url", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    companion object {
        const val EXTRA_SHORTCUT_ID = "ods.launcher.SHORTCUT_ID"

        /**
         * The intent a widget cell fires. The `data` uri is what makes each cell's
         * PendingIntent distinct — PendingIntents compare with `Intent.filterEquals`,
         * which ignores extras, so without it all twelve cells would collapse into one.
         */
        fun intentFor(context: android.content.Context, id: String): Intent =
            Intent(context, LaunchActivity::class.java)
                .setData(Uri.parse("ods-launcher://slot/$id"))
                .putExtra(EXTRA_SHORTCUT_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
