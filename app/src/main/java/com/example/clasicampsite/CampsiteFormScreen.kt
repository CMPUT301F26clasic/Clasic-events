/*
 * Purpose: The screen an owner uses to add a new campsite (US 02.01.01, 02.01.02),
 * look at one they already made (US 02.01.04), and change it (US 02.01.03), including
 * an optional promo picture and map location.
 * Outstanding issues: None known.\
 * The photo picker code is based on ProfileScreen.kt.
 */
package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
/**
 * One screen for both adding and editing. If there's no [campsiteId] it's a blank
 * form for a new campsite, otherwise it loads that campsite and locks the fields until
 * the owner hits Edit.
 */
@Composable
fun CampsiteFormScreen(navController: NavController, campsiteId: String = "") {
    val viewModel: CampsiteFormViewModel = viewModel()
    LaunchedEffect(campsiteId) { viewModel.start(campsiteId) }
    val context = LocalContext.current

    // Opens Android's photo picker. When a photo is chosen, hand it to the ViewModel.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setImage(uri)
    }

    // Turns the saved text back into a picture (decodePicture is in ProfileScreen.kt).
    val picture = remember(viewModel.form.image) { decodePicture(viewModel.form.image) }

    val title = when {
        viewModel.isNew -> "Register Campsite"
        viewModel.editing -> "Edit Campsite"
        else -> "Campsite Details"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton"),
        ) {
            Text("Back")
        }
        Text(title, fontSize = 24.sp, modifier = Modifier.testTag("campsiteFormTitle"))

        if (!viewModel.loaded) {
            Text("Loading...")
            return@Column
        }

        val form = viewModel.form
        val readOnly = !viewModel.editing

        FormField("Name", form.name, readOnly, "campsiteNameField") {
            viewModel.form = form.copy(name = it)
        }
        FormField("Location", form.location, readOnly, "campsiteLocationField") {
            viewModel.form = form.copy(location = it)
        }
        FormField("Description and amenities", form.description, readOnly, "campsiteDescriptionField", singleLine = false) {
            viewModel.form = form.copy(description = it)
        }
        FormField("Max occupancy", form.maxOccupancy, readOnly, "campsiteOccupancyField", KeyboardType.Number) {
            viewModel.form = form.copy(maxOccupancy = it)
        }
        FormField("Price per night ($)", form.pricePerNight, readOnly, "campsitePriceField", KeyboardType.Decimal) {
            viewModel.form = form.copy(pricePerNight = it)
        }
        FormField("Tags (comma separated)", form.tags, readOnly, "campsiteTagsField") {
            viewModel.form = form.copy(tags = it)
        }

        // Promo picture (optional, US 02.01.02)
        if (picture != null) {
            Image(
                bitmap = picture,
                contentDescription = "Campsite picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp).testTag("campsiteImage"),
            )
        }
        if (viewModel.editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("pickCampsiteImageButton"),
                ) {
                    Text(if (picture == null) "Add picture" else "Change picture")
                }
                if (picture != null) {
                    TextButton(
                        onClick = { viewModel.removeImage() },
                        modifier = Modifier.testTag("removeCampsiteImageButton"),
                    ) {
                        Text("Remove")
                    }
                }
            }
        }

        // Map location (optional, US 02.01.02)
        FormField("Latitude (optional)", form.latitude, readOnly, "campsiteLatitudeField") {
            viewModel.form = form.copy(latitude = it)
        }
        FormField("Longitude (optional)", form.longitude, readOnly, "campsiteLongitudeField") {
            viewModel.form = form.copy(longitude = it)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (viewModel.editing) {
                OutlinedButton(
                    onClick = { viewModel.findLocation() },
                    modifier = Modifier.testTag("findLocationButton"),
                ) {
                    Text("Find from location")
                }
            }
            val spot = form.mapLocation()
            if (spot != null) {
                OutlinedButton(
                    onClick = { openMap(context, spot.first, spot.second) },
                    modifier = Modifier.testTag("viewOnMapButton"),
                ) {
                    Text("View on map")
                }
            }
        }

        if (viewModel.editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val wasNew = viewModel.isNew
                        viewModel.save { if (wasNew) navController.popBackStack() }
                    },
                    modifier = Modifier.weight(1f).testTag("saveCampsiteButton"),
                ) {
                    Text(if (viewModel.isNew) "Register campsite" else "Save changes")
                }
                if (!viewModel.isNew) {
                    OutlinedButton(
                        onClick = { viewModel.cancelEditing() },
                        modifier = Modifier.testTag("cancelEditButton"),
                    ) {
                        Text("Cancel")
                    }
                }
            }
        } else if (viewModel.isOwner) {
            Button(
                onClick = { viewModel.startEditing() },
                modifier = Modifier.fillMaxWidth().testTag("editCampsiteButton"),
            ) {
                Text("Edit")
            }
            // US 02.10.01: send a notification to everyone on this campsite's waitlist
            OutlinedButton(
                onClick = { navController.navigate("notify_waitlist/$campsiteId") },
                modifier = Modifier.fillMaxWidth().testTag("notifyWaitlistButton"),
            ) {
                Text("Notify waitlist")
            }
        }

        Text(viewModel.message, modifier = Modifier.testTag("campsiteFormMessage"))
    }
}

/** One labelled text field on the campsite form. */
@Composable
private fun FormField(
    label: String,
    value: String,
    readOnly: Boolean,
    tag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}
/** Opens Google Maps (or the web browser) at [latitude], [longitude]. */
private fun openMap(context: Context, latitude: Double, longitude: Double) {
    val url = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "No app can open maps", Toast.LENGTH_SHORT).show()
    }
}