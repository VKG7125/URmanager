package com.example.urnodeswidget.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import com.example.urnodeswidget.network.URApiService
import com.example.urnodeswidget.network.AuthService // Import AuthService

object RetrofitClient {
    // Changed BASE_URL to the correct API domain
    private const val BASE_URL = "https://api.bringyour.com/"

    private val client = OkHttpClient.Builder().build()

    val apiService: URApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(URApiService::class.java)
    }

    val authService: AuthService by lazy { // Re-added authService
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthService::class.java)
    }
}