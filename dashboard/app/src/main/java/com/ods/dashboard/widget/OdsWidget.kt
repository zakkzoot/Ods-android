package com.ods.dashboard.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ods.dashboard.MainActivity
import com.ods.dashboard.data.ConnectionStatus
import com.ods.dashboard.data.Health
import com.ods.dashboard.data.StatusStore
import com.ods.dashboard.model.Connection
import com.ods.dashboard.model.Connections
import com.ods.dashboard.ui.theme.OdsColors

/**
 * The home-screen widget — the at-a-glance grid. Renders every connection as a
 * monogram tile with a status dot + notification bubble, from the cached status map.
 * Tapping a tile deep-links into [MainActivity] focused on that connection, where the
 * two-stage popup/open interaction lives (Glance cannot hold transient popup state).
 *
 * Resizes from a small block up to an entire home page (see ods_widget_info.xml).
 */
class OdsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val statuses = StatusStore(context).snapshot()
        provideContent { WidgetBody(context, statuses) }
    }

    @Composable
    private fun WidgetBody(context: Context, statuses: Map<String, ConnectionStatus>) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(OdsColors.Charcoal).padding(10.dp),
        ) {
            Text(
                "ODS · CONNECTIONS",
                style = TextStyle(color = ColorProvider(OdsColors.SilverFaint), fontSize = 11.sp),
                modifier = GlanceModifier.padding(bottom = 6.dp),
            )
            LazyVerticalGrid(
                gridCells = GridCells.Adaptive(72.dp),
                modifier = GlanceModifier.fillMaxSize(),
            ) {
                items(Connections.all, itemId = { it.id.hashCode().toLong() }) { c ->
                    WidgetTile(context, c, statuses[c.id])
                }
            }
        }
    }

    @Composable
    private fun WidgetTile(context: Context, c: Connection, status: ConnectionStatus?) {
        val params: ActionParameters = actionParametersOf(focusKey to c.id)
        Column(
            modifier = GlanceModifier
                .padding(4.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java), params)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = GlanceModifier.size(56.dp).background(OdsColors.Graphite).padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(c.monogram, style = TextStyle(color = ColorProvider(OdsColors.Silver), fontSize = 16.sp))
                // status dot
                Box(
                    modifier = GlanceModifier.size(9.dp).background(dotColor(status?.health)),
                    content = {},
                )
                // notification bubble
                val badge = status?.badge ?: 0
                if (badge > 0) {
                    Box(
                        modifier = GlanceModifier.background(OdsColors.Crimson).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (badge > 99) "99+" else badge.toString(),
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 9.sp),
                        )
                    }
                }
            }
            Text(
                c.monogram,
                style = TextStyle(color = ColorProvider(OdsColors.SilverDim), fontSize = 9.sp),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }

    private fun dotColor(h: Health?): ColorProvider {
        val c = when (h) {
            Health.UP -> OdsColors.StatusUp
            Health.DEGRADED -> OdsColors.StatusDegraded
            Health.DOWN -> OdsColors.StatusDown
            else -> OdsColors.StatusUnknown
        }
        return ColorProvider(c)
    }

    companion object {
        val focusKey = ActionParameters.Key<String>(MainActivity.EXTRA_FOCUS)

        /** Redraw every placed widget from the freshly-cached status map. */
        suspend fun refresh(context: Context) {
            OdsWidget().updateAll(context)
        }
    }
}
