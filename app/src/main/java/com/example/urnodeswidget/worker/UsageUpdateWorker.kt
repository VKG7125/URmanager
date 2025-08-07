package com.example.urnodeswidget.worker

import android.content.Context
import android.appwidget.AppWidgetManager
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.urnodeswidget.URWidget
import com.example.urnodeswidget.WidgetInfoStateDefinition
import com.example.urnodeswidget.repository.UsageRepository
import com.example.urnodeswidget.WidgetContentState
import com.example.urnodeswidget.util.AuthTokenManager
import com.example.urnodeswidget.security.JwtManager

/**
 * A CoroutineWorker responsible for fetching updated usage statistics in the background.
 * This worker is scheduled periodically to keep the widget data fresh without
 * impacting the main application's performance.
 */
class UsageUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val APP_WIDGET_ID_KEY = "appWidgetId"
        private const val TAG = "UsageUpdateWorker"
    }

    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(APP_WIDGET_ID_KEY, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "No valid appWidgetId provided to worker.")
            return Result.failure()
        }

        val glanceAppWidgetManager = GlanceAppWidgetManager(context)
        val glanceId: GlanceId = glanceAppWidgetManager.getGlanceIdBy(appWidgetId)

        Log.d(TAG, "Worker started for appWidgetId: $appWidgetId, GlanceId: $glanceId")

        updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) { currentState ->
            currentState.copy(isLoading = true, errorMessage = null)
        }
        URWidget().update(context, glanceId)

        // Retrieve auth token using the widget-specific key
        val authToken = AuthTokenManager.getAuthToken(context, JwtManager.KEY_WIDGET_JWT)
        if (authToken.isNullOrBlank()) {
            Log.w(TAG, "Auth token missing for widget update. Redirecting to login.")
            updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) {
                WidgetContentState(isLoading = false, errorMessage = "Login required", unpaidBytes = null)
            }
            URWidget().update(context, glanceId)
            return Result.failure()
        }

        return try {
            val unpaidBytes = UsageRepository.fetchUsageData(context)
            if (unpaidBytes != null) {
                updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) {
                    WidgetContentState(
                        unpaidBytes = unpaidBytes,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                Log.d(TAG, "Successfully fetched and updated widget for GlanceId: $glanceId")
                Result.success()
            } else {
                Log.e(TAG, "Failed to fetch usage data for GlanceId: $glanceId (data was null).")
                updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) { currentState ->
                    currentState.copy(isLoading = false, errorMessage = "Failed to load data")
                }
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching usage data for GlanceId: $glanceId: ${e.message}", e)
            updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) { currentState ->
                currentState.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
            }
            Result.retry()
        } finally {
            URWidget().update(context, glanceId)
        }
    }
}
