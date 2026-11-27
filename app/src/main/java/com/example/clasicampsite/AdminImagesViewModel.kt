/**
 * Purpose: Holds the admin's list of uploaded images and removes ones that break the app policy
 * (US 03.06.01, US 03.03.01). Right now the only uploaded images are profile pictures.
 * Outstanding issues: Campsites can't have pictures yet, so none are listed here.
 */
package com.example.clasicampsite

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.User
import kotlinx.coroutines.launch

/** Holds admin images screen's data. */
class AdminImagesViewModel : ViewModel() {

    private val database = DatabaseController()

    /** Users who have uploaded a profile picture */
    var users by mutableStateOf(listOf<User>())
    var message by mutableStateOf("")
    var loaded by mutableStateOf(false)

    init {
        viewModelScope.launch {
            try {
                users = database.getAllUsers().filter { it.profileImage.isNotEmpty() }
            } catch (e: Exception) {
                message = "Couldn't load images"
            }
            loaded = true
        }
    }

    /** Removes [user]'s picture and takes it off the list. Their profile stays. */
    fun removeImage(user: User) {
        viewModelScope.launch {
            try {
                database.removeProfileImage(user.id)
                users = users.filter { it.id != user.id }
                message = "Image removed"
            } catch (e: Exception) {
                message = "Couldn't remove image"
            }
        }
    }
}