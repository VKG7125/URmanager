package com.github.vkg7125.urmanager.repository

import android.content.Context
import android.util.Log
import com.github.vkg7125.urmanager.data.TransferStatsResponse
import com.github.vkg7125.urmanager.network.RetrofitClient
import com.github.vkg7125.urmanager.util.AuthTokenManager
import com.github.vkg7125.urmanager.security.JwtManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for fetching usage data from the URnetwork API.
 * This acts as a single source of truth for network-related data operations.
 */
object UsageRepository {
    private const val TAG = "UsageRepo"

    suspend fun fetchUsageData(context: Context): Long? {
        return withContext(Dispatchers.IO) {
            try {
                // Retrieve token using the widget-specific key
                val token = AuthTokenManager.getAuthToken(context, JwtManager.KEY_WIDGET_JWT)
                Log.d(TAG, "Token: $token")
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "Auth token is null or blank, cannot fetch usage data.")
                    return@withContext null
                }

                val bearer = "Bearer $token"
                val response: TransferStatsResponse = RetrofitClient.apiService.getUsage(bearer)
                
                val unpaidBytes = response.unpaidBytesProvided

                Log.d(TAG, "API → unpaid bytes provided=${unpaidBytes}, paid bytes provided=${response.paidBytesProvided}")
                unpaidBytes
            } catch (e: Exception) {
                Log.e(TAG, "Fetch error: ${e.message}", e)
                null
            }
        }
    }
}