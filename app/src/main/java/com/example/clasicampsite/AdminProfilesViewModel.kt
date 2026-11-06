/*
Purpose: Holds admin's list of every profile and removes profiles that break the app policy.
Outstanding issues: Removing a profile does not yet remove that user's waiting list entries or
reservations.
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

/** Holds admin profiles screen's data. */
class AdminProfilesViewModel : ViewModel() {

    private val database = DatabaseController()

    var users by mutableStateOf(listOf<User>())
    var message by mutableStateOf("")
    var loaded by mutableStateOf(false)

    init {
        viewModelScope.launch {
            try {
                users = database.getAllUsers()
            } catch (e: Exception) {
                message = "Couldn't load profiles"
            }
            loaded = true
        }
    }

    /** Deletes [user]'s profile and takes it off the list */
    fun remove(user: User) {
        viewModelScope.launch {
            try {
                database.deleteUser(user.id)
                users = users.filter { it.id != user.id }
                message = "Profile removed"
            } catch (e: Exception) {
                message = "Couldn't remove profile"
            }
        }
    }
}