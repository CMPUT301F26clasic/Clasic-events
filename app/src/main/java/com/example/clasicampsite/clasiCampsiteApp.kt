package com.example.clasicampsite

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun clasiCampsiteApp() {
    val navController = rememberNavController()
    NavHost(navController=navController, startDestination = "home") {
        composable (route = "home") { HomeScreen(navController) }
        composable (route = "campsites") { CampsiteListScreen(navController) }
        composable("reservations") { CampSiteReservationScreen(navController) }
        composable("notifications") { NotificationScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("policy") { PolicyScreen(navController) }
        composable("campsite_detail") { CampsiteDetailScreen(navController) }
        composable("admin_profiles") { AdminProfilesScreen(navController) }
        composable("admin_campsites") {AdminCampsitesScreen(navController)}
        composable("campsite_detail/{campsiteId}") { backStackEntry ->
            val campsiteId = backStackEntry.arguments?.getString("campsiteId") ?: ""
            CampsiteDetailScreen(navController, campsiteId = campsiteId)
        }
    }
}
