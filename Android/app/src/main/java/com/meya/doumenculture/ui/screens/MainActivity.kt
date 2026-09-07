package com.meya.doumenculture.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meya.doumenculture.data.local.TokenManager
import com.meya.doumenculture.data.network.RetrofitClient
import com.meya.doumenculture.data.repository.Injection
import com.meya.doumenculture.ui.state.AuthViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(applicationContext)
        RetrofitClient.init(TokenManager)

        setContent {
            androidx.compose.material3.MaterialTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel {
                    AuthViewModel(Injection.authRepository, Injection.userRepository)
                }
                val isLoggedIn by authViewModel.isLoggedIn

                LaunchedEffect(isLoggedIn) {
                    val current = navController.currentDestination?.route
                    if (isLoggedIn && current != "main") {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else if (!isLoggedIn && current != "login") {
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "main" else "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onRegisterClick = { navController.navigate("register") }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            authViewModel = authViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("main") {
                        MainTabScreen(authViewModel = authViewModel)
                    }
                }
            }
        }
    }
}
