/**
 * Purpose: Shows the list of campsites, with a search bar (US 01.01.01) and filters for location,
 * dates and number of guests (US 01.01.02). Tapping a campsite opens its details.
 * Outstanding issues: Loads every campsite at once, which is fine for a class project but slow
 * with many campsites.
 */
package com.example.clasicampsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.CampsiteFilter
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.Instant

/** Sample campsites the list tests use, so they don't depend on what is saved in Firestore. */
internal val sampleCampsites = listOf(
    Campsite("hello", "Pine Ridge", "Jasper, AB", 6, 3.0, LocalDate.of(2027, 5, 1), LocalDate.of(2027, 9, 30)),
    Campsite("bye", "Lakeside Haven", "Banff, AB", 4, 2.5, LocalDate.of(2027, 6, 1), LocalDate.of(2027, 8, 31)),
    Campsite("what", "Cedar Hollow", "Canmore, AB", 10, 4.4, LocalDate.of(2027, 5, 15), LocalDate.of(2027, 10, 15)),
    Campsite("sorry", "Sunset Meadows", "Kananaskis, AB", 8, 5.0, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 9, 15)),
    Campsite("craycray", "River Bend", "Hinton, AB", 2, 3.8, LocalDate.of(2027, 5, 1), LocalDate.of(2027, 7, 31))
)

/**
 * Checks if this campsite matches the search bar text (US 01.01.01).
 * A number matches campsites that hold at least that many guests.
 *
 * @param query the text typed into the search bar
 * @return true if the name or location contains the text, or the campsite fits that many guests
 */
internal fun Campsite.matchesSearch(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true

    val matchesText = name.contains(q, ignoreCase = true) ||
            location.contains(q, ignoreCase = true)

    val number = q.toIntOrNull()
    val matchesOccupancy = number != null && maxOccupancy >= number

    return matchesText || matchesOccupancy
}

/**
 * Checks if this campsite passes every filter that is set (US 01.01.02).
 *
 * @param filter the location, guests and dates picked in the filter dialog
 * @return true if the campsite matches all of them
 */
internal fun Campsite.matchesFilter(filter: CampsiteFilter): Boolean {
    if (filter.location.isNotBlank() &&
        !location.contains(filter.location.trim(), ignoreCase = true)
    ) return false

    if (filter.guests != null && maxOccupancy < filter.guests) return false

    /**
     * The campsite must be available for the whole requested stay.
     * If only one date is picked, that single date must be available.
     */
    val needFrom = filter.startDate ?: filter.endDate
    val needTo = filter.endDate ?: filter.startDate
    if (needFrom != null && needTo != null) {
        if (availableFrom.isAfter(needFrom) || availableTo.isBefore(needTo)) return false
    }

    return true
}

/**
 * Shows the campsites saved in Firestore with a search bar (US 01.01.01) and a filter button
 * (US 01.01.02). Tapping a campsite opens its details (US 01.01.04).
 *
 * @param navController used to open a campsite's details
 * @param dbController where the campsites are loaded from
 * @param campsites campsites to show instead of loading them, used by the tests
 */
@Composable
fun CampsiteListScreen(
    navController: NavController,
    dbController: DatabaseController = remember { DatabaseController() },
    campsites: List<Campsite>? = null,
) {
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CampsiteFilter()) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var allCampsites by remember { mutableStateOf(campsites ?: emptyList()) }
    var isLoading by remember { mutableStateOf(campsites == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(campsites) {
        if (campsites == null) {
            try {
                allCampsites = dbController.getCampsites().sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                errorMessage = "Failed to load campsites: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    val filteredCampsites = remember(allCampsites, searchQuery, filter) {
        allCampsites.filter { it.matchesSearch(searchQuery) && it.matchesFilter(filter) }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)
    ) {
        Button(
            onClick = { navController.popBackStack() },
            /** back button */
            modifier = Modifier.testTag("backButton")
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search name, location, or occupancy") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("searchField")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { showFilterDialog = true },
                modifier = Modifier.testTag("filterButton")
            ) {
                Text(if (filter.activeCount > 0) "Filter (${filter.activeCount})" else "Filter")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Text("Loading campsites...")
        } else if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("errorText")
            )
        } else if (filteredCampsites.isEmpty()) {
            Text(
                text = "No campsites found",
                fontSize = 16.sp,
                modifier = Modifier.testTag("noResultsText")
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("campsiteList")
            ) {
                items(filteredCampsites, key = { it.id }) { campsite ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { navController.navigate("campsite_detail/${campsite.id}") }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = campsite.name, fontSize = 18.sp)
                            Text(text = campsite.location, fontSize = 14.sp)
                            Text(
                                text = "Max occupancy: ${campsite.maxOccupancy}",
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Available: ${campsite.availableFrom} to ${campsite.availableTo}",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            current = filter,
            onApply = {
                filter = it
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

/**
 * Popup for filtering campsites by location, dates and number of guests (US 01.01.02).
 *
 * @param current the filter that is set right now
 * @param onApply called with the new filter when Apply is tapped
 * @param onDismiss called when the popup is closed without applying
 */
@Composable
private fun FilterDialog(
    current: CampsiteFilter,
    onApply: (CampsiteFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var location by remember { mutableStateOf(current.location) }
    var guests by remember { mutableStateOf(current.guests?.toString() ?: "") }
    var startDate by remember { mutableStateOf(current.startDate) }
    var endDate by remember { mutableStateOf(current.endDate) }

    val start = startDate
    val end = endDate
    val dateError = start != null && end != null && end.isBefore(start)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter campsites") },
        text = {
            Column {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("filterLocation")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = guests,
                    onValueChange = { guests = it.filter(Char::isDigit) },
                    label = { Text("Number of guests") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("filterGuests")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    DateField(
                        label = "Start date",
                        date = startDate,
                        onDateSelected = { startDate = it },
                        modifier = Modifier.weight(1f),
                        tag = "filterStartDate",
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    DateField(
                        label = "End date",
                        date = endDate,
                        onDateSelected = { endDate = it },
                        modifier = Modifier.weight(1f),
                        tag = "filterEndDate",
                    )
                }

                if (dateError) {
                    Text(
                        text = "End date must be after start date",
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !dateError,
                onClick = {
                    onApply(
                        CampsiteFilter(
                            location = location,
                            guests = guests.toIntOrNull(),
                            startDate = startDate,
                            endDate = endDate
                        )
                    )
                },
                modifier = Modifier.testTag("applyFilterButton")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { onApply(CampsiteFilter()) },
                    modifier = Modifier.testTag("resetFilterButton")
                ) {
                    Text("Reset")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Button that opens a date picker for one filter date.
 *
 * @param label text shown on the button when no date is picked
 * @param date the picked date, or null
 * @param onDateSelected called with the new date, or null when it is cleared
 * @param modifier layout modifier for the button
 * @param tag test tag for the button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = modifier.testTag(tag)
    ) {
        Text(date?.toString() ?: label)
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showPicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}