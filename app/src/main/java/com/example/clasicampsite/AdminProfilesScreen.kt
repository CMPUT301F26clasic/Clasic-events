/*
Purpose: The admin screen that lists every profile and lets the admin remove one after confirming
Outstanding issues: Anyone can open this screen from Home for now. It should only show for
admins once roles are added
 */
package com.example.clasicampsite

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.clasicampsite.model.User
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

/** The admin's list of every profile. */
@Composable
fun AdminProfilesScreen(navController: NavController) {
    val viewModel: AdminProfilesViewModel = viewModel()
    var userToRemove by remember { mutableStateOf<User?>(null) }

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
        Text("All profiles", fontSize = 24.sp)
        Text(viewModel.message)

        if (!viewModel.loaded) {
            Text("Loading...")
        } else if (viewModel.users.isEmpty()) {
            Text("No profiles yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("profileList"),
            ) {
                items(viewModel.users) { user ->
                    val picture = remember(user.profileImage) { decodePicture(user.profileImage) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (picture != null) {
                                Image(
                                    bitmap = picture,
                                    contentDescription = "Profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(CircleShape),
                                )
                            }
                            Text(if (user.name.isBlank()) "(no name)" else user.name, fontSize = 18.sp)
                            Text("Email: ${user.email}")
                            Text("Phone: ${user.phone}")
                            Text("Device ID: ${user.id}")
                            Button(
                                onClick = { userToRemove = user },
                                modifier = Modifier.testTag("removeButton_${user.id}"),
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }

    val user = userToRemove
    if (user != null) {
        AlertDialog(
            onDismissRequest = { userToRemove = null },
            title = { Text("Remove profile?") },
            text = { Text("This deletes this user's profile. It can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.remove(user)
                        userToRemove = null
                    },
                    modifier = Modifier.testTag("confirmRemoveButton"),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToRemove = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}