/*
 * Purpose: The screen an owner uses to add a new campsite (US 02.01.01, 02.01.02),
 * look at one they already made (US 02.01.04), and change it (US 02.01.03).
 * Outstanding issues: No image upload or map location yet.
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
/**
 * One screen for both adding and editing. If there's no [campsiteId] it's a blank
 * form for a new campsite, otherwise it loads that campsite and locks the fields until
 * the owner hits Edit.
 */
@Composable
fun CampsiteFormScreen(navController: NavController, campsiteId: String = "") {
    val viewModel: CampsiteFormViewModel = viewModel()
    LaunchedEffect(campsiteId) { viewModel.start(campsiteId) }

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