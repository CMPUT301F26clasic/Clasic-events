/*
Purpose: Shows the confirmation of a reservation (US 01.05.01) and lets the entrant download it
as a CSV file (US 01.05.02).
Outstanding issues: None.
*/
package com.example.clasicampsite

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Reservation

@Composable
fun ReservationConfirmationScreen(
    navController: NavController,
    reservationId: String,
    dbController: DatabaseController = remember { DatabaseController() }
) {
    val context = LocalContext.current

    var reservation by remember { mutableStateOf<Reservation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // US 01.05.02: lets the user pick where to save the CSV file
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val currentReservation = reservation
        if (uri != null && currentReservation != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(currentReservation.toCsv().toByteArray())
                }
                Toast.makeText(context, "Reservation saved as CSV", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not save the CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(reservationId) {
        try {
            reservation = dbController.getReservation(reservationId)
            if (reservation == null) {
                errorMessage = "Reservation not found."
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load reservation: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        val currentReservation = reservation
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        } else if (currentReservation != null) {
            Text(
                "Reservation Confirmed",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("confirmedTitle")
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(currentReservation.campsiteName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Dates: ${currentReservation.dates.joinToString(", ")}")
                    Text("Nights: ${currentReservation.nights()}")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("A confirmation was also sent to your notifications.")
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { csvLauncher.launch("reservation-${currentReservation.id}.csv") },
                modifier = Modifier.fillMaxWidth().testTag("downloadCsvButton")
            ) {
                Text("Download as CSV")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { navController.popBackStack("home", inclusive = false) },
            modifier = Modifier.fillMaxWidth().testTag("doneButton")
        ) {
            Text("Done")
        }
    }
}
