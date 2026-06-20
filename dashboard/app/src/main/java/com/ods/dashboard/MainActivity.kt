package com.ods.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.ods.dashboard.data.ConnectionRepository
import com.ods.dashboard.data.SecureConfig
import com.ods.dashboard.model.Connection
import com.ods.dashboard.ui.DashboardScreen
import com.ods.dashboard.ui.SettingsScreen
import com.ods.dashboard.ui.theme.OdsTheme
import com.ods.dashboard.work.StatusRefreshWorker
import kotlinx.coroutines.launch

/**
 * Hosts the full-screen dashboard. Launched standalone or deep-linked from a widget
 * tile via the "ods.dashboard.FOCUS" extra carrying the tapped connection id.
 * A lightweight in-app screen toggle swaps between the dashboard and token settings.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = ConnectionRepository(this)
        val config = SecureConfig(this)
        StatusRefreshWorker.schedule(this)
        refresh(repository)

        val focusId = intent?.getStringExtra(EXTRA_FOCUS)

        setContent {
            OdsTheme {
                var showSettings by remember { mutableStateOf(false) }
                if (showSettings) {
                    androidx.activity.compose.BackHandler { showSettings = false }
                    SettingsScreen(
                        config = config,
                        onBack = { showSettings = false },
                        onSaved = { showSettings = false; refresh(repository) },
                    )
                } else {
                    DashboardScreen(
                        repository = repository,
                        focusId = focusId,
                        onOpen = ::open,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }

    private fun refresh(repository: ConnectionRepository) {
        lifecycleScope.launch { runCatching { repository.refreshAll() } }
    }

    private fun open(c: Connection) {
        val action = if (c.url.startsWith("mailto:")) Intent.ACTION_SENDTO else Intent.ACTION_VIEW
        runCatching { startActivity(Intent(action, Uri.parse(c.url))) }
    }

    companion object {
        const val EXTRA_FOCUS = "ods.dashboard.FOCUS"
    }
}
