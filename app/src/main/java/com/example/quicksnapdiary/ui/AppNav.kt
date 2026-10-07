package com.example.quicksnapdiary.ui

import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
@UnstableApi
fun AppNav(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "camera") {
        composable("camera") { CameraScreen(navController) }
        composable("timeline") { TimelineScreen(navController) }
        composable("history") { HistoryScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}
