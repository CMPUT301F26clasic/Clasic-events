package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
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

    // Fetch details from Firestore when campsiteId changes
    LaunchedEffect(campsiteId) {
        if (campsiteId.isNotBlank()) {
            try {
                isLoading = true
                val fetched = dbController.getCampsite(campsiteId)
                if (fetched != null) {
                    campsite = fetched
                } else {
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
                                text = "★ ${site.rating} / 5.0",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("campsiteRating")
                            )

                            // Campsite details button
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    navController.navigate("manage_lottery/${site.id}")
                                },
                                modifier = Modifier.testTag("manageLotteryButton")
                            ) {
                                Text("Manage Lottery")
                            }
                        }
                    }
                }
            }
        }
    }
}
