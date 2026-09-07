package com.meya.doumenculture.data.model

import com.google.gson.annotations.SerializedName

// MARK: - User

data class User(
    val id: Int,
    var nickname: String,
    val phone: String? = null,
    @SerializedName("avatar_url") val avatarURL: String? = null,
    @SerializedName("team_id") val teamId: Int? = null,
    @SerializedName("team_name") val teamName: String? = null,
    @SerializedName("team_role") val teamRole: String? = null
)

// MARK: - Venue

data class Venue(
    val id: Int,
    val name: String,
    @SerializedName("image_url") val imageURL: String? = null,
    val description: String = "",
    val capacity: String = "",
    val area: String = "",
    val facilities: List<String> = emptyList(),
    val address: String = "",
    @SerializedName("is_active") val isOpen: Boolean = false
)

// MARK: - TimeSlot

data class TimeSlot(
    val id: Int,
    @SerializedName("venue_id") val venueId: Int,
    @SerializedName("available_date") val availableDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    val period: SlotPeriod,
    @SerializedName("is_open") val isOpen: Boolean = false,
    @SerializedName("is_booked") val isBooked: Boolean = false
)

enum class SlotPeriod(val displayName: String) {
    @SerializedName("morning")
    MORNING("上午"),

    @SerializedName("afternoon")
    AFTERNOON("下午")
}

// MARK: - Booking

data class Booking(
    val id: Int,
    @SerializedName("venue_name") val venueName: String,
    @SerializedName("team_name") val teamName: String,
    @SerializedName("time_slot") val timeSlot: BookingTimeSlot? = null,
    val status: BookingStatus,
    @SerializedName("cancel_reason") val cancelReason: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

data class BookingTimeSlot(
    val date: String? = null,
    val start: String? = null,
    val end: String? = null
)

enum class BookingStatus(val displayName: String, val colorHex: String) {
    @SerializedName("pending")
    PENDING("待抽签", "#e8a840"),

    @SerializedName("won")
    WON("已中签", "#3d8e7a"),

    @SerializedName("lost")
    LOST("未中签", "#8c7b6a"),

    @SerializedName("cancelled")
    CANCELLED("已取消", "#8c7b6a")
}

// MARK: - Team

data class Team(
    val id: Int,
    val name: String,
    val intro: String = "",
    @SerializedName("member_count") val memberCount: Int = 0,
    @SerializedName("max_members") val maxMembers: Int = 0,
    @SerializedName("leader_id") val leaderId: Int? = null,
    @SerializedName("leader_name") val leaderName: String = "",
    val members: List<TeamMember> = emptyList(),
    @SerializedName("created_at") val createdAt: String = ""
)

data class TeamMember(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    val nickname: String,
    @SerializedName("avatar_url") val avatarURL: String? = null,
    val role: TeamRole
)

enum class TeamRole(val displayName: String) {
    @SerializedName("leader")
    LEADER("团长"),

    @SerializedName("vice_leader")
    VICE_LEADER("副团长"),

    @SerializedName("member")
    MEMBER("团员")
}

// MARK: - Notification

data class AppNotification(
    val id: Int? = null,
    val title: String? = null,
    val content: String? = null,
    @SerializedName("notif_type") val notifType: String? = null
)

// MARK: - API Request / Response

data class PasswordLoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String,
    val phone: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class CreateBookingRequest(
    @SerializedName("venue_id") val venueId: Int,
    @SerializedName("team_id") val teamId: Int,
    @SerializedName("time_slot_ids") val timeSlotIds: List<Int>
)

data class CreateTeamRequest(
    val name: String,
    val intro: String,
    @SerializedName("user_id") val userId: Int? = null
)

data class JoinTeamRequest(
    @SerializedName("team_id") val teamId: Int,
    @SerializedName("user_id") val userId: Int
)

data class LeaveTeamRequest(
    @SerializedName("user_id") val userId: Int
)

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null
)

class EmptyResponse
