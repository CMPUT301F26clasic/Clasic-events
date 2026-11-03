/*
 * Purpose: Holds what the campsite form shows and saves it. Used to register a new campsite
 *          (US 02.01.01, US 02.01.02), view one (US 02.01.04) and update it (US 02.01.03).
 * Outstanding issues: Image upload and map location are not added yet.
 */
package com.example.clasicampsite

import android.app.Application
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.CampsiteForm
import kotlinx.coroutines.launch

/** Holds the campsite form's data. */
class CampsiteFormViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseController()

    /** This device's ID, used as the owner ID */
    val deviceId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    var form by mutableStateOf(CampsiteForm())
    /** True when registering a new campsite, false when viewing a saved one */
    var isNew by mutableStateOf(true)
    /** True while the fields can be changed */
    var editing by mutableStateOf(true)
    /** True if this device owns the campsite, so it may edit it */
    var isOwner by mutableStateOf(true)
    var loaded by mutableStateOf(false)
    var message by mutableStateOf("")

    private var original = Campsite()
    private var started = false

    /** Sets up the form once: empty if [campsiteId] is blank, otherwise loads that campsite. */
    fun start(campsiteId: String) {
        if (started) return
        started = true
        if (campsiteId.isBlank()) {
            original = Campsite(ownerId = deviceId)
            loaded = true
            return
        }
        isNew = false
        editing = false
        viewModelScope.launch {
            try {
                val campsite = database.getCampsite(campsiteId)
                if (campsite == null) {
                    message = "Campsite not found"
                } else {
                    original = campsite
                    form = CampsiteForm.from(campsite)
                    isOwner = campsite.ownerId == deviceId
                }
            } catch (e: Exception) {
                message = "Couldn't load the campsite"
            }
            loaded = true
        }
    }

    /** Lets the owner change the fields. */
    fun startEditing() {
        if (isOwner) {
            editing = true
            message = ""
        }
    }

    /** Throws away unsaved changes. */
    fun cancelEditing() {
        form = CampsiteForm.from(original)
        editing = false
        message = ""
    }

    /** Checks the form and saves it if valid, then runs [onSaved]. */
    fun save(onSaved: () -> Unit) {
        val error = form.errorMessage()
        if (error != null) {
            message = error
            return
        }
        val campsite = form.applyTo(original)
        viewModelScope.launch {
            try {
                database.saveCampsite(campsite)
                original = campsite
                form = CampsiteForm.from(campsite)
                message = if (isNew) "Campsite registered" else "Campsite updated"
                editing = false
                onSaved()
            } catch (e: Exception) {
                message = "Couldn't save the campsite"
            }
        }
    }
}