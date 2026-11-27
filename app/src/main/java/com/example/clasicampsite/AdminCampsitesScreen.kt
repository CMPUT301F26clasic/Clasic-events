/*
Purpose: The admin screen that lists every campsite. Tapping one opens its details,
and admins can remove campsites that violate policy after confirming.
Only admins can use it; everyone else sees "Admins only" (US 03.11.01).
Outstanding issues: None.
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.clasicampsite.model.Campsite

/** The admin's list of every campsite. */
@Composable
fun AdminCampsitesScreen(navController: NavController) {
    // only admins can see this screen. null = still checking with Firestore.
    val context = LocalContext.current
    var allowed by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        allowed = AdminAccess.isAdmin(context)
    }
    if (allowed != true) {
        AdminOnlyMessage(navController, checking = allowed == null)
        return
    }

    val viewModel: AdminCampsitesViewModel = viewModel()
    var campsiteToRemove by remember { mutableStateOf<Campsite?>(null) }

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
                            Button(
                                onClick = { campsiteToRemove = campsite },
                                modifier = Modifier.testTag("removeCampsiteButton_${campsite.id}"),
                            ) {
                                Text("Remove campsite")
                            }
                        }
                    }
                }
            }
        }
    }

    val target = campsiteToRemove
    if (target != null) {
        AlertDialog(
            onDismissRequest = { campsiteToRemove = null },
            title = { Text("Remove campsite?") },
            text = { Text("This deletes \"${target.name.ifBlank { "(no name)" }}\". It can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeCampsite(target)
                        campsiteToRemove = null
                    },
                    modifier = Modifier.testTag("confirmRemoveCampsiteButton"),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { campsiteToRemove = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}