package com.meya.doumenculture.ui.state

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meya.doumenculture.data.model.Team
import com.meya.doumenculture.data.model.User
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.repository.TeamRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val teamRepository: TeamRepository,
    private val user: User?
) : ViewModel() {

    private val _team = mutableStateOf<Team?>(null)
    val team: State<Team?> = _team

    init {
        loadTeamInfo()
    }

    fun loadTeamInfo() {
        val teamId = user?.teamId ?: return
        viewModelScope.launch {
            when (val result = teamRepository.fetchTeamDetail(teamId)) {
                is ApiResult.Success -> _team.value = result.data
                is ApiResult.Error -> { /* ignore */ }
            }
        }
    }
}
