package com.github.vkg7125.urmanager

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import com.github.vkg7125.urmanager.ui.theme.URnodesWidgetTheme
import com.github.vkg7125.urmanager.util.AuthTokenManager
import com.github.vkg7125.urmanager.network.RetrofitClient
import com.github.vkg7125.urmanager.security.JwtManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import com.github.vkg7125.urmanager.worker.UsageUpdateWorker

/**
 * Configuration activity for the home screen widget.
 * This screen allows the user to enter their auth code and set a refresh interval.
 * Upon successful setup, it saves a widget-specific JWT, schedules a periodic
 * background worker for updates, and finishes.
 */
@OptIn(ExperimentalMaterial3Api::class)
class WidgetLoginActivity : ComponentActivity() {

    private val tag = "WidgetLoginActivity"
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent
            .getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            .takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            ?: run {
                Log.e(tag, "WidgetLoginActivity started with an invalid appWidgetId. Finishing.")
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }
        Log.d(tag, "Configuring for appWidgetId: $appWidgetId")

        setContent {
            URnodesWidgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF131829)
                ) {
                    val context = LocalContext.current
                    var authCodeInput by remember { mutableStateOf("") }
                    var isLoading by remember { mutableStateOf(false) }

                    val refreshIntervalOptions = listOf(
                        stringResource(id = R.string.interval_15_min) to 15L,
                        stringResource(id = R.string.interval_30_min) to 30L,
                        stringResource(id = R.string.interval_1_hour) to 60L,
                        stringResource(id = R.string.interval_2_hours) to 120L,
                        stringResource(id = R.string.interval_4_hours) to 240L,
                        stringResource(id = R.string.interval_6_hours) to 360L,
                        stringResource(id = R.string.interval_12_hours) to 720L,
                        stringResource(id = R.string.interval_24_hours) to 1440L
                    )
                    var expanded by remember { mutableStateOf(false) }
                    var selectedInterval by remember { mutableStateOf(refreshIntervalOptions[0]) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.sigma_launcher_trans_foreground),
                            contentDescription = "Widget Logo",
                            modifier = Modifier.size(120.dp)
                        )
                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = stringResource(id = R.string.widget_setup),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = stringResource(id = R.string.widget_setup_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(32.dp))

                        OutlinedTextField(
                            value = authCodeInput,
                            onValueChange = { authCodeInput = it },
                            label = { Text(stringResource(id = R.string.auth_code)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Gray,
                                unfocusedIndicatorColor = Color.DarkGray,
                                disabledIndicatorColor = Color.DarkGray,
                                focusedLabelColor = Color.Gray,
                                unfocusedLabelColor = Color.DarkGray,
                                disabledLabelColor = Color.DarkGray
                            )
                        )
                        Spacer(Modifier.height(16.dp))

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedInterval.first,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(id = R.string.refresh_interval)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Gray,
                                unfocusedIndicatorColor = Color.DarkGray,
                                disabledIndicatorColor = Color.DarkGray,
                                focusedLabelColor = Color.Gray,
                                unfocusedLabelColor = Color.DarkGray,
                                disabledLabelColor = Color.DarkGray
                            )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                refreshIntervalOptions.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption.first) },
                                        onClick = {
                                            selectedInterval = selectionOption
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (authCodeInput.isBlank()) {
                                    Toast.makeText(context, R.string.prompt_enter_auth_code, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (authCodeInput.length < 100) {
                                    Toast.makeText(context, R.string.auth_code_too_short, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isLoading = true
                                Log.d(tag, "Auth code input length: ${authCodeInput.length}")

                                lifecycleScope.launch {
                                    try {
                                        val requestBody = mapOf("auth_code" to authCodeInput)
                                        val response = RetrofitClient.authService.loginWithCode(requestBody)
                                        if (response.isSuccessful) {
                                            val jwtToken = response.body()?.token
                                            if (!jwtToken.isNullOrBlank()) {
                                                // Save the JWT specifically for widget use.
                                                AuthTokenManager.saveAuthToken(context, jwtToken, JwtManager.KEY_WIDGET_JWT)
                                                Log.i(tag, "JWT token saved for appWidgetId: $appWidgetId.")

                                                val inputData = Data.Builder()
                                                    .putInt(UsageUpdateWorker.APP_WIDGET_ID_KEY, appWidgetId)
                                                    .build()

                                                // Schedule the periodic background task to update the widget.
                                                val workTag = "widget_update_$appWidgetId"
                                                WorkManager.getInstance(context).cancelAllWorkByTag(workTag) // Cancel any old work.
                                                Log.d(tag, "Cancelled existing work for tag: $workTag")

                                                val refreshMinutes = selectedInterval.second
                                                val usageUpdateWorkRequest = PeriodicWorkRequestBuilder<UsageUpdateWorker>(
                                                    refreshMinutes, TimeUnit.MINUTES
                                                )
                                                    .addTag(workTag)
                                                    .setInputData(inputData)
                                                    .build()

                                                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                                    workTag,
                                                    ExistingPeriodicWorkPolicy.UPDATE, // Replace existing work.
                                                    usageUpdateWorkRequest
                                                )
                                                Log.d(tag, "Scheduled periodic work for appWidgetId: $appWidgetId with interval: $refreshMinutes minutes")

                                                // Enqueue an immediate update to show data right away.
                                                val immediateWorkRequest = OneTimeWorkRequestBuilder<UsageUpdateWorker>()
                                                    .addTag(workTag)
                                                    .setInputData(inputData)
                                                    .build()
                                                WorkManager.getInstance(context).enqueue(immediateWorkRequest)
                                                Log.d(tag, "Enqueued immediate update for appWidgetId: $appWidgetId")

                                                Log.d(tag, "Configuration complete for appWidgetId: $appWidgetId. Setting result OK.")
                                                val resultValue = Intent().apply {
                                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                                }
                                                setResult(Activity.RESULT_OK, resultValue)
                                                finish()
                                            } else {
                                                Toast.makeText(context, R.string.setup_failed_no_token, Toast.LENGTH_LONG).show()
                                                Log.e(tag, "Setup failed: No token received in response.")
                                            }
                                        } else {
                                            val errorBody = response.errorBody()?.string()
                                            val errorMessage = context.getString(R.string.setup_failed_with_error, response.code(), errorBody ?: "Unknown error")
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                            Log.e(tag, "Setup failed: HTTP ${response.code()}, Error: $errorBody")
                                        }
                                    } catch (e: HttpException) {
                                        val errorMessage = context.getString(R.string.network_error, e.message())
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        Log.e(tag, "HTTP Exception during widget setup: ${e.message()}", e)
                                    } catch (e: Exception) {
                                        val errorMessage = context.getString(R.string.unexpected_error, e.message)
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        Log.e(tag, "Unexpected error during widget setup", e)
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray,
                                contentColor = Color.White
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(stringResource(id = R.string.setup_widget))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        TextButton(
                            onClick = {
                                Log.d(tag, "Configuration cancelled for appWidgetId: $appWidgetId.")
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                        ) {
                            Text(stringResource(id = R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}