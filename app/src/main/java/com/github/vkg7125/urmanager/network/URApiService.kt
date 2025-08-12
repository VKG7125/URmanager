package com.github.vkg7125.urmanager.network

import com.github.vkg7125.urmanager.data.TransferStatsResponse // Import TransferStatsResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface URApiService {
    @GET("transfer/stats") // Changed endpoint to transfer/stats
    suspend fun getUsage(@Header("Authorization") authorizationHeader: String): TransferStatsResponse // Changed return type to TransferStatsResponse
}