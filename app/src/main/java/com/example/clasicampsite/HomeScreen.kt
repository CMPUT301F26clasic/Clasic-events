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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    // check whether this device is an admin each time Home is shown (US 03.11.01)
    val context = LocalContext.current
    var isAdmin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isAdmin = AdminAccess.isAdmin(context)
    }

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
            onClick = { navController.navigate("my_campsites") },
            modifier = Modifier.testTag("myCampsitesButton")
        ) {
            Text("My Campsites")
        }

        // admin buttons only show for admins
        if (isAdmin) {
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
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("register_campsite") },
            modifier = Modifier.testTag("registerCampsiteButton")
        ) {
            Text("Register a Campground")
        }

    }
}