
package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun RegisterCampsiteScreen(navController: NavController) {

    val database = remember { DatabaseController() }
    val scope = rememberCoroutineScope()

    // Form fields
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var maxOccupancy by remember { mutableStateOf("") }
    var numberOfWinners by remember { mutableStateOf("") }

    var availableFrom by remember { mutableStateOf<LocalDate?>(null) }
    var availableTo by remember { mutableStateOf<LocalDate?>(null) }

    var message by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton")
        ) {
            Text("Back")
        }

        Text(
            text = "Register a Campground",
            fontSize = 24.sp
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Campground Name") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nameField")
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("locationField")
        )

        OutlinedTextField(
            value = maxOccupancy,
            onValueChange = { maxOccupancy = it.filter(Char::isDigit) },
            label = { Text("Maximum Occupancy") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("occupancyField")
        )

        OutlinedTextField(
            value = numberOfWinners,
            onValueChange = {
                numberOfWinners = it.filter(Char::isDigit)
            },
            label = { Text("Number of Lottery Winners") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("numberOfWinnersField")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Campground Availability", fontSize = 18.sp)

        RegistrationDateField(
            label = "Available From",
            date = availableFrom,
            onDateSelected = { availableFrom = it },
            tag = "availableFromField"
        )

        RegistrationDateField(
            label = "Available Until",
            date = availableTo,
            onDateSelected = { availableTo = it },
            tag = "availableToField"
        )

        Text(
            text = message,
            modifier = Modifier.testTag("registrationMessage")
        )

        Button(
            onClick = {
                val occupancy = maxOccupancy.toIntOrNull()
                val winners = numberOfWinners.toIntOrNull()

                when {
                    name.isBlank() ->
                        message = "Please enter a campground name."

                    location.isBlank() ->
                        message = "Please enter a location."

                    occupancy == null || occupancy <= 0 ->
                        message = "Please enter a valid maximum occupancy."

                    winners == null || winners <= 0 ->
                        message = "Please enter a valid number of lottery winners."

                    availableFrom == null || availableTo == null ->
                        message = "Please select both availability dates."

                    availableTo!!.isBefore(availableFrom) ->
                        message = "The end date cannot be before the start date."

                    else -> {
                        val campsite = Campsite(
                            name = name.trim(),
                            location = location.trim(),
                            maxOccupancy = occupancy,
                            rating = 0.0,
                            availableFrom = availableFrom!!,
                            availableTo = availableTo!!,
                            numberOfWinners = winners
                        )

                        scope.launch {
                            isSaving = true
                            message = "Registering campground..."

                            try {
                                database.saveCampsite(campsite)
                                message = "Campground successfully registered!"

                                // Clear form after successful registration
                                name = ""
                                location = ""
                                maxOccupancy = ""
                                availableFrom = null
                                availableTo = null
                                numberOfWinners = ""

                            } catch (e: Exception) {
                                message = "Registration failed: ${e.localizedMessage}"
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("registerButton")
        ) {
            Text(if (isSaving) "Registering..." else "Register Campground")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationDateField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    tag: String
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Text(date?.toString() ?: label)
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                            )
                        }
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
