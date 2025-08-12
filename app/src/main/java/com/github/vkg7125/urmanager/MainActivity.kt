package com.github.vkg7125.urmanager

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowInsetsControllerCompat
import com.github.vkg7125.urmanager.ui.theme.URnodesWidgetTheme
import com.github.vkg7125.urmanager.util.AuthTokenManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.github.vkg7125.urmanager.security.JwtManager

/**
 * MainActivity serves as the primary screen for displaying the UR.network web panel.
 * It handles authentication logic, loading the web content in a WebView, and ensuring
 * a seamless, edge-to-edge user interface.
 */
class MainActivity : ComponentActivity() {

    private var isWebViewReady by mutableStateOf(false)
    private lateinit var webViewInstance: WebView
    private var currentLoadedAuthCode: String? = null

    private val webLoginPageUrl = "https://app.ur.network/login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(0xFF131829.toInt().toDrawable())

        val desiredDarkColor = 0xFF131829.toInt()

        window.statusBarColor = desiredDarkColor
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        window.navigationBarColor = desiredDarkColor
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false

        // Use the auth token from the intent if available, otherwise use the stored one.
        val retrievedAuthToken = AuthTokenManager.getAuthToken(this, JwtManager.KEY_WEB_PANEL_AUTH_CODE)
        val authTokenFromIntent = intent.getStringExtra("AUTH_CODE_EXTRA")

        val tokenToUse: String?

        if (!authTokenFromIntent.isNullOrBlank()) {
            Log.d("MainActivityAuth", "Using fresh Auth Token from Intent: ${authTokenFromIntent.take(10)}...")
            tokenToUse = authTokenFromIntent
        } else if (!retrievedAuthToken.isNullOrBlank()) {
            Log.d("MainActivityAuth", "Using stored Auth Token: ${retrievedAuthToken.take(10)}...")
            tokenToUse = retrievedAuthToken
        } else {
            Log.w("MainActivityAuth", "No Auth Token found. Redirecting to LoginActivity.")
            redirectToLogin()
            return
        }

        currentLoadedAuthCode = tokenToUse
        initializeWebView(tokenToUse)
    }

    private fun redirectToLogin() {
        // Clear only the web panel's auth token and redirect to the login screen.
        AuthTokenManager.deleteAuthToken(this, JwtManager.KEY_WEB_PANEL_AUTH_CODE)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun initializeWebView(authToken: String) {
        setContent {
            URnodesWidgetTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewInstance = this

                                setBackgroundColor(0xFF131829.toInt())

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isWebViewReady = false
                                        Log.d("WebViewLoad", "Page started loading: $url")
                                        // If the webview tries to navigate to its own login page, redirect to our native login.
                                        if (url?.startsWith(webLoginPageUrl, ignoreCase = true) == true) {
                                            Log.i("WebViewBonus", "Web login page detected ($url). Redirecting to app login.")
                                            redirectToLogin()
                                            view?.stopLoading()
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        Log.d("WebViewLoad", "Page finished loading: $url")
                                        if (!isWebViewReady) { isWebViewReady = true }
                                    }

                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        super.onReceivedError(view, request, error)
                                        val errorMessage = "Code: ${error?.errorCode}, Description: ${error?.description}"
                                        Log.e("WebViewError", "Error: ${request?.url} - $errorMessage")

                                        // If the auth code fails, the user is likely unauthorized. Redirect to login.
                                        val failingUrl = request?.url?.toString() ?: ""
                                        if (failingUrl.contains("?auth_code=$currentLoadedAuthCode")) {
                                            Log.e("WebViewError", "Auth code likely failed for $failingUrl. Redirecting to Login.")
                                            redirectToLogin()
                                        } else if (!isWebViewReady) {
                                            isWebViewReady = true
                                        }
                                    }
                                }

                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true

                                val targetUrl = "https://app.ur.network/?auth_code=$authToken"
                                Log.i("WebViewLoad", "Loading URL with auth_code (len: ${authToken.length}): ${targetUrl.take(targetUrl.indexOf("?auth_code=") + 15)}...")
                                loadUrl(targetUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("AUTH_CODE_EXTRA")?.let { newAuthToken ->
            Log.d("MainActivityAuth", "Received new Auth Token via onNewIntent: ${newAuthToken.take(10)}...")
            currentLoadedAuthCode = newAuthToken
            if (::webViewInstance.isInitialized) {
                val targetUrl = "https://app.ur.network/?auth_code=$newAuthToken"
                Log.i("WebViewLoad", "Reloading URL from onNewIntent: ${targetUrl.take(targetUrl.indexOf("?auth_code=") + 15)}...")
                isWebViewReady = false
                webViewInstance.loadUrl(targetUrl)
            } else {
                Log.w("MainActivityAuth", "WebView not initialized in onNewIntent. Recreating activity.")
                recreate()
            }
        } ?: run {
            Log.w("MainActivityAuth", "onNewIntent called but AUTH_CODE_EXTRA was missing in the intent.")
        }
    }
}