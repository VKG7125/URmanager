package com.github.vkg7125.urmanager.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Manages the secure storage and retrieval of JWT tokens using EncryptedSharedPreferences.
 */
object JwtManager {

    private const val PREFS_FILENAME = "urnetwork_secure_prefs"
    // Define distinct keys for different types of tokens
    const val KEY_WEB_PANEL_AUTH_CODE = "web_panel_auth_code"
    const val KEY_WIDGET_JWT = "widget_jwt_token"

    /**
     * Retrieves or creates the master key alias for encryption.
     * This key is stored in the Android Keystore.
     *
     * @return The master key alias string.
     * @throws GeneralSecurityException If there is an issue with Keystore operations.
     * @throws IOException If there is an I/O issue during key generation/retrieval.
     */
    private fun getMasterKeyAlias(): String {
        return MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    /**
     * Creates and returns an instance of EncryptedSharedPreferences.
     *
     * @param context The application context.
     * @return An instance of SharedPreferences (backed by EncryptedSharedPreferences).
     * @throws GeneralSecurityException If there is an issue with Keystore/encryption setup.
     * @throws IOException If there is an I/O issue.
     */
    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKeyAlias = getMasterKeyAlias()

        return EncryptedSharedPreferences.create(
            PREFS_FILENAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Saves a token securely using a specified key. If the provided token is null,
     * it clears any existing token for that key.
     *
     * @param context The application context.
     * @param token The token string to save, or null to clear the token.
     * @param key The specific key under which to save/clear the token.
     */
    fun saveJwt(context: Context, token: String?, key: String) {
        try {
            val prefs = getEncryptedPrefs(context)
            if (token == null) {
                prefs.edit().remove(key).apply()
                android.util.Log.i("JwtManager", "Cleared token for key: $key")
            } else {
                prefs.edit().putString(key, token).apply()
                android.util.Log.i("JwtManager", "Saved token for key: $key (length: ${token.length})")
            }
        } catch (e: Exception) {
            android.util.Log.e("JwtManager", "Error saving token for key: $key", e)
        }
    }

    /**
     * Retrieves the saved token for a specified key.
     *
     * @param context The application context.
     * @param key The specific key from which to retrieve the token.
     * @return The token string, or null if not found or if an error occurs.
     */
    fun getJwt(context: Context, key: String): String? {
        return try {
            val prefs = getEncryptedPrefs(context)
            val token = prefs.getString(key, null)
            if (token == null) {
                android.util.Log.d("JwtManager", "No token found for key: $key")
            } else {
                android.util.Log.d("JwtManager", "Token retrieved for key: $key (length: ${token.length})")
            }
            token
        } catch (e: Exception) {
            android.util.Log.e("JwtManager", "Error retrieving token for key: $key", e)
            null
        }
    }

    /**
     * Clears the saved token for a specified key from secure storage.
     *
     * @param context The application context.
     * @param key The specific key for which to clear the token.
     */
    fun clearJwt(context: Context, key: String) {
        try {
            val prefs = getEncryptedPrefs(context)
            prefs.edit().remove(key).apply()
            android.util.Log.i("JwtManager", "Cleared token for key: $key")
        } catch (e: Exception) {
            android.util.Log.e("JwtManager", "Error clearing token for key: $key", e)
        }
    }
}