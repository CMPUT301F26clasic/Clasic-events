/**
 * Purpose: Keeps track of what's typed on the campsite form and handles loading and
 * saving it to Firestore, plus the promo picture and map location (US 02.01.01 - 02.01.04).
 * Outstanding issues: Finding a map location needs internet and a phone or emulator
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

/** Holds everything the campsite form needs so it survives screen rotations. */
class CampsiteFormViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseController()

    /** The phone's ID, we use it as the owner ID */
    val deviceId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    var form by mutableStateOf(CampsiteForm())
    /** true = making a new campsite, false = looking at a saved one */
    var isNew by mutableStateOf(true)
    /** true when the boxes can be typed in */
    var editing by mutableStateOf(true)
    /** only the phone that made the campsite gets to edit it */
    var isOwner by mutableStateOf(true)
    var loaded by mutableStateOf(false)
    var message by mutableStateOf("")

    private var original = Campsite()
    private var started = false

    /**
     * Runs once when the screen opens. Blank [campsiteId] means a new campsite,
     * otherwise go grab that campsite from Firestore.
     */
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

    /** Unlocks the fields (only if you own it). */
    fun startEditing() {
        if (isOwner) {
            editing = true
            message = ""
        }
    }

    /** Puts back whatever was saved before and locks the fields again. */
    fun cancelEditing() {
        form = CampsiteForm.from(original)
        editing = false
        message = ""
    }
    /**
     * Shrinks the picture at [uri] to 300 pixels wide and puts it on the form as text,
     * so it gets saved with the campsite when the owner presses save.
     */
    fun setImage(uri: Uri) {
        try {
            val input = getApplication<Application>().contentResolver.openInputStream(uri) ?: return
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return
            val height = original.height * 300 / original.width
            val small = Bitmap.createScaledBitmap(original, 300, height, true)
            val bytes = ByteArrayOutputStream()
            small.compress(Bitmap.CompressFormat.JPEG, 70, bytes)
            form = form.copy(image = Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP))
        } catch (e: Exception) {
            message = "Couldn't load that picture"
        }
    }

    /** Takes the picture off the form. */
    fun removeImage() {
        form = form.copy(image = "")
    }

    /**
     * Looks up the typed location (like "Hinton, AB") and fills in the latitude and
     * longitude boxes. Shows a message if the place can't be found.
     */
    fun findLocation() {
        val place = form.location.trim()
        if (place.isEmpty()) {
            message = "Type a location first"
            return
        }
        if (!Geocoder.isPresent()) {
            message = "This phone can't look up locations"
            return
        }
        viewModelScope.launch {
            try {
                /** Looking up a place uses the internet, so do it off the main thread. */
                val results = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    Geocoder(getApplication(), Locale.getDefault()).getFromLocationName(place, 1)
                }
                val spot = results?.firstOrNull()
                if (spot == null) {
                    message = "Couldn't find \"$place\" on the map"
                } else {
                    form = form.copy(
                        latitude = String.format(Locale.US, "%.5f", spot.latitude),
                        longitude = String.format(Locale.US, "%.5f", spot.longitude),
                    )
                    message = "Map location found"
                }
            } catch (e: Exception) {
                message = "Couldn't look up the location"
            }
        }
    }
    /**
     * Checks the form first, and if it's ok saves it to Firestore and then calls [onSaved].
     * If something's wrong it just shows the error message instead.
     */
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