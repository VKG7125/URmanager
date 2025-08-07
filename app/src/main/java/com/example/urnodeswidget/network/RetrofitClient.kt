package com.example.urnodeswidget.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import com.example.urnodeswidget.network.URApiService
import com.example.urnodeswidget.network.AuthService

/**
 * A singleton object that provides configured Retrofit instances for network operations.
 * It centralizes API service creation for the entire application.
 */
object RetrofitClient {
    private const val BASE_URL = "https://api.bringyour.com/"

    private val client = OkHttpClient.Builder().build()

    /**
     * Provides a lazily-initialized Retrofit service for general API interactions.
     */
    val apiService: URApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(URApiService::class.java)
    }

    /**
     * Provides a lazily-initialized Retrofit service specifically for authentication tasks.
     */
    val authService: AuthService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthService::class.java)
    }
}
