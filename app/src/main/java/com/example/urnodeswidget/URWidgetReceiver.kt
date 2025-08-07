package com.example.urnodeswidget

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

    // Override onUpdate to trigger data fetch when the widget is updated by the system
    // This is primarily for initial setup or system-triggered updates.
    // Periodic updates are now handled by WorkManager.
    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // For each widget instance, trigger an immediate refresh action.
        // Periodic updates will be handled by WorkManager, scheduled in WidgetConfigActivity.
        appWidgetIds.forEach { appWidgetId ->
            CoroutineScope(Dispatchers.IO).launch {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                // The initial update is now handled by WidgetLoginActivity, and periodic updates by WorkManager.
                // No need to manually trigger RefreshWidgetAction here.
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            val workTag = "widget_update_$appWidgetId"
            WorkManager.getInstance(context).cancelAllWorkByTag(workTag)
            Log.d("URWidgetReceiver", "Cancelled WorkManager task for deleted widget: $workTag")
        }
    }
}