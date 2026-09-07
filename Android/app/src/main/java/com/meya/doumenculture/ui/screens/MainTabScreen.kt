package com.meya.doumenculture.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meya.doumenculture.ui.state.AuthViewModel
import com.meya.doumenculture.utils.Card
import com.meya.doumenculture.utils.Muted
import com.meya.doumenculture.utils.Primary

private sealed class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : TabItem("home", "首页", Icons.Default.Home)
    object Team : TabItem("team", "团队", Icons.Default.Group)
    object Bookings : TabItem("bookings", "预约", Icons.Default.CalendarToday)
    object Profile : TabItem("profile", "我的", Icons.Default.Person)
}

private val tabs = listOf(TabItem.Home, TabItem.Team, TabItem.Bookings, TabItem.Profile)

@Composable
fun MainTabScreen(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Card) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                            indicatorColor = Primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        TabNavHost(
            navController = navController,
            authViewModel = authViewModel,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun TabNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val userId = authViewModel.user.value?.id
    val user = authViewModel.user.value

    NavHost(
        navController = navController,
        startDestination = TabItem.Home.route,
        modifier = modifier
    ) {
        composable(TabItem.Home.route) {
            HomeScreen(
                onVenueClick = { venueId ->
                    navController.navigate("venue_detail/$venueId")
                }
            )
        }
        composable("venue_detail/{venueId}") { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId")?.toIntOrNull() ?: 0
            VenueDetailScreen(
                venueId = venueId,
                teamId = user?.teamId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(TabItem.Team.route) {
            TeamScreen(
                userId = userId,
                onUserChanged = { authViewModel.refreshUser() }
            )
        }
        composable(TabItem.Bookings.route) {
            BookingsScreen(userId = userId)
        }
        composable(TabItem.Profile.route) {
            ProfileScreen(
                authViewModel = authViewModel
            )
        }
    }
}
