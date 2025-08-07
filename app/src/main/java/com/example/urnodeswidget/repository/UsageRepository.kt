package com.example.urnodeswidget.repository

import android.content.Context
import android.util.Log
import com.example.urnodeswidget.data.TransferStatsResponse
import com.example.urnodeswidget.network.RetrofitClient
import com.example.urnodeswidget.util.AuthTokenManager
import com.example.urnodeswidget.security.JwtManager // Import JwtManager to access keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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