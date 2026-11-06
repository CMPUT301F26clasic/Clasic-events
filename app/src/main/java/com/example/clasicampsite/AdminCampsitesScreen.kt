/*
Purpose: The admin screen that lists every campsite. Tapping one opens its details.
Outstanding issues: Anyone can open this screen from Home for now. It should only show for
admins once roles are added.
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/** The admin's list of every campsite. */
@Composable
fun AdminCampsitesScreen(navController: NavController) {
    val viewModel: AdminCampsitesViewModel = viewModel()

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
        Text("All campsites", fontSize = 24.sp)
        Text(viewModel.message)

        if (!viewModel.loaded) {
            Text("Loading...")
        } else if (viewModel.campsites.isEmpty()) {
            Text("No campsites yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("adminCampsiteList"),
            ) {
                items(viewModel.campsites, key = { it.id }) { campsite ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("campsite_detail/${campsite.id}") },
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                if (campsite.name.isBlank()) "(no name)" else campsite.name,
                                fontSize = 18.sp,
                            )
                            Text("Location: ${campsite.location}")
                            Text("Max occupancy: ${campsite.maxOccupancy}")
                            Text("Rating: ${campsite.rating}")
                            Text("Available: ${campsite.availableFrom} to ${campsite.availableTo}")
                        }
                    }
                }
            }
        }
    }
}
