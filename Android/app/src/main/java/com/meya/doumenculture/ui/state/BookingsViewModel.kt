package com.meya.doumenculture.ui.state

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meya.doumenculture.data.model.Booking
import com.meya.doumenculture.data.model.BookingStatus
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.repository.BookingRepository
import com.meya.doumenculture.data.repository.TeamRepository
import com.meya.doumenculture.utils.DateUtils
import kotlinx.coroutines.launch

class BookingsViewModel(
    private val bookingRepository: BookingRepository,
    private val teamRepository: TeamRepository,
    private val userId: Int?
) : ViewModel() {

    private val _uiState = mutableStateOf<UiState<Unit>>(UiState.Loading)
    val uiState: State<UiState<Unit>> = _uiState

    private val _bookings = mutableStateOf<List<Booking>>(emptyList())
    val bookings: State<List<Booking>> = _bookings

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    init {
        loadBookings()
    }

    fun loadBookings() {
        val uid = userId ?: run {
            _uiState.value = UiState.Error("用户未登录")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _errorMessage.value = null

            val queryParams = mutableMapOf<String, String>()
            try {
                when (val teamsResult = teamRepository.fetchTeams(uid)) {
                    is ApiResult.Success -> {
                        teamsResult.data.firstOrNull()?.let {
                            queryParams["team_id"] = it.id.toString()
                        } ?: run {
                            queryParams["user_id"] = uid.toString()
                        }
                    }
                    is ApiResult.Error -> {
                        queryParams["user_id"] = uid.toString()
                    }
                }
            } catch (_: Exception) {
                queryParams["user_id"] = uid.toString()
            }

            when (val result = bookingRepository.fetchUserBookings(uid, queryParams)) {
                is ApiResult.Success -> {
                    _bookings.value = result.data
                    _uiState.value = UiState.Success(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun cancelBooking(booking: Booking) {
        val uid = userId ?: return
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = bookingRepository.cancelBooking(uid, booking.id)) {
                is ApiResult.Success -> loadBookings()
                is ApiResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun canCancel(booking: Booking): Boolean {
        val statusOk = booking.status == BookingStatus.PENDING || booking.status == BookingStatus.WON
        val bookingDate = booking.timeSlot?.date ?: return false
        val today = DateUtils.formatApi(DateUtils.today())
        return statusOk && bookingDate >= today
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
