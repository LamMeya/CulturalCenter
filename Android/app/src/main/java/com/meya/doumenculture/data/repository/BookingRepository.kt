package com.meya.doumenculture.data.repository

import com.meya.doumenculture.data.model.Booking
import com.meya.doumenculture.data.model.CreateBookingRequest
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.network.ApiService
import com.meya.doumenculture.data.network.safeApiCall

class BookingRepository(private val apiService: ApiService) {

    suspend fun createBooking(
        venueId: Int,
        teamId: Int,
        timeSlotIds: List<Int>
    ): ApiResult<Booking> {
        return safeApiCall {
            apiService.createBooking(CreateBookingRequest(venueId, teamId, timeSlotIds))
        }
    }

    suspend fun fetchUserBookings(
        userId: Int,
        queryParams: Map<String, String>
    ): ApiResult<List<Booking>> {
        return safeApiCall { apiService.fetchUserBookings(userId, queryParams) }
    }

    suspend fun cancelBooking(userId: Int, bookingId: Int): ApiResult<Booking> {
        return safeApiCall { apiService.cancelBooking(userId, bookingId) }
    }
}
