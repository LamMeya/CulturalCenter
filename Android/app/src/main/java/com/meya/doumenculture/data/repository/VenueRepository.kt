package com.meya.doumenculture.data.repository

import com.meya.doumenculture.data.model.TimeSlot
import com.meya.doumenculture.data.model.Venue
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.network.ApiService
import com.meya.doumenculture.data.network.safeApiCall

class VenueRepository(private val apiService: ApiService) {

    suspend fun fetchVenues(): ApiResult<List<Venue>> {
        return safeApiCall { apiService.fetchVenues() }
    }

    suspend fun fetchVenueDetail(id: Int): ApiResult<Venue> {
        return safeApiCall { apiService.fetchVenueDetail(id) }
    }

    suspend fun fetchTimeSlots(venueId: Int, date: String): ApiResult<List<TimeSlot>> {
        return safeApiCall { apiService.fetchTimeSlots(venueId, date) }
    }
}
