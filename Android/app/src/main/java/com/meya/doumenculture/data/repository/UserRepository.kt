package com.meya.doumenculture.data.repository

import com.meya.doumenculture.data.model.User
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.network.ApiService
import com.meya.doumenculture.data.network.safeApiCall

class UserRepository(private val apiService: ApiService) {

    suspend fun fetchUserProfile(userId: Int): ApiResult<User> {
        return safeApiCall { apiService.fetchUserProfile(userId) }
    }
}
