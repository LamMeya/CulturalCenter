package com.meya.doumenculture.data.repository

import com.meya.doumenculture.data.model.AppNotification
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.network.ApiService
import com.meya.doumenculture.data.network.safeApiCall

class NotificationRepository(private val apiService: ApiService) {

    suspend fun fetchActiveNotifications(): ApiResult<AppNotification> {
        return safeApiCall { apiService.fetchActiveNotifications() }
    }
}
