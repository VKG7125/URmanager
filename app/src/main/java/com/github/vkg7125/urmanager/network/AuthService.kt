package com.github.vkg7125.urmanager.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class AuthResponse(
    @SerializedName("by_jwt")
    val token: String
)

interface AuthService {
    @POST("auth/code-login")
    suspend fun loginWithCode(@Body authCode: Map<String, String>): Response<AuthResponse>
}