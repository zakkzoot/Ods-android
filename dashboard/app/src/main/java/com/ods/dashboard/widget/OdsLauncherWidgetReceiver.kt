package com.ods.dashboard.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Hosts [OdsLauncherWidget] on the home screen. Unlike [OdsWidgetReceiver] this one has
 * nothing to schedule — the grid is static links, so there is no status to refresh.
 */
class OdsLauncherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OdsLauncherWidget()
}
