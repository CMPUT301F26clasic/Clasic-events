/*
Purpose: Decides who is an admin (US 03.11.01). A user is an admin when their Firestore user
document has admin = true, which is set by hand in Firebase. Also holds the message shown to
non-admins who reach an admin screen.
Outstanding issues: None.
 */
package com.example.clasicampsite

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Checks whether the person using this device is an admin. */
/** Checks whether the person using this device is an admin. */
object AdminAccess {

    // true when an admin picked "View as: User" on Profile. Resets when the app closes.
    var viewAsUser by mutableStateOf(false)

    /** Returns this device's ID, the same one profiles are saved under. */
    @SuppressLint("HardwareIds")
    fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    /**
     * Returns true if this device's user is an admin, or false if not, if Firestore fails,
     * or if the admin is viewing the app as a user.
     */
    suspend fun isAdmin(context: Context): Boolean {
        if (viewAsUser) return false // NEW
        return try {
            DatabaseController().getUser(deviceId(context)).admin
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Shown instead of an admin screen when the user isn't an admin.
 * @param checking True while still asking Firestore, so it says "Checking access..." instead.
 */
@Composable
fun AdminOnlyMessage(navController: NavController, checking: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton"),
        ) {
            Text("Back")
        }
        Text(
            if (checking) "Checking access..." else "Admins only",
            fontSize = 24.sp,
            modifier = Modifier.testTag("adminOnlyMessage"),
        )
    }
}