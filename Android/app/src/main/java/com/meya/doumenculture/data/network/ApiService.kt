package com.meya.doumenculture.data.network

import com.meya.doumenculture.data.model.ApiResponse
import com.meya.doumenculture.data.model.AppNotification
import com.meya.doumenculture.data.model.Booking
import com.meya.doumenculture.data.model.CreateBookingRequest
import com.meya.doumenculture.data.model.CreateTeamRequest
import com.meya.doumenculture.data.model.EmptyResponse
import com.meya.doumenculture.data.model.JoinTeamRequest
import com.meya.doumenculture.data.model.LeaveTeamRequest
import com.meya.doumenculture.data.model.LoginResponse
import com.meya.doumenculture.data.model.PasswordLoginRequest
import com.meya.doumenculture.data.model.RegisterRequest
import com.meya.doumenculture.data.model.Team
import com.meya.doumenculture.data.model.TimeSlot
import com.meya.doumenculture.data.model.User
import com.meya.doumenculture.data.model.Venue
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiService {

    // MARK: - Auth

    @POST("users/login/password")
    suspend fun login(@Body body: PasswordLoginRequest): ApiResponse<LoginResponse>

    @POST("users/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<LoginResponse>

    // MARK: - Venues

    @GET("venues")
    suspend fun fetchVenues(): ApiResponse<List<Venue>>

    @GET("venues/{id}")
    suspend fun fetchVenueDetail(@Path("id") id: Int): ApiResponse<Venue>

    @GET("venues/{venueId}/time-slots")
    suspend fun fetchTimeSlots(
        @Path("venueId") venueId: Int,
        @Query("date") date: String
    ): ApiResponse<List<TimeSlot>>

    // MARK: - Teams

    @GET("teams")
    suspend fun fetchTeams(@Query("user_id") userId: Int? = null): ApiResponse<List<Team>>

    @POST("teams")
    suspend fun createTeam(@Body body: CreateTeamRequest): ApiResponse<Team>

    @GET("teams/{id}")
    suspend fun fetchTeamDetail(@Path("id") id: Int): ApiResponse<Team>

    @POST("teams/{teamId}/join")
    suspend fun joinTeam(
        @Path("teamId") teamId: Int,
        @Body body: JoinTeamRequest
    ): ApiResponse<Team>

    @POST("teams/{teamId}/leave")
    suspend fun leaveTeam(
        @Path("teamId") teamId: Int,
        @Body body: LeaveTeamRequest
    ): ApiResponse<EmptyResponse>

    // MARK: - Bookings

    @POST("bookings")
    suspend fun createBooking(@Body body: CreateBookingRequest): ApiResponse<Booking>

    @GET("users/{userId}/bookings")
    suspend fun fetchUserBookings(
        @Path("userId") userId: Int,
        @QueryMap params: Map<String, String>
    ): ApiResponse<List<Booking>>

    @POST("users/{userId}/bookings/{bookingId}/cancel")
    suspend fun cancelBooking(
        @Path("userId") userId: Int,
        @Path("bookingId") bookingId: Int
    ): ApiResponse<Booking>

    // MARK: - Users

    @GET("users/{userId}/profile")
    suspend fun fetchUserProfile(@Path("userId") userId: Int): ApiResponse<User>

    // MARK: - Notifications

    @GET("notifications/published")
    suspend fun fetchActiveNotifications(): ApiResponse<AppNotification>
}
