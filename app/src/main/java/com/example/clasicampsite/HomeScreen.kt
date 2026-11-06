package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "clasiCampsite Home",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {navController.navigate("campsites")},
            modifier = Modifier.testTag("campsiteButton")) {
            Text("Campsites")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("reservations") },
            modifier = Modifier.testTag("reservationButton")
        ) {
            Text("Reservations")
        }

        Button(
            onClick = { navController.navigate("notifications") },
            modifier = Modifier.testTag("notificationButton")
        ) {
            Text("Notifications")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("profile") },
            modifier = Modifier.testTag("profileButton")
        ) {
            Text("Profile")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("admin_profiles") },
            modifier = Modifier.testTag("adminProfilesButton")
        ) {
            Text("Admin: All profiles")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("admin_campsites") },
            modifier = Modifier.testTag("adminCampsitesButton")
        ) {
            Text("Admin: All campsites")
        Button(
            onClick = { navController.navigate("register_campsite") },
            modifier = Modifier.testTag("registerCampsiteButton")
        ) {
            Text("Register a Campground")
        }

    }

}
