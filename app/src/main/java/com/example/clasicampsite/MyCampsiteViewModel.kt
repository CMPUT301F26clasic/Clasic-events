/*
 * Purpose: Holds what the My Campsites screen shows (US 02.03.01): the campsites this
 *          device's owner has registered.
 * Outstanding issues: Owners are identified by device ID only; roles are not checked yet.
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

/** Loads the current owner's campsites. */
class MyCampsitesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseController()

    /** This device's ID, used as the owner ID */
    val deviceId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    var campsites by mutableStateOf<List<Campsite>>(emptyList())
    var loading by mutableStateOf(true)
    var message by mutableStateOf("")

    /** Reloads the owner's campsites, sorted by name. */
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