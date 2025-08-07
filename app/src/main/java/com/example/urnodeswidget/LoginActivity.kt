package com.example.urnodeswidget

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.urnodeswidget.ui.theme.URnodesWidgetTheme
import com.example.urnodeswidget.util.AuthTokenManager
import com.example.urnodeswidget.security.JwtManager

class LoginActivity : ComponentActivity() {

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition. This must be called before super.onCreate().
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Replicating the window setup from MainActivity for consistency
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(0xFF131829.toInt().toDrawable())

        val desiredDarkColor = 0xFF131829.toInt()
        window.statusBarColor = desiredDarkColor
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        window.navigationBarColor = desiredDarkColor
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false

        // Check if user is already logged in using AuthTokenManager with the web panel key
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
                            text = "Webpanel Login",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Enter your auth code to login into webpanel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(32.dp))

                        OutlinedTextField(
                            value = longAuthCodeInput,
                            onValueChange = { longAuthCodeInput = it },
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
                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (longAuthCodeInput.isBlank()) {
                                    Toast.makeText(context, "Please enter the auth code", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (longAuthCodeInput.length < 100) {
                                    Toast.makeText(context, "Auth code seems too short", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isLoading = true
                                Log.d(TAG, "Auth Code input length: ${longAuthCodeInput.length}")

                                // Directly save the provided auth code using the web panel key
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
                                    Toast.makeText(context, "Failed to save auth code.", Toast.LENGTH_LONG).show()
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
                                Text("Login")
                            }
                        }
                    }
                }
            }
        }
    }
}