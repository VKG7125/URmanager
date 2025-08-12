package com.github.vkg7125.urmanager.worker

import android.content.Context
import android.appwidget.AppWidgetManager
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.vkg7125.urmanager.R
import com.github.vkg7125.urmanager.URWidget
import com.github.vkg7125.urmanager.WidgetInfoStateDefinition
import com.github.vkg7125.urmanager.repository.UsageRepository
import com.github.vkg7125.urmanager.WidgetContentState
import com.github.vkg7125.urmanager.util.AuthTokenManager
import com.github.vkg7125.urmanager.security.JwtManager

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
                WidgetContentState(isLoading = false, errorMessage = context.getString(R.string.login_required), unpaidBytes = null)
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
                    currentState.copy(isLoading = false, errorMessage = context.getString(R.string.failed_to_load_data))
                }
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching usage data for GlanceId: $glanceId: ${e.message}", e)
            updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) { currentState ->
                currentState.copy(isLoading = false, errorMessage = e.message ?: context.getString(R.string.unknown_error))
            }
            Result.retry()
        } finally {
            URWidget().update(context, glanceId)
        }
    }
}