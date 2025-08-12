package com.github.vkg7125.urmanager

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.github.vkg7125.urmanager.repository.UsageRepository
import com.github.vkg7125.urmanager.security.JwtManager
import com.github.vkg7125.urmanager.ui.theme.WidgetColors
import com.github.vkg7125.urmanager.util.AuthTokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

@Serializable
data class WidgetContentState(
    val unpaidBytes: Long? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Defines how the state for the [URWidget] is stored and retrieved.
 *
 * This custom implementation uses a simple file-based approach with JSON serialization.
 * It manually reads and writes the [WidgetContentState] to a JSON file in the app's
 * internal storage. This provides a straightforward state persistence mechanism
 * for the Glance widget.
 */
object WidgetInfoStateDefinition : GlanceStateDefinition<WidgetContentState> {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "widget_state_$fileKey.json")
    }

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<WidgetContentState> {
        return object : DataStore<WidgetContentState> {
            override val data: Flow<WidgetContentState> = flow {
                val file = getLocation(context, fileKey)
                val state = if (file.exists()) {
                    try {
                        json.decodeFromString<WidgetContentState>(file.readText())
                    } catch (_: Exception) {
                        WidgetContentState(isLoading = false, errorMessage = context.getString(R.string.error))
                    }
                } else {
                    WidgetContentState(isLoading = true)
                }
                emit(state)
            }

            override suspend fun updateData(transform: suspend (t: WidgetContentState) -> WidgetContentState): WidgetContentState {
                val file = getLocation(context, fileKey)
                val currentState = if (file.exists()) {
                    try {
                        json.decodeFromString<WidgetContentState>(file.readText())
                    } catch (_: Exception) {
                        WidgetContentState(isLoading = false, errorMessage = context.getString(R.string.error))
                    }
                } else {
                    WidgetContentState(isLoading = true)
                }

                val newState = transform(currentState)
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(newState))
                return newState
            }
        }
    }
}

/**
 * Action callback to handle manual refresh events on the widget.
 * When triggered (e.g., by a user tap), it fetches the latest usage data.
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        // Set loading state and update UI
        updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) { currentState ->
            currentState.copy(isLoading = true, errorMessage = null)
        }
        URWidget().update(context, glanceId)

        // Check for auth token before fetching data
        val authToken = AuthTokenManager.getAuthToken(context, JwtManager.KEY_WIDGET_JWT)
        if (authToken.isNullOrBlank()) {
            updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) {
                WidgetContentState(isLoading = false, errorMessage = context.getString(R.string.login_required), unpaidBytes = null)
            }
        } else {
            try {
                val data = UsageRepository.fetchUsageData(context)
                if (data != null) {
                    updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) {
                        WidgetContentState(
                            unpaidBytes = data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) { currentState ->
                        currentState.copy(isLoading = false, errorMessage = context.getString(R.string.failed_to_load_data))
                    }
                }
            } catch (e: Exception) {
                updateAppWidgetState(context, WidgetInfoStateDefinition, glanceId) { currentState ->
                    currentState.copy(isLoading = false, errorMessage = e.message ?: context.getString(R.string.unknown_error))
                }
            }
        }
        // Final update to render the new state
        URWidget().update(context, glanceId)
    }
}

class URWidget : GlanceAppWidget() {
    override val stateDefinition = WidgetInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val widgetState = currentState<WidgetContentState>()

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.widgetBackground)
                .padding(16.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.mipmap.sigma_launcher_trans_foreground),
                contentDescription = "App Logo",
                modifier = GlanceModifier.size(72.dp)
            )

            Spacer(modifier = GlanceModifier.width(12.dp))

            when {
                widgetState.isLoading -> {
                    Text(
                        text = context.getString(R.string.loading),
                        style = TextStyle(fontSize = 20.sp, color = WidgetColors.textColorSecondary)
                    )
                }
                widgetState.errorMessage != null -> {
                    Text(
                        text = context.getString(R.string.error),
                        style = TextStyle(fontSize = 20.sp, color = WidgetColors.errorColor, fontWeight = FontWeight.Bold)
                    )
                }
                widgetState.unpaidBytes != null -> {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = context.getString(R.string.unpaid_provided),
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = WidgetColors.textColorSecondary
                            )
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = formatBytes(widgetState.unpaidBytes),
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = WidgetColors.textColorPrimary
                            )
                        )
                    }
                }
                else -> {
                    Text(
                        text = context.getString(R.string.tap_to_refresh),
                        style = TextStyle(fontSize = 20.sp, color = WidgetColors.textColorSecondary)
                    )
                }
            }
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String = when {
            bytes >= 1000L * 1000 * 1000 -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1000.0 * 1000.0 * 1000.0))
            bytes >= 1000L * 1000 -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1000.0 * 1000.0))
            bytes >= 1000L -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1000.0)
            else -> "$bytes B"
        }
    }
}