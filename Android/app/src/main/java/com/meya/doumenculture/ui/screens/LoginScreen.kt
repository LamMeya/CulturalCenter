package com.meya.doumenculture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meya.doumenculture.ui.state.AuthViewModel
import com.meya.doumenculture.ui.state.UiState
import com.meya.doumenculture.utils.Accent
import com.meya.doumenculture.utils.Background
import com.meya.doumenculture.utils.Card
import com.meya.doumenculture.utils.Muted
import com.meya.doumenculture.utils.Primary
import com.meya.doumenculture.utils.PrimaryDark
import com.meya.doumenculture.utils.TextPrimary

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onRegisterClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val uiState = authViewModel.uiState.value

    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            showError = true
            errorMessage = uiState.message
            authViewModel.clearError()
        }
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("登录失败") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("确定")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        WaveHeader()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("请输入账号") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Muted
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("请输入密码") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Muted
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val canLogin = username.isNotBlank() && password.isNotEmpty()

            Button(
                onClick = { authViewModel.login(username.trim(), password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canLogin && uiState !is UiState.Loading,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Muted.copy(alpha = 0.4f)
                )
            ) {
                if (uiState is UiState.Loading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Card
                    )
                } else {
                    Text("登录", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onRegisterClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("没有账号？立即注册", color = Primary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "登录即表示同意《用户协议》和《隐私政策》",
                fontSize = 12.sp,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WaveHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(Primary, PrimaryDark)
                )
            )
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Card
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "珠海文化中心",
                fontSize = 28.sp,
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "场地预约平台",
                fontSize = 16.sp,
                color = Muted
            )
        }
    }
}
