package com.ods.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.ods.dashboard.data.AppearanceStore
import com.ods.dashboard.data.ConnectionRepository
import com.ods.dashboard.data.LocalAppearance
import com.ods.dashboard.data.SecureConfig
import com.ods.dashboard.model.Connection
import com.ods.dashboard.ui.CustomizeScreen
import com.ods.dashboard.ui.DashboardScreen
import com.ods.dashboard.ui.SettingsScreen
import com.ods.dashboard.ui.theme.OdsTheme
import com.ods.dashboard.work.StatusRefreshWorker
import kotlinx.coroutines.launch

private enum class Screen { DASHBOARD, SETTINGS, CUSTOMIZE }

/**
 * Hosts the full-screen dashboard. Launched standalone or deep-linked from a widget
 * tile via the "ods.dashboard.FOCUS" extra carrying the tapped connection id. A simple
 * in-app screen state swaps between the dashboard, token settings, and customisation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = ConnectionRepository(this)
        val config = SecureConfig(this)
        val appearanceStore = AppearanceStore(this)
        StatusRefreshWorker.schedule(this)
        refresh(repository)

        val focusId = intent?.getStringExtra(EXTRA_FOCUS)

        setContent {
            var appearance by remember { mutableStateOf(appearanceStore.load()) }
            CompositionLocalProvider(LocalAppearance provides appearance) {
                OdsTheme(accent = appearance.accent) {
                    var screen by remember { mutableStateOf(Screen.DASHBOARD) }
                    when (screen) {
                        Screen.SETTINGS -> {
                            BackHandler { screen = Screen.DASHBOARD }
                            SettingsScreen(
                                config = config,
                                onBack = { screen = Screen.DASHBOARD },
                                onSaved = { screen = Screen.DASHBOARD; refresh(repository) },
                                onCustomize = { screen = Screen.CUSTOMIZE },
                            )
                        }
                        Screen.CUSTOMIZE -> {
                            BackHandler { screen = Screen.SETTINGS }
                            CustomizeScreen(
                                store = appearanceStore,
                                onBack = { screen = Screen.SETTINGS },
                                onChanged = { appearance = appearanceStore.load() },
                            )
                        }
                        Screen.DASHBOARD -> DashboardScreen(
                            repository = repository,
                            focusId = focusId,
                            onOpen = ::open,
                            onOpenSettings = { screen = Screen.SETTINGS },
                        )
                    }
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
