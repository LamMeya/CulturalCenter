package com.meya.doumenculture.data.repository

import com.meya.doumenculture.data.model.CreateTeamRequest
import com.meya.doumenculture.data.model.EmptyResponse
import com.meya.doumenculture.data.model.JoinTeamRequest
import com.meya.doumenculture.data.model.LeaveTeamRequest
import com.meya.doumenculture.data.model.Team
import com.meya.doumenculture.data.network.ApiResult
import com.meya.doumenculture.data.network.ApiService
import com.meya.doumenculture.data.network.safeApiCall

class TeamRepository(private val apiService: ApiService) {

    suspend fun fetchTeams(userId: Int? = null): ApiResult<List<Team>> {
        return safeApiCall { apiService.fetchTeams(userId) }
    }

    suspend fun createTeam(name: String, intro: String, userId: Int): ApiResult<Team> {
        return safeApiCall { apiService.createTeam(CreateTeamRequest(name, intro, userId)) }
    }

    suspend fun fetchTeamDetail(id: Int): ApiResult<Team> {
        return safeApiCall { apiService.fetchTeamDetail(id) }
    }

    suspend fun joinTeam(teamId: Int, userId: Int): ApiResult<Team> {
        return safeApiCall { apiService.joinTeam(teamId, JoinTeamRequest(teamId, userId)) }
    }

    suspend fun leaveTeam(teamId: Int, userId: Int): ApiResult<EmptyResponse> {
        return safeApiCall { apiService.leaveTeam(teamId, LeaveTeamRequest(userId)) }
    }
}
