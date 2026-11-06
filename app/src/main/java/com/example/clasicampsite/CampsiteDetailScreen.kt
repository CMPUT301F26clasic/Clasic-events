package com.example.clasicampsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.provider.Settings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CampsiteDetailScreen(
    navController: NavController,
    campsiteId: String = "",
    dbController: DatabaseController = remember { DatabaseController() }
) {
    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    //keya adding variables for already in lottery list //
    var alreadyinLottery by remember { mutableStateOf(false) }
    var updateLottery by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    var userrating by remember { mutableStateOf<Int?>(null) }
    var showRating by remember { mutableStateOf<Double?>(null) }
    var rating by remember { mutableStateOf(false) }
    var ratingError by remember { mutableStateOf<String?>(null) }



    // Fetch details from Firestore when campsiteId changes
    LaunchedEffect(campsiteId) {
        if (campsiteId.isNotBlank()) {
            try {
                isLoading = true
                val fetched = dbController.getCampsite(campsiteId)
                if (fetched != null) {
                    campsite = fetched
                    showRating = fetched.rating
                    alreadyinLottery =
                        userId != null && dbController.isUserInLottery(campsiteId, userId)
                    userrating = if( userId != null) dbController.getrating(campsiteId, userId) else null
                }
                else {
                    errorMessage = "Campsite not found."
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load campsite: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        } else {
            // Sample fallback data if no ID provided for preview/testing
            campsite = Campsite(
                id = "sample_1",
                name = "Pine Ridge Campsite",
                location = "Banff National Park, AB",
                maxOccupancy = 6,
                rating = 4.8,
                availableFrom = LocalDate.of(2027, 5, 1),
                availableTo = LocalDate.of(2027, 9, 30)
            )

            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton")
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading campsite details...")
            }
        } else if (errorMessage != null) {
            Text(
                text = errorMessage ?: "An error occurred",
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp
            )
        } else campsite?.let { site ->
            Text(
                text = "Campsite Details",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("campsiteDetailsTitle")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            val onToggle: () -> Unit = {
                if (userId != null) {
                    scope.launch {
                        updateLottery = true
                        try {
                            if (alreadyinLottery) {
                                dbController.leaveLottery(site.id, userId)
                                alreadyinLottery = false
                            } else {
                                dbController.joinLottery(site.id, userId)
                                alreadyinLottery = true
                            }
                        } catch (e: Exception) {
                            errorMessage = "Lottery update failed: ${e.localizedMessage}"
                        } finally {
                            updateLottery = false
                        }
                    }
                }
            }

            if (alreadyinLottery) {
                OutlinedButton(
                    onClick = onToggle,
                    enabled = !updateLottery && userId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("leaveLotteryButton")
                ) {
                    Text(if (updateLottery) "Leaving..." else "Leave Lottery")
                }
            } else {
                Button(
                    onClick = onToggle,
                    enabled = !updateLottery && userId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("joinLotteryButton")
                ) {
                    Text(if (updateLottery) "Joining..." else "Join Lottery")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campsiteCard"),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Name
                    Column {
                        Text(
                            text = "Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = site.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("campsiteName")
                        )
                    }

                    // 2. Location
                    Column {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = site.location,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag("campsiteLocation")
                        )
                    }

                    // 3. Max Occupancy & 4. Rating Row
                    // 3. Max Occupancy & 4. Rating Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Max Occupancy",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${site.maxOccupancy} people",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.testTag("campsiteMaxOccupancy")
                            )
                        }

                        Column {
                            Text(
                                text = "Rating",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "★ ${showRating ?: site.rating} / 5.0",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("campsiteRating")
                            )
                        }
                    }
                }
            }

            // Star picker, below the card
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (userrating == null) "Rate this campsite" else "Your rating",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (star in 1..5) {
                    Text(
                        text = if (star <= (userrating ?: 0)) "★" else "☆",
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .testTag("star$star")
                            .clickable(enabled = !rating) {
                                scope.launch {
                                    rating = true
                                    ratingError = null
                                    try {
                                        showRating = dbController.rateCampsite(site.id, userId, star)
                                        userrating = star
                                    } catch (e: Exception) {
                                        ratingError = "Rating failed: ${e.localizedMessage}"
                                    } finally {
                                        rating = false
                                    }
                                }
                            }
                    )
                }
            }

            ratingError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }
    }
}

