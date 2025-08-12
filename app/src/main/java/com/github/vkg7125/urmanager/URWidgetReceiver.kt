package com.github.vkg7125.urmanager

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class URWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = URWidget()

    /**
     * Called by the system to update the widget.
     * Periodic updates are handled by a scheduled WorkManager job. This onUpdate callback
     * is primarily for the initial widget placement. The actual data fetching and UI
     * updates are triggered from the widget's configuration activity (`WidgetLoginActivity`).
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // The main logic for widget updates (both initial and periodic) is handled
        // by WorkManager, which is scheduled in WidgetLoginActivity.
        // This method is intentionally left with minimal logic to avoid duplicate work.
    }

    /**
     * Called when a widget instance is deleted from the host.
     * This cancels any scheduled background work associated with that specific widget instance.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            val workTag = "widget_update_$appWidgetId"
            WorkManager.getInstance(context).cancelAllWorkByTag(workTag)
            Log.d("URWidgetReceiver", "Cancelled WorkManager task for deleted widget: $workTag")
        }
    }
}