package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
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
import com.example.clasicampsite.model.Lottery
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AvailableDatesScreen(
    navController: NavController,
    lotteryId: String,
    dbController: DatabaseController = remember { DatabaseController() }
) {
    val context = LocalContext.current
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    var lottery by remember { mutableStateOf<Lottery?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lotteryId) {
        try {
            lottery = dbController.getLottery(lotteryId)
            if (lottery == null) {
                errorMessage = "Lottery not found."
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load lottery: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton")
        ) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Available Dates", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        val currentLottery = lottery
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        } else if (currentLottery != null) {
            Text(currentLottery.title, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (!currentLottery.isWinner(deviceId)) {
                Text("You did not win this lottery.", modifier = Modifier.testTag("notWinnerText"))
            } else {
                val dates = currentLottery.freeDates(today)
                if (dates.isEmpty()) {
                    Text("No dates are left.", modifier = Modifier.testTag("noDatesText"))
                } else {
                    LazyColumn(modifier = Modifier.testTag("datesList")) {
                        items(dates) { date ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(date, fontSize = 16.sp, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}