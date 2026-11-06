/*
Purpose: The user's profile screen: shows their device ID, lets them edit their
name, email, phone, turn notifications off, open the app policy, and delete their profile.
 */
package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/** Turns picture saved as Base64 text back into an image, or null if there is none. */
fun decodePicture(text: String): ImageBitmap? {
    if (text.isEmpty()) return null
    val bytes = Base64.decode(text, Base64.NO_WRAP)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}

/** The Profile screen. */
@Composable
fun ProfileScreen(navController: NavController) {
    val viewModel: ProfileViewModel = viewModel()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Opens Android's photo picker. When photo is chosen, hand it to the ViewModel.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.setImage(uri)
        }
    }

    // Turns the saved text back into a picture. "remember" only redoes this when it changes.
    val picture = remember(viewModel.profileImage) { decodePicture(viewModel.profileImage) }

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
        Text("Profile", fontSize = 24.sp)
        Text("Device ID: ${viewModel.deviceId}")

        if (!viewModel.loaded) {
            Text("Loading...")
        } else {
            if (picture != null) {
                Image(
                    bitmap = picture,
                    contentDescription = "Profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp).clip(CircleShape).testTag("profilePicture"),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("pickImageButton"),
                ) {
                    Text(if (picture == null) "Add picture" else "Change picture")
                }
                if (picture != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { viewModel.profileImage = "" },
                        modifier = Modifier.testTag("removeImageButton"),
                    ) {
                        Text("Remove")
                    }
                }
            }
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().testTag("nameField"),
            )
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().testTag("emailField"),
            )
            OutlinedTextField(
                value = viewModel.phone,
                onValueChange = { viewModel.phone = it },
                label = { Text("Phone (optional)") },
                modifier = Modifier.fillMaxWidth().testTag("phoneField"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.fillMaxWidth().testTag("saveButton"),
                ) {
                    Text("Update info")
                }
            }
            Text(viewModel.message)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Switch(
                    checked = viewModel.notificationsEnabled,
                    onCheckedChange = { viewModel.notificationsEnabled = it },
                    modifier = Modifier.testTag("notificationsSwitch"),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notifications", modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { navController.navigate("policy") },
                    modifier = Modifier.testTag("policyButton"),
                ) {
                    Text("View App Policy and Guidelines")
                }
            }
            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.testTag("deleteButton"),
            ) {
                Text("Delete profile")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete profile?") },
            text = { Text("This removes your saved details. It can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete { navController.popBackStack() }
                    },
                    modifier = Modifier.testTag("confirmDeleteButton"),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}