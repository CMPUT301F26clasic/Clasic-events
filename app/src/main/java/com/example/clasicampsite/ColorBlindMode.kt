/*
Purpose: Holds whether color blind mode is on for the whole app. The theme reads it and
the Profile screen changes it, so the colors switch right away. It is saved with the
user's profile and loaded when the app opens.
Outstanding issues: None.
 */
package com.example.clasicampsite

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.clasicampsite.data.DatabaseController

/** The app-wide color blind mode setting. */
object ColorBlindMode {

    /** True when color blind mode is on */
    var enabled by mutableStateOf(false)

    /** Loads this device's saved setting from Firestore, leaving it off if that fails. */
    suspend fun load(context: Context) {
        enabled = try {
            DatabaseController().getUser(AdminAccess.deviceId(context)).colorBlindMode
        } catch (e: Exception) {
            false
        }
    }
}