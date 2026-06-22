package com.ods.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ods.dashboard.data.Appearance
import com.ods.dashboard.data.AppearanceStore
import com.ods.dashboard.data.ConnectionRepository
import com.ods.dashboard.data.LocalAppearance
import com.ods.dashboard.data.SecureConfig
import com.ods.dashboard.model.Category
import com.ods.dashboard.model.Connection
import com.ods.dashboard.ui.CustomizeScreen
import com.ods.dashboard.ui.DashboardScreen
import com.ods.dashboard.ui.SettingsScreen
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.ui.theme.OdsTheme
import com.ods.dashboard.work.StatusRefreshWorker
import kotlinx.coroutines.launch

private enum class Screen { DASHBOARD, SETTINGS, CUSTOMIZE }

/** Shown if app startup fails, so we get a readable message instead of a blank screen. */
@Composable
private fun StartupError(e: Throwable) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OdsColors.Charcoal)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "ODS Dashboard couldn't start.\n\n${e::class.simpleName}: ${e.message ?: "unknown error"}",
            color = OdsColors.Silver,
        )
    }
}

/**
 * Hosts the full-screen dashboard. Launched standalone or deep-linked from a widget
 * tile via the "ods.dashboard.FOCUS" extra carrying the tapped connection id. A simple
 * in-app screen state swaps between the dashboard, token settings, and customisation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val setup = runCatching {
            val repository = ConnectionRepository(this)
            val config = SecureConfig(this)
            val appearanceStore = AppearanceStore(this)
            StatusRefreshWorker.schedule(this)
            refresh(repository)
            Triple(repository, config, appearanceStore)
        }
        setup.exceptionOrNull()?.let { e ->
            setContent { OdsTheme { StartupError(e) } }
            return
        }
        val (repository, config, appearanceStore) = setup.getOrThrow()

        val focusId = intent?.getStringExtra(EXTRA_FOCUS)
        val focusCategory = intent?.getStringExtra(EXTRA_CATEGORY)
            ?.let { runCatching { Category.valueOf(it) }.getOrNull() }

        setContent {
            var appearance by remember { mutableStateOf(runCatching { appearanceStore.load() }.getOrNull()) }
            OdsTheme(accent = appearance?.accent ?: OdsColors.Crimson) {
                CompositionLocalProvider(LocalAppearance provides (appearance ?: Appearance())) {
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
                                onChanged = { appearance = runCatching { appearanceStore.load() }.getOrNull() },
                            )
                        }
                        Screen.DASHBOARD -> DashboardScreen(
                            repository = repository,
                            appearanceStore = appearanceStore,
                            onAppearanceChanged = { appearance = appearanceStore.load() },
                            focusId = focusId,
                            initialCategory = focusCategory,
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
        const val EXTRA_CATEGORY = "ods.dashboard.CATEGORY"
    }
}
