package com.ods.dashboard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ods.dashboard.launcher.LaunchActivity
import com.ods.dashboard.launcher.LauncherGrid
import com.ods.dashboard.launcher.LauncherStore
import com.ods.dashboard.launcher.ResolvedShortcut
import com.ods.dashboard.ui.theme.OdsColors

/**
 * The ODS launcher grid — [LauncherGrid.ROWS] × [LauncherGrid.COLUMNS] of logo-and-label
 * cells, each opening its destination directly rather than routing through the dashboard.
 *
 * Layout is weight-driven (equal rows, equal columns within a row) so the twelve cells
 * keep their proportions as the widget is resized. Glance has no grid primitive; a Column
 * of weighted Rows is the equivalent.
 */
class OdsLauncherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val shortcuts = LauncherStore(context).resolved()
        provideContent { LauncherBody(context, shortcuts) }
    }

    @Composable
    private fun LauncherBody(context: Context, shortcuts: List<ResolvedShortcut>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(OdsColors.Charcoal)
                .cornerRadius(20.dp)
                .padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            shortcuts.chunked(LauncherGrid.COLUMNS).forEach { row ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEach { item ->
                        LauncherCell(context, item, GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }

    companion object {
        /** Redraw every placed launcher widget (after a URL or icon is changed). */
        suspend fun refresh(context: Context) {
            OdsLauncherWidget().updateAll(context)
        }
    }
}

/** One grid cell: the mark, then its word. Tapping opens the slot's destination. */
@Composable
private fun LauncherCell(context: Context, item: ResolvedShortcut, modifier: GlanceModifier) {
    val custom = LauncherStore.decodeIcon(item.iconPath)
    val provider = if (custom != null) ImageProvider(custom) else ImageProvider(item.iconRes)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(2.dp)
            .clickable(actionStartActivity(LaunchActivity.intentFor(context, item.id))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = provider,
            contentDescription = item.label,
            modifier = GlanceModifier.size(34.dp),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            item.label,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(OdsColors.Silver),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
