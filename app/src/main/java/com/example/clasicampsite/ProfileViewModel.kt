/*
Purpose: Holds what Profile screen shows and talks to database. Loads user for
this device, saves their details and notification setting and deletes their profile
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
import com.example.clasicampsite.model.User
import kotlinx.coroutines.launch

/** Holds Profile screen's data. */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseController()

    /** This device's ID */
    val deviceId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var notificationsEnabled by mutableStateOf(true)
    var message by mutableStateOf("")
    var loaded by mutableStateOf(false)

    init {
        viewModelScope.launch {
            try {
                val user = database.getUser(deviceId)
                name = user.name
                email = user.email
                phone = user.phone
                notificationsEnabled = user.notificationsEnabled
            } catch (e: Exception) {
                message = "Couldn't load your profile"
            }
            loaded = true
        }
    }

    /** Checks details and saves them if valid */
    fun save() {
        val user = User(
            id = deviceId,
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            notificationsEnabled = notificationsEnabled,
        )
        val error = user.errorMessage()
        if (error != null) {
            message = error
            return
        }
        viewModelScope.launch {
            database.saveUser(user)
            message = "Profile saved"
        }
    }

    /** Deletes device's profile, then runs [onDeleted] */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            database.deleteUser(deviceId)
            onDeleted()
        }
    }
}