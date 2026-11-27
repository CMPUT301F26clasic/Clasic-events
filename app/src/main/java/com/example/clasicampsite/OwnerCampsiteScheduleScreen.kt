/*
 * Purpose: Lets a campsite owner view currently booked dates (#25) and filter
 * past, active, and cancelled reservations (#26) for one of their campsites.
 * Outstanding issues: None right now.
 */
package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.example.clasicampsite.model.Reservation
import java.time.LocalDate

private enum class ReservationFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    PAST("Past"),
    CANCELLED("Cancelled"),
}

/** Shows booked dates and reservation history for [campsiteId]. */
@Composable
fun OwnerCampsiteScheduleScreen(
    navController: NavController,
    campsiteId: String,
    dbController: DatabaseController = remember { DatabaseController() },
) {
    var campsiteName by remember { mutableStateOf("") }
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var bookedDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf(ReservationFilter.ALL) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val todayStr = remember { LocalDate.now().toString() }

    LaunchedEffect(campsiteId) {
        try {
            isLoading = true
            errorMessage = null

            val campsite = dbController.getCampsite(campsiteId)
            campsiteName = campsite?.name ?: "Campsite"

            val loaded = dbController.getReservationsForCampsite(campsiteId)
            reservations = loaded
            bookedDates = Reservation.bookedDates(loaded)
                .filter { it >= todayStr }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            errorMessage = "Failed to load schedule: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    val filteredReservations = remember(reservations, selectedFilter, todayStr) {
        reservations.filter { res ->
            when (selectedFilter) {
                ReservationFilter.ALL -> true
                ReservationFilter.CANCELLED -> res.status.equals("cancelled", ignoreCase = true)
                ReservationFilter.PAST -> !res.status.equals("cancelled", ignoreCase = true) &&
                        res.dates.maxOrNull()?.let { it < todayStr } == true
                ReservationFilter.ACTIVE -> !res.status.equals("cancelled", ignoreCase = true) &&
                        res.dates.maxOrNull()?.let { it >= todayStr } != false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton"),
        ) {
            Text("Back")
        }

        Text(
            text = "Bookings & Reservations",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("ownerScheduleTitle"),
        )
        if (campsiteName.isNotBlank()) {
            Text(campsiteName, style = MaterialTheme.typography.titleMedium)
        }

        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text("Loading bookings...")
                }
            }

            errorMessage != null -> {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            }

            else -> {
                // #25: Currently booked dates (active reservations from today onward)
                Text(
                    text = "Currently Booked Dates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (bookedDates.isEmpty()) {
                    Text(
                        text = "No upcoming booked dates.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("noBookedDatesText"),
                    )
                } else {
                    Text(
                        text = bookedDates.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("bookedDatesList"),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // #26: Filter chips for past, active, and cancelled reservations
                Text(
                    text = "Reservations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReservationFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
                        )
                    }
                }

                if (filteredReservations.isEmpty()) {
                    Text(
                        text = "No ${selectedFilter.label.lowercase()} reservations.",
                        modifier = Modifier.testTag("emptyReservationsText"),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).testTag("ownerReservationsList"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredReservations, key = { it.id.ifBlank { it.hashCode().toString() } }) { res ->
                            val isPast = !res.status.equals("cancelled", ignoreCase = true) &&
                                    res.dates.maxOrNull()?.let { it < todayStr } == true
                            val displayStatus = when {
                                res.status.equals("cancelled", ignoreCase = true) -> "Cancelled"
                                isPast -> "Past"
                                else -> "Active"
                            }

                            Card(modifier = Modifier.fillMaxWidth().testTag("ownerReservationItem")) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Status: $displayStatus", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (res.dates.isEmpty()) "No dates" else res.dates.sorted().joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = "Guest ID: ${res.userId} · ${res.nights()} night(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}