package com.meya.doumenculture.ui.state

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meya.doumenculture.data.model.AppNotification
import com.meya.doumenculture.data.model.Venue
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.repository.NotificationRepository
import com.meya.doumenculture.data.repository.VenueRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val venueRepository: VenueRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = mutableStateOf<UiState<Unit>>(UiState.Loading)
    val uiState: State<UiState<Unit>> = _uiState

    private val _venues = mutableStateOf<List<Venue>>(emptyList())
    val venues: State<List<Venue>> = _venues

    private val _notification = mutableStateOf<AppNotification?>(null)
    val notification: State<AppNotification?> = _notification

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val venuesResult = venueRepository.fetchVenues()
            val notificationResult = notificationRepository.fetchActiveNotifications()

            val venuesSuccess = venuesResult is ApiResult.Success
            val notificationSuccess = notificationResult is ApiResult.Success

            if (venuesSuccess && notificationSuccess) {
                _venues.value = (venuesResult as ApiResult.Success).data
                _notification.value = (notificationResult as ApiResult.Success).data
                _uiState.value = UiState.Success(Unit)
            } else {
                val message = when {
                    venuesResult is ApiResult.Error -> venuesResult.message
                    notificationResult is ApiResult.Error -> notificationResult.message
                    else -> "加载失败"
                }
                _uiState.value = UiState.Error(message)
            }
        }
    }
}
