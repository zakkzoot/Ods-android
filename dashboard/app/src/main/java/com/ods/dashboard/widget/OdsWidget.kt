package com.ods.dashboard.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ods.dashboard.MainActivity
import com.ods.dashboard.data.Appearance
import com.ods.dashboard.data.AppearanceStore
import com.ods.dashboard.data.ConnectionStatus
import com.ods.dashboard.data.Health
import com.ods.dashboard.data.StatusStore
import com.ods.dashboard.data.rollupCategory
import com.ods.dashboard.model.Category
import com.ods.dashboard.ui.theme.OdsColors
import com.ods.dashboard.util.balanced

/**
 * The home-screen widget — four category rows mirroring the app home. Each row is a
 * collective inbox: an aggregate status dot + total notification count for everything in
 * that category, rendered from the cached status map. Tapping a row deep-links into
 * [MainActivity] focused on that category.
 *
 * Resizes from a small block up to an entire home page (see ods_widget_info.xml).
 */
class OdsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val statuses = StatusStore(context).snapshot()
        val appearance = AppearanceStore(context).load()
        provideContent { WidgetBody(context, statuses, appearance) }
    }

    private fun shownCategories(appearance: Appearance): List<Category> {
        val byName = Category.entries.associateBy { it.name }
        val ordered = appearance.categoryOrder.mapNotNull { byName[it] } +
            Category.entries.filter { it.name !in appearance.categoryOrder }
        return if (appearance.widgetCategories.isEmpty()) ordered
        else ordered.filter { it.name in appearance.widgetCategories }
    }

    @Composable
    private fun WidgetBody(context: Context, statuses: Map<String, ConnectionStatus>, appearance: Appearance) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(OdsColors.Charcoal).padding(10.dp),
        ) {
            Text(
                "ODS · CONNECTIONS",
                style = TextStyle(color = ColorProvider(OdsColors.SilverFaint), fontSize = 11.sp),
                modifier = GlanceModifier.padding(bottom = 8.dp),
            )
            shownCategories(appearance).forEach { category ->
                CategoryBlock(context, category, statuses, appearance)
                Spacer(GlanceModifier.height(6.dp))
            }
        }
    }

    @Composable
    private fun CategoryBlock(
        context: Context,
        category: Category,
        statuses: Map<String, ConnectionStatus>,
        appearance: Appearance,
    ) {
        val roll = rollupCategory(category, statuses)
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_CATEGORY, category.name)
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(OdsColors.Graphite)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clickable(actionStartActivity(intent)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = GlanceModifier.size(10.dp).background(dotColor(roll.health)), content = {})
                Spacer(GlanceModifier.width(10.dp))
                Text(
                    category.title,
                    style = TextStyle(color = ColorProvider(OdsColors.Silver), fontSize = 14.sp),
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (roll.totalBadge > 0) {
                    Box(
                        modifier = GlanceModifier.background(appearance.accent).padding(horizontal = 6.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (roll.totalBadge > 99) "99+" else roll.totalBadge.toString(),
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp),
                        )
                    }
                }
            }
            if (appearance.widgetShowInbox && roll.items.isNotEmpty()) {
                roll.items.take(2).forEach { line ->
                    Text(
                        "• ${balanced(line)}",
                        style = TextStyle(color = ColorProvider(OdsColors.SilverDim), fontSize = 11.sp),
                        modifier = GlanceModifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }

    private fun dotColor(h: Health): ColorProvider {
        val c = when (h) {
            Health.UP -> OdsColors.StatusUp
            Health.DEGRADED -> OdsColors.StatusDegraded
            Health.DOWN -> OdsColors.StatusDown
            Health.UNKNOWN -> OdsColors.StatusUnknown
        }
        return ColorProvider(c)
    }

    companion object {
        /** Redraw every placed widget from the freshly-cached status map. */
        suspend fun refresh(context: Context) {
            OdsWidget().updateAll(context)
        }
    }
}
