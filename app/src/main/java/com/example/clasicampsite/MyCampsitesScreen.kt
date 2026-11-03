/*
 * Purpose: Lists the campsites the owner has registered (US 02.03.01), with a button to
 *          register a new one (US 02.01.01). Tapping a campsite opens it to view or edit.
 * Outstanding issues: None known.
 */
package com.example.clasicampsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.clasicampsite.model.Campsite

/** The My Campsites screen. */
@Composable
fun MyCampsitesScreen(navController: NavController) {
    val viewModel: MyCampsitesViewModel = viewModel()

    // Runs each time the screen is shown, so a newly saved campsite appears.
    LaunchedEffect(Unit) { viewModel.load() }

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
        Text("My Campsites", fontSize = 24.sp, modifier = Modifier.testTag("myCampsitesTitle"))
        Button(
            onClick = { navController.navigate("campsite_form") },
            modifier = Modifier.fillMaxWidth().testTag("registerCampsiteButton"),
        ) {
            Text("Register a campsite")
        }

        when {
            viewModel.loading && viewModel.campsites.isEmpty() -> Text("Loading...")
            viewModel.message.isNotEmpty() -> Text(viewModel.message, color = MaterialTheme.colorScheme.error)
            viewModel.campsites.isEmpty() -> Text(
                "You haven't registered any campsites yet.",
                modifier = Modifier.testTag("noCampsitesText"),
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.campsites, key = { it.id }) { campsite ->
                    OwnerCampsiteRow(campsite) {
                        navController.navigate("campsite_form/${campsite.id}")
                    }
                }
            }
        }
    }
}

/** One campsite in the owner's list. Tapping it runs [onClick]. */
@Composable
private fun OwnerCampsiteRow(campsite: Campsite, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("myCampsiteItem"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(campsite.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(campsite.location)
            Text(
                "Up to ${campsite.maxOccupancy} guests · $${campsite.pricePerNight}/night",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}