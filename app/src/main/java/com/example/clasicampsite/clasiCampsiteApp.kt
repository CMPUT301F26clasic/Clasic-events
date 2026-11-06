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
        composable("campsite_detail/{campsiteId}") { backStackEntry ->
            val campsiteId = backStackEntry.arguments?.getString("campsiteId") ?: ""
            CampsiteDetailScreen(navController, campsiteId = campsiteId)
        }
        composable("available_dates/{lotteryId}") { backStackEntry ->
            val lotteryId = backStackEntry.arguments?.getString("lotteryId") ?: ""
            AvailableDatesScreen(navController, lotteryId = lotteryId)
        }
        composable("reservation_confirmation/{reservationId}") { backStackEntry ->
            val reservationId = backStackEntry.arguments?.getString("reservationId") ?: ""
            ReservationConfirmationScreen(navController, reservationId = reservationId)
        }
    }
}