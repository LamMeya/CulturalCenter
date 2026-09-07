package com.meya.doumenculture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meya.doumenculture.data.model.User
import com.meya.doumenculture.data.repository.Injection
import com.meya.doumenculture.ui.state.AuthViewModel
import com.meya.doumenculture.ui.state.ProfileViewModel
import com.meya.doumenculture.utils.Accent
import com.meya.doumenculture.utils.Background
import com.meya.doumenculture.utils.Border
import com.meya.doumenculture.utils.Card as CardColor
import com.meya.doumenculture.utils.Muted
import com.meya.doumenculture.utils.Primary
import com.meya.doumenculture.utils.PrimaryDark
import com.meya.doumenculture.utils.TextPrimary

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel = viewModel {
        ProfileViewModel(Injection.teamRepository, authViewModel.user.value)
    }
) {
    val user = authViewModel.user.value
    val team = profileViewModel.team.value
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("退出后需要重新登录才能使用。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.logout()
                        showLogoutDialog = false
                    }
                ) {
                    Text("确定退出", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        UserInfoCard(user = user)

        if (team != null) {
            TeamInfoCard(teamName = team.name, role = user?.teamRole)
        }

        QuickActionsSection()
        SettingsSection()

        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardColor)
        ) {
            Text("退出登录", color = Color.Red, fontSize = 16.sp)
        }
    }
}

@Composable
private fun UserInfoCard(user: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Primary, PrimaryDark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.nickname?.take(1) ?: "?",
                    fontSize = 24.sp,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = user?.nickname ?: "未知用户",
                    fontSize = 18.sp,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                user?.phone?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TeamInfoCard(teamName: String, role: String?) {
    val roleDisplay = when (role) {
        "leader" -> "团长"
        "vice_leader" -> "副团长"
        "member" -> "团员"
        else -> role ?: ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = teamName,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                if (roleDisplay.isNotBlank()) {
                    Text(
                        text = roleDisplay,
                        fontSize = 13.sp,
                        color = Muted
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QuickActionsSection() {
    Column {
        SectionHeader(title = "快捷操作")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column {
                QuickActionRow(
                    icon = Icons.Default.CalendarToday,
                    iconColor = Primary,
                    title = "我的预约",
                    subtitle = "查看预约记录"
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Border)
                QuickActionRow(
                    icon = Icons.Default.Group,
                    iconColor = Accent,
                    title = "我的团队",
                    subtitle = "管理团队成员"
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Border)
                QuickActionRow(
                    icon = Icons.Default.Notifications,
                    iconColor = Muted,
                    title = "消息通知",
                    subtitle = "查看系统通知"
                )
            }
        }
    }
}

@Composable
private fun SettingsSection() {
    Column {
        SectionHeader(title = "设置")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column {
                QuickActionRow(
                    icon = Icons.Default.Settings,
                    iconColor = Muted,
                    title = "通用设置",
                    subtitle = null
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Border)
                QuickActionRow(
                    icon = Icons.Default.Person,
                    iconColor = Muted,
                    title = "用户协议",
                    subtitle = null
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Border)
                QuickActionRow(
                    icon = Icons.Default.Lock,
                    iconColor = Muted,
                    title = "隐私政策",
                    subtitle = null
                )
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Border)
                QuickActionRow(
                    icon = Icons.Default.Info,
                    iconColor = Muted,
                    title = "关于",
                    subtitle = "v1.0.0"
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        color = Muted,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = TextPrimary
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Muted
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Border,
            modifier = Modifier.size(20.dp)
        )
    }
}
