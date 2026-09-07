package com.meya.doumenculture.ui.state

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meya.doumenculture.data.model.Team
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.repository.TeamRepository
import kotlinx.coroutines.launch

class TeamViewModel(
    private val teamRepository: TeamRepository,
    private val userId: Int?
) : ViewModel() {

    private val _uiState = mutableStateOf<UiState<Unit>>(UiState.Loading)
    val uiState: State<UiState<Unit>> = _uiState

    private val _team = mutableStateOf<Team?>(null)
    val team: State<Team?> = _team

    private val _teams = mutableStateOf<List<Team>>(emptyList())
    val teams: State<List<Team>> = _teams

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    init {
        loadTeamData()
    }

    fun loadTeamData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _errorMessage.value = null

            when (val allTeams = teamRepository.fetchTeams()) {
                is ApiResult.Success -> {
                    _teams.value = allTeams.data

                    val userTeamId = _team.value?.id
                        ?: allTeams.data.firstOrNull { team ->
                            team.members.any { it.userId == userId }
                        }?.id

                    if (userTeamId != null) {
                        loadTeamDetail(userTeamId)
                    } else {
                        _team.value = null
                        _uiState.value = UiState.Success(Unit)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = UiState.Error(allTeams.message)
                }
            }
        }
    }

    private suspend fun loadTeamDetail(teamId: Int) {
        when (val result = teamRepository.fetchTeamDetail(teamId)) {
            is ApiResult.Success -> {
                _team.value = result.data
                _uiState.value = UiState.Success(Unit)
            }
            is ApiResult.Error -> {
                _uiState.value = UiState.Error(result.message)
            }
        }
    }

    fun createTeam(name: String, intro: String, onSuccess: () -> Unit) {
        val uid = userId ?: return
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = teamRepository.createTeam(name, intro, uid)) {
                is ApiResult.Success -> {
                    _team.value = result.data
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun joinTeam(teamId: Int, onSuccess: () -> Unit) {
        val uid = userId ?: return
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = teamRepository.joinTeam(teamId, uid)) {
                is ApiResult.Success -> {
                    _team.value = result.data
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun leaveTeam(teamId: Int, onSuccess: () -> Unit) {
        val uid = userId ?: return
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = teamRepository.leaveTeam(teamId, uid)) {
                is ApiResult.Success -> {
                    _team.value = null
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
