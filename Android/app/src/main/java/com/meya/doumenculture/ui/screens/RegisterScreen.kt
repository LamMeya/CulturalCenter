package com.meya.doumenculture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meya.doumenculture.ui.state.AuthViewModel
import com.meya.doumenculture.ui.state.UiState
import com.meya.doumenculture.utils.Background
import com.meya.doumenculture.utils.Muted
import com.meya.doumenculture.utils.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
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
            title = { Text("注册失败") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("确定")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("注册账号") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("取消")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 32.dp, vertical = 20.dp)
        ) {
            AuthTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "账号（用于登录）",
                leadingIcon = Icons.Default.AccountCircle
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = "昵称",
                leadingIcon = Icons.Default.Face
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "手机号（选填）",
                leadingIcon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "密码",
                leadingIcon = Icons.Default.Lock,
                isPassword = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "确认密码",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(24.dp))

            val canRegister = username.isNotBlank()
                    && password.length >= 4
                    && password == confirmPassword

            Button(
                onClick = {
                    val nick = nickname.trim().ifEmpty { username.trim() }
                    authViewModel.register(
                        username = username.trim(),
                        password = password,
                        nickname = nick,
                        phone = phone.trim()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canRegister && uiState !is UiState.Loading,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Muted.copy(alpha = 0.4f)
                )
            ) {
                if (uiState is UiState.Loading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                } else {
                    Text("注册", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Muted
            )
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
