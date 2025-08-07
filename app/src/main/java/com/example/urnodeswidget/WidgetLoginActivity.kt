package com.example.urnodeswidget

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
import com.example.urnodeswidget.ui.theme.URnodesWidgetTheme
import com.example.urnodeswidget.util.AuthTokenManager
import com.example.urnodeswidget.network.RetrofitClient
import com.example.urnodeswidget.security.JwtManager // Import JwtManager to access keys
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import com.example.urnodeswidget.worker.UsageUpdateWorker

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
                        "15 minutes" to 15L,
                        "30 minutes" to 30L,
                        "1 hour" to 60L,
                        "2 hours" to 120L,
                        "4 hours" to 240L,
                        "6 hours" to 360L,
                        "12 hours" to 720L,
                        "24 hours" to 1440L
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
                            text = "Widget Setup",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Enter your auth code and select refresh rate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(32.dp))

                        OutlinedTextField(
                            value = authCodeInput,
                            onValueChange = { authCodeInput = it },
                            label = { Text("Auth Code") },
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
                                label = { Text("Refresh Interval") },
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
                                    Toast.makeText(context, "Please enter the auth code", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (authCodeInput.length < 100) {
                                    Toast.makeText(context, "Auth code seems too short", Toast.LENGTH_SHORT).show()
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
                                                // Save the JWT token using the widget-specific key
                                                AuthTokenManager.saveAuthToken(context, jwtToken, JwtManager.KEY_WIDGET_JWT)
                                                Log.i(tag, "JWT token saved for appWidgetId: $appWidgetId.")

                                                val inputData = Data.Builder()
                                                    .putInt(UsageUpdateWorker.APP_WIDGET_ID_KEY, appWidgetId)
                                                    .build()

                                                val workTag = "widget_update_$appWidgetId"
                                                WorkManager.getInstance(context).cancelAllWorkByTag(workTag)
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
                                                    ExistingPeriodicWorkPolicy.UPDATE,
                                                    usageUpdateWorkRequest
                                                )
                                                Log.d(tag, "Scheduled periodic work for appWidgetId: $appWidgetId with interval: $refreshMinutes minutes")

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
                                                Toast.makeText(context, "Setup failed: No token received", Toast.LENGTH_LONG).show()
                                                Log.e(tag, "Setup failed: No token received in response.")
                                            }
                                        } else {
                                            val errorBody = response.errorBody()?.string()
                                            Toast.makeText(context, "Setup failed: ${response.code()} - ${errorBody ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                                            Log.e(tag, "Setup failed: HTTP ${response.code()}, Error: $errorBody")
                                        }
                                    } catch (e: HttpException) {
                                        Toast.makeText(context, "Network error: ${e.message()}", Toast.LENGTH_LONG).show()
                                        Log.e(tag, "HTTP Exception during widget setup: ${e.message()}", e)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
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
                                Text("Setup Widget")
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
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}