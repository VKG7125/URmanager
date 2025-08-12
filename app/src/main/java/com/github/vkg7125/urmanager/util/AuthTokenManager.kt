package com.github.vkg7125.urmanager.util

import android.content.Context
import android.util.Log
import com.github.vkg7125.urmanager.security.JwtManager

/**
 * Manages the secure storage and retrieval of authentication tokens (JWT).
 * This manager now uses specific keys to differentiate between tokens.
 */
object AuthTokenManager {
    private const val TAG = "AuthTokenManager"

    /**
     * Retrieves the authentication token (JWT) from secure storage for a given key.
     *
     * @param context The application context.
     * @param key The specific key for the token (e.g., JwtManager.KEY_WEB_PANEL_AUTH_CODE or JwtManager.KEY_WIDGET_JWT).
     * @return The JWT token string, or null if not found or an error occurs.
     */
    fun getAuthToken(context: Context, key: String): String? {
        val authToken = JwtManager.getJwt(context, key)
        if (authToken == null) {
            Log.d(TAG, "No auth token found for key: $key.")
        } else {
            Log.d(TAG, "Auth token retrieved for key: $key (length: ${authToken.length}).")
        }
        return authToken
    }

    /**
     * Saves the authentication token (JWT) securely for a given key.
     *
     * @param context The application context.
     * @param authToken The JWT token string to save.
     * @param key The specific key for the token (e.g., JwtManager.KEY_WEB_PANEL_AUTH_CODE or JwtManager.KEY_WIDGET_JWT).
     * @return True if the auth token was saved successfully, false otherwise.
     */
    fun saveAuthToken(context: Context, authToken: String, key: String): Boolean {
        return try {
            JwtManager.saveJwt(context, authToken, key)
            Log.i(TAG, "Auth token saved securely for key: $key.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving auth token securely for key: $key", e)
            false
        }
    }

    /**
     * Deletes the authentication token (JWT) from secure storage for a given key.
     *
     * @param context The application context.
     * @param key The specific key for the token (e.g., JwtManager.KEY_WEB_PANEL_AUTH_CODE or JwtManager.KEY_WIDGET_JWT).
     * @return True if the auth token was deleted successfully, false otherwise.
     */
    fun deleteAuthToken(context: Context, key: String): Boolean {
        return try {
            JwtManager.clearJwt(context, key)
            Log.i(TAG, "Auth token cleared from secure storage for key: $key.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing auth token securely for key: $key", e)
            false
        }
    }
}