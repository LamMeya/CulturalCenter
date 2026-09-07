package com.meya.doumenculture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meya.doumenculture.data.model.Team
import com.meya.doumenculture.data.model.TeamMember
import com.meya.doumenculture.data.repository.Injection
import com.meya.doumenculture.ui.state.TeamViewModel
import com.meya.doumenculture.ui.state.UiState
import com.meya.doumenculture.utils.Accent
import com.meya.doumenculture.utils.Background
import com.meya.doumenculture.utils.Border
import com.meya.doumenculture.utils.Card as CardColor
import com.meya.doumenculture.utils.Muted
import com.meya.doumenculture.utils.Primary
import com.meya.doumenculture.utils.PrimaryDark
import com.meya.doumenculture.utils.TextPrimary
import com.meya.doumenculture.utils.teamRoleColor

@Composable
fun TeamScreen(
    userId: Int?,
    onUserChanged: () -> Unit,
    teamViewModel: TeamViewModel = viewModel {
        TeamViewModel(Injection.teamRepository, userId)
    }
) {
    val uiState = teamViewModel.uiState.value
    val team = teamViewModel.team.value
    val teams = teamViewModel.teams.value
    val errorMessage = teamViewModel.errorMessage.value

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            teamViewModel.clearError()
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { teamViewModel.clearError() },
            title = { Text("提示") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { teamViewModel.clearError() }) {
                    Text("确定")
                }
            }
        )
    }

    if (showCreateDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, intro ->
                teamViewModel.createTeam(name, intro) {
                    showCreateDialog = false
                    onUserChanged()
                }
            }
        )
    }

    if (showJoinDialog) {
        JoinTeamDialog(
            teams = teams,
            onDismiss = { showJoinDialog = false },
            onJoin = { teamId ->
                teamViewModel.joinTeam(teamId) {
                    showJoinDialog = false
                    onUserChanged()
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when (uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Primary
                )
            }
            is UiState.Error -> {
                ErrorMessage(message = uiState.message, onRetry = { teamViewModel.loadTeamData() })
            }
            is UiState.Success -> {
                if (team != null) {
                    TeamDetailContent(
                        team = team,
                        userId = userId,
                        onLeave = { teamId ->
                            teamViewModel.leaveTeam(teamId) {
                                onUserChanged()
                            }
                        }
                    )
                } else {
                    NoTeamContent(
                        onCreate = { showCreateDialog = true },
                        onJoin = { showJoinDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamDetailContent(
    team: Team,
    userId: Int?,
    onLeave: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Primary, PrimaryDark))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = team.name.take(1),
                        fontSize = 28.sp,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = team.name,
                    fontSize = 20.sp,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = team.intro,
                    fontSize = 14.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    StatItem(value = team.memberCount.toString(), label = "成员")
                    StatItem(value = team.maxMembers.toString(), label = "上限")
                    StatItem(value = team.leaderName, label = "团长")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "团队成员",
            fontSize = 16.sp,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column {
                team.members.forEachIndexed { index, member ->
                    TeamMemberRow(member = member)
                    if (index != team.members.lastIndex) {
                        HorizontalDivider(
                            color = Border,
                            modifier = Modifier.padding(start = 56.dp)
                        )
                    }
                }
            }
        }

        if (team.members.any { it.userId == userId }) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onLeave(team.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("退出团队", color = Color.Red)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            color = Primary,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Muted
        )
    }
}

@Composable
private fun TeamMemberRow(member: TeamMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.nickname.take(1),
                fontSize = 16.sp,
                color = Primary
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = member.nickname,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Text(
                text = member.role.displayName,
                fontSize = 12.sp,
                color = teamRoleColor(member.role.name)
            )
        }
    }
}

@Composable
private fun NoTeamContent(onCreate: () -> Unit, onJoin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = Muted.copy(alpha = 0.3f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "你还没有加入团队",
            fontSize = 18.sp,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "加入或创建一个团队，即可预约场地",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("创建团队", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onJoin,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("加入团队", fontSize = 16.sp, color = Primary)
        }
    }
}

@Composable
private fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var intro by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建团队") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("团队名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = intro,
                    onValueChange = { intro = it },
                    label = { Text("团队简介") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), intro.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun JoinTeamDialog(
    teams: List<Team>,
    onDismiss: () -> Unit,
    onJoin: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入团队") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (teams.isEmpty()) {
                    Text("暂无可加入的团队", color = Muted)
                } else {
                    teams.forEach { team ->
                        TextButton(
                            onClick = { onJoin(team.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = team.name,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${team.memberCount}/${team.maxMembers}人 | ${team.leaderName}",
                                    fontSize = 13.sp,
                                    color = Muted
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = Muted)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
