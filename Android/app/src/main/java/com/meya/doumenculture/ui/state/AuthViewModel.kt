package com.meya.doumenculture.ui.state

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.meya.doumenculture.data.model.User
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.repository.AuthRepository
import com.meya.doumenculture.data.repository.UserRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val gson = Gson()

    private val _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> = _isLoggedIn

    private val _user = mutableStateOf<User?>(null)
    val user: State<User?> = _user

    private val _uiState = mutableStateOf<UiState<Unit>>(UiState.Loading)
    val uiState: State<UiState<Unit>> = _uiState

    init {
        viewModelScope.launch {
            restoreSession()
        }
    }

    private suspend fun restoreSession() {
        val token = com.meya.doumenculture.data.local.TokenManager.getToken()
        val userJson = com.meya.doumenculture.data.local.TokenManager.getUserJson()
        if (!token.isNullOrBlank() && !userJson.isNullOrBlank()) {
            try {
                _user.value = gson.fromJson(userJson, User::class.java)
                _isLoggedIn.value = true
            } catch (_: Exception) {
                _isLoggedIn.value = false
            }
        }
        _uiState.value = UiState.Success(Unit)
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = authRepository.login(username, password)) {
                is ApiResult.Success -> {
                    authRepository.saveSession(result.data)
                    _user.value = result.data.user
                    _isLoggedIn.value = true
                    _uiState.value = UiState.Success(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun register(
        username: String,
        password: String,
        nickname: String,
        phone: String
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = authRepository.register(username, password, nickname, phone)) {
                is ApiResult.Success -> {
                    authRepository.saveSession(result.data)
                    _user.value = result.data.user
                    _isLoggedIn.value = true
                    _uiState.value = UiState.Success(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(result.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearSession()
            _user.value = null
            _isLoggedIn.value = false
            _uiState.value = UiState.Success(Unit)
        }
    }

    fun refreshUser() {
        viewModelScope.launch {
            val userId = _user.value?.id ?: return@launch
            when (val result = userRepository.fetchUserProfile(userId)) {
                is ApiResult.Success -> {
                    _user.value = result.data
                    com.meya.doumenculture.data.local.TokenManager.saveUserJson(
                        gson.toJson(result.data)
                    )
                }
                is ApiResult.Error -> { /* ignore */ }
            }
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Success(Unit)
        }
    }
}
