/*
 * Purpose: Gets the list of campsites this phone's owner has made, for the
 * My Campsites screen (US 02.03.01).
 * Outstanding issues: Owners are only identified by device ID, no owner/entrant roles yet.
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
import kotlinx.coroutines.launch

/** Loads the owner's campsites for the My Campsites screen. */
class MyCampsitesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseController()

    /** The phone's ID, we use it as the owner ID */
    val deviceId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    var campsites by mutableStateOf<List<Campsite>>(emptyList())
    var loading by mutableStateOf(true)
    var message by mutableStateOf("")

    /** Pulls the owner's campsites from Firestore again and sorts them A-Z by name. */
    fun load() {
        viewModelScope.launch {
            loading = true
            try {
                campsites = database.getCampsitesByOwner(deviceId).sortedBy { it.name.lowercase() }
                message = ""
            } catch (e: Exception) {
                message = "Couldn't load your campsites"
            }
            loading = false
        }
    }
}