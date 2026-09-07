package com.meya.doumenculture.data.repository

import com.google.gson.Gson
import com.meya.doumenculture.data.local.TokenManager
import com.meya.doumenculture.data.model.LoginResponse
import com.meya.doumenculture.data.model.PasswordLoginRequest
import com.meya.doumenculture.data.model.RegisterRequest
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.network.ApiService
import com.meya.doumenculture.data.network.safeApiCall

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    private val gson = Gson()

    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        return safeApiCall {
            apiService.login(PasswordLoginRequest(username, password))
        }
    }

    suspend fun register(
        username: String,
        password: String,
        nickname: String,
        phone: String
    ): ApiResult<LoginResponse> {
        return safeApiCall {
            apiService.register(RegisterRequest(username, password, nickname, phone))
        }
    }

    suspend fun saveSession(response: LoginResponse) {
        tokenManager.saveToken(response.token)
        tokenManager.saveUserJson(gson.toJson(response.user))
    }

    suspend fun clearSession() {
        tokenManager.clear()
    }
}
