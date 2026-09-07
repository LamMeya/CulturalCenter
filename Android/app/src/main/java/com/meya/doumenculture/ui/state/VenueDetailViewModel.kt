package com.meya.doumenculture.ui.state

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meya.doumenculture.data.model.Booking
import com.meya.doumenculture.data.model.SlotPeriod
import com.meya.doumenculture.data.model.TimeSlot
import com.meya.doumenculture.data.model.Venue
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.repository.BookingRepository
import com.meya.doumenculture.data.repository.VenueRepository
import com.meya.doumenculture.utils.DateUtils
import kotlinx.coroutines.launch
import java.time.LocalDate

class VenueDetailViewModel(
    private val venueRepository: VenueRepository,
    private val bookingRepository: BookingRepository,
    private val venueId: Int
) : ViewModel() {

    private val _uiState = mutableStateOf<UiState<Unit>>(UiState.Loading)
    val uiState: State<UiState<Unit>> = _uiState

    private val _venue = mutableStateOf<Venue?>(null)
    val venue: State<Venue?> = _venue

    private val _selectedDate = mutableStateOf(DateUtils.today())
    val selectedDate: State<LocalDate> = _selectedDate

    private val _slots = mutableStateOf<List<TimeSlot>>(emptyList())
    val slots: State<List<TimeSlot>> = _slots

    private val _selectedSlotIds = mutableStateOf<Set<Int>>(emptySet())
    val selectedSlotIds: State<Set<Int>> = _selectedSlotIds

    private val _isSubmitting = mutableStateOf(false)
    val isSubmitting: State<Boolean> = _isSubmitting

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _success = mutableStateOf(false)
    val success: State<Boolean> = _success

    private val maxSlots = 4

    init {
        loadVenue()
        loadTimeSlots()
    }

    private fun loadVenue() {
        viewModelScope.launch {
            when (val result = venueRepository.fetchVenueDetail(venueId)) {
                is ApiResult.Success -> _venue.value = result.data
                is ApiResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun loadTimeSlots() {
        viewModelScope.launch {
            _slots.value = emptyList()
            _selectedSlotIds.value = emptySet()
            _uiState.value = UiState.Loading

            when (val result = venueRepository.fetchTimeSlots(
                venueId,
                DateUtils.formatApi(_selectedDate.value)
            )) {
                is ApiResult.Success -> {
                    _slots.value = result.data
                    _uiState.value = UiState.Success(Unit)
                }
                is ApiResult.Error -> {
                    _slots.value = emptyList()
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _selectedSlotIds.value = emptySet()
        loadTimeSlots()
    }

    fun toggleSlot(slot: TimeSlot) {
        if (!slot.isOpen || slot.isBooked) return

        val current = _selectedSlotIds.value
        _selectedSlotIds.value = when {
            current.contains(slot.id) -> current - slot.id
            current.size >= maxSlots -> current
            else -> current + slot.id
        }
    }

    fun submitBooking(teamId: Int?, onNoTeam: () -> Unit) {
        if (teamId == null) {
            onNoTeam()
            return
        }
        if (_selectedSlotIds.value.isEmpty()) return

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            when (val result = bookingRepository.createBooking(
                venueId = venueId,
                teamId = teamId,
                timeSlotIds = _selectedSlotIds.value.toList()
            )) {
                is ApiResult.Success -> {
                    _isSubmitting.value = false
                    _success.value = true
                }
                is ApiResult.Error -> {
                    _isSubmitting.value = false
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun dismissSuccess() {
        _success.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun morningSlots(): List<TimeSlot> = _slots.value.filter { it.period == SlotPeriod.MORNING }
    fun afternoonSlots(): List<TimeSlot> = _slots.value.filter { it.period == SlotPeriod.AFTERNOON }
}
