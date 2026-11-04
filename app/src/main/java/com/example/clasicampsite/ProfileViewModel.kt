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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/** Holds Profile screen's data. */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseController()

    /** This device's ID */
    val deviceId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var notificationsEnabled by mutableStateOf(true)
    var profileImage by mutableStateOf("")
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
                profileImage = user.profileImage
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
            profileImage = profileImage,
        )
        val error = user.errorMessage()
        if (error != null) {
            message = error
            return
        }
        viewModelScope.launch {
            try {
                database.saveUser(user)
                message = "Profile saved"
            } catch (e: Exception) {
                message = "Couldn't save your profile"
            }
        }
    }

    /**
     * Shrinks the picture at [uri] to 300 pixels wide and stores it as text, so it can be saved
     * in Firestore with the rest of the profile when Update info is pressed.
     */
    fun setImage(uri: Uri) {
        val input = getApplication<Application>().contentResolver.openInputStream(uri) ?: return
        val original = BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return

        val height = original.height * 300 / original.width
        val small = Bitmap.createScaledBitmap(original, 300, height, true)
        val bytes = ByteArrayOutputStream()
        small.compress(Bitmap.CompressFormat.JPEG, 70, bytes)
        profileImage = Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
    }

    /** Deletes device's profile, then runs [onDeleted] */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                database.deleteUser(deviceId)
                onDeleted()
            } catch (e: Exception) {
                message = "Couldn't delete your profile"
            }
        }
    }
}