package com.mathieu.blocker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mathieu.blocker.ui.components.BlockerBottomNavBar
import com.mathieu.blocker.ui.screens.AppListScreen
import com.mathieu.blocker.ui.screens.HomeScreen
import com.mathieu.blocker.ui.screens.SettingsScreen
import com.mathieu.blocker.ui.screens.StatsScreen
import com.mathieu.blocker.ui.screens.TipsScreen
import com.mathieu.blocker.ui.theme.BlockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("blocker_meta", Context.MODE_PRIVATE)
        val isFirstLaunch = !prefs.getBoolean("launched", false)
        if (isFirstLaunch) prefs.edit().putBoolean("launched", true).apply()

        setContent {
            BlockerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Hide bottom bar on applist (it's a detail screen)
                val showBottomBar = currentRoute in listOf("home", "stats", "tips", "settings")

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            BlockerBottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                isFirstLaunch = isFirstLaunch,
                                onNavigateToAppList = { navController.navigate("applist") },
                                onNavigateToGuide = {
                                    navController.navigate("tips") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable("applist") {
                            AppListScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("stats") {
                            StatsScreen()
                        }
                        composable("tips") {
                            TipsScreen()
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateToAppList = { navController.navigate("applist") }
                            )
                        }
                    }
                }
            }
        }
    }
}
