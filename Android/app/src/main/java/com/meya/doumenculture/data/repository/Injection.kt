package com.meya.doumenculture.data.repository

import com.meya.doumenculture.data.local.TokenManager
import com.meya.doumenculture.data.network.RetrofitClient

object Injection {

    val authRepository: AuthRepository by lazy {
        AuthRepository(RetrofitClient.apiService, TokenManager)
    }

    val venueRepository: VenueRepository by lazy {
        VenueRepository(RetrofitClient.apiService)
    }

    val teamRepository: TeamRepository by lazy {
        TeamRepository(RetrofitClient.apiService)
    }

    val bookingRepository: BookingRepository by lazy {
        BookingRepository(RetrofitClient.apiService)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(RetrofitClient.apiService)
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(RetrofitClient.apiService)
    }
}
