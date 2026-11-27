/*
Purpose: Holds which color blind mode the whole app uses (WOW factor): regular, protanopia,
deuteranopia or tritanopia. The theme reads it and the Profile screen changes it, so the colors
switch right away. It is saved with the user's profile and loaded when the app opens.
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

    /** The modes the user can pick, as (saved value, name shown in the dropdown) */
    val options = listOf(
        "regular" to "Regular",
        "protanopia" to "Protanopia (red-blind)",
        "deuteranopia" to "Deuteranopia (green-blind)",
        "tritanopia" to "Tritanopia (blue-blind)",
    )

    /** The current mode, one of the saved values in [options] */
    var mode by mutableStateOf("regular")

    /** Returns the name shown for the current mode */
    fun label(): String = options.firstOrNull { it.first == mode }?.second ?: "Regular"

    /** Loads this device's saved mode from Firestore, using regular if that fails. */
    suspend fun load(context: Context) {
        mode = try {
            DatabaseController().getUser(AdminAccess.deviceId(context)).colorMode
        } catch (e: Exception) {
            "regular"
        }
    }
}