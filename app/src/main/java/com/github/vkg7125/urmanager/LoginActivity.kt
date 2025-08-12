package com.github.vkg7125.urmanager

import android.app.Activity
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
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.vkg7125.urmanager.ui.theme.URnodesWidgetTheme
import com.github.vkg7125.urmanager.util.AuthTokenManager
import com.github.vkg7125.urmanager.security.JwtManager

/**
 * Handles the user login process for the main web panel.
 * It provides a UI for entering an auth code, saves it securely, and navigates
 * to the MainActivity upon successful login. It also handles the splash screen
 * and checks for existing login sessions.
 */
class LoginActivity : ComponentActivity() {

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition. This must be called before super.onCreate().
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Configure window for edge-to-edge display.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(0xFF131829.toInt().toDrawable())

        val desiredDarkColor = 0xFF131829.toInt()
        window.statusBarColor = desiredDarkColor
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        window.navigationBarColor = desiredDarkColor
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false

        // Check if user is already logged in.
        val authToken = AuthTokenManager.getAuthToken(this, JwtManager.KEY_WEB_PANEL_AUTH_CODE)
        val isLoggedIn = !authToken.isNullOrBlank()

        if (isLoggedIn) {
            Log.d(TAG, "User already logged in, navigating to MainActivity")
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("AUTH_CODE_EXTRA", authToken as String) // Pass the stored token
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        setContent {
            URnodesWidgetTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues()),
                    color = Color(0xFF131829)
                ) {
                    val context = LocalContext.current
                    var longAuthCodeInput by remember { mutableStateOf("") }
                    var isLoading by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.sigma_launcher_foreground),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(120.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(id = R.string.webpanel_login),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.webpanel_login_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(32.dp))

                        OutlinedTextField(
                            value = longAuthCodeInput,
                            onValueChange = { longAuthCodeInput = it },
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
                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (longAuthCodeInput.isBlank()) {
                                    Toast.makeText(context, R.string.prompt_enter_auth_code, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (longAuthCodeInput.length < 100) {
                                    Toast.makeText(context, R.string.auth_code_too_short, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isLoading = true
                                Log.d(TAG, "Auth Code input length: ${longAuthCodeInput.length}")

                                // Save the provided auth code for the web panel.
                                val isSaved = AuthTokenManager.saveAuthToken(context, longAuthCodeInput, JwtManager.KEY_WEB_PANEL_AUTH_CODE)
                                if (isSaved) {
                                    Log.i(TAG, "Auth code saved securely for web panel.")
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        putExtra("AUTH_CODE_EXTRA", longAuthCodeInput) // Pass the auth code directly
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    context.startActivity(intent)
                                    (context as? Activity)?.finish()
                                } else {
                                    Toast.makeText(context, R.string.failed_to_save_auth_code, Toast.LENGTH_LONG).show()
                                    Log.e(TAG, "Failed to save auth code for web panel.")
                                }
                                isLoading = false
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
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(id = R.string.login))
                            }
                        }
                    }
                }
            }
        }
    }
}