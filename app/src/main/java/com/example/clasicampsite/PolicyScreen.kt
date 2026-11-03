/*
Purpose: Shows the app's policy and guidelines.
 */
package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/** The app policy and guidelines screen. */
@Composable
fun PolicyScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton"),
        ) {
            Text("Back")
        }
        Text(
            "App policy and guidelines",
            fontSize = 24.sp,
            modifier = Modifier.testTag("policyTitle"),
        )
        Text("Fair lotteries: winners are drawn at random after registration closes, so it doesn't matter when you join the waiting list.")
        Text("One entry per person: you are identified by your device. Join each lottery once for yourself and your group.")
        Text("Reserving: if you win, read and accept the campsite's rules before you confirm. If you change your mind, decline so someone else can have the spot.")
        Text("Your information: we store your name, email, phone and notification setting. You can edit or delete your profile at any time.")
        Text("Removal: admins may remove campsites, images or profiles that break these guidelines.")
    }
}