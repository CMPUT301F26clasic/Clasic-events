/*
Purpose: Holds the admin's list of every campsite.
Outstanding issues: Anyone can open this screen for now. It should only show for admins once
roles are added.
 */
package com.example.clasicampsite

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import kotlinx.coroutines.launch

/** Holds admin campsites screen's data. */
class AdminCampsitesViewModel : ViewModel() {

    private val database = DatabaseController()

    var campsites by mutableStateOf(listOf<Campsite>())
    var message by mutableStateOf("")
    var loaded by mutableStateOf(false)

    init {
        viewModelScope.launch {
            try {
                campsites = database.getAllCampsites()
            } catch (e: Exception) {
                message = "Couldn't load campsites"
            }
            loaded = true
        }
    }
}
