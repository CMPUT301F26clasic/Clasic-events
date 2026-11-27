/**
 * Purpose: Loads the notifications this phone's owner has sent to their waitlists, for
 * the Sent Notifications screen (US 02.10.02).
 * Outstanding issues: Only shows notifications sent from the Notify Waitlist screen
 * (US 02.10.01), not automatic lottery win/lose messages.
 * Parts of this file were written with help from Claude (Anthropic), 2026-11-27
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
import com.example.clasicampsite.model.SentNotice
import kotlinx.coroutines.launch

/** Loads the owner's sent notifications for the Sent Notifications screen. */
class SentNotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseController()

    /** The phone's ID, used as the owner ID */
    val deviceId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    /** Each send, newest first */
    var notices by mutableStateOf<List<SentNotice>>(emptyList())
    var loading by mutableStateOf(false)
    var message by mutableStateOf("")

    /** Gets this owner's sent notifications from Firestore. */
    fun load() {
        loading = true
        viewModelScope.launch {
            try {
                notices = SentNotice.group(database.getNotificationsSentBy(deviceId))
                message = ""
            } catch (e: Exception) {
                message = "Couldn't load your sent notifications"
            }
            loading = false
        }
    }
}
