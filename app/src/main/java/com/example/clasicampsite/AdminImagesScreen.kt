/**
 * Purpose: The admin screen that shows every uploaded image and who uploaded it (US 03.06.01),
 * and lets the admin remove one after confirming (US 03.03.01).
 * Only admins can use it. Everyone else sees "Admins only" (US 03.11.01).
 * Outstanding issues: None.
 */
package com.example.clasicampsite

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.clasicampsite.model.User

/** The admin's list of every uploaded image. */
@Composable
fun AdminImagesScreen(navController: NavController) {
    /** only admins can see this screen. null = still checking with Firestore. */
    val context = LocalContext.current
    var allowed by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        allowed = AdminAccess.isAdmin(context)
    }
    if (allowed != true) {
        AdminOnlyMessage(navController, checking = allowed == null)
        return
    }

    val viewModel: AdminImagesViewModel = viewModel()
    var imageToRemove by remember { mutableStateOf<User?>(null) }

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
        Text("All images", fontSize = 24.sp)
        Text(viewModel.message)

        if (!viewModel.loaded) {
            Text("Loading...")
        } else if (viewModel.users.isEmpty()) {
            Text("No images uploaded.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("imageList"),
            ) {
                items(viewModel.users) { user ->
                    val picture = remember(user.profileImage) { decodePicture(user.profileImage) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (picture != null) {
                                Image(
                                    bitmap = picture,
                                    contentDescription = "Uploaded image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(120.dp).testTag("image_${user.id}"),
                                )
                            }
                            Text("Uploaded by: ${if (user.name.isBlank()) "(no name)" else user.name}")
                            Text("Device ID: ${user.id}")
                            Button(
                                onClick = { imageToRemove = user },
                                modifier = Modifier.testTag("removeImageButton_${user.id}"),
                            ) {
                                Text("Remove image")
                            }
                        }
                    }
                }
            }
        }
    }

    val user = imageToRemove
    if (user != null) {
        AlertDialog(
            onDismissRequest = { imageToRemove = null },
            title = { Text("Remove image?") },
            text = { Text("This removes the picture. The user's profile stays.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeImage(user)
                        imageToRemove = null
                    },
                    modifier = Modifier.testTag("confirmRemoveImageButton"),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { imageToRemove = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}