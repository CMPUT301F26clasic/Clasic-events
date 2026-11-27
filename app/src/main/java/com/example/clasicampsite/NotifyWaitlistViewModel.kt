/*
 * Purpose: Loads a campsite's lottery waitlist and sends the owner's notification to
 * everyone on it who hasn't turned notifications off (US 02.10.01).
 * Outstanding issues: Sends notifications one at a time, so a very long waitlist takes
 * a few seconds.
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
import com.example.clasicampsite.model.WaitlistNotice
import kotlinx.coroutines.launch

/** Holds what the Notify Waitlist screen needs, so it survives screen rotations. */
class NotifyWaitlistViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseController()

    /** The phone's ID, used as the owner ID */
    val deviceId: String = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)

    var campsiteName by mutableStateOf("")
    /** Device IDs of everyone on this campsite's waitlist */
    var waitlistUserIds by mutableStateOf<List<String>>(emptyList())
    var message by mutableStateOf("")
    var status by mutableStateOf("")
    var loaded by mutableStateOf(false)
    var sending by mutableStateOf(false)
    /** Only the phone that registered the campsite can notify its waitlist */
    var isOwner by mutableStateOf(false)

    private var campsiteId = ""
    private var started = false

    /** Runs once when the screen opens: loads the campsite and its waitlist. */
    fun start(campsiteId: String) {
        if (started) return
        started = true
        this.campsiteId = campsiteId
        viewModelScope.launch {
            try {
                val campsite = database.getCampsite(campsiteId)
                if (campsite == null) {
                    status = "Campsite not found"
                } else {
                    campsiteName = campsite.name
                    isOwner = campsite.ownerId == deviceId
                    if (!isOwner) status = "Only the campsite's owner can notify its waitlist"
                    waitlistUserIds = database.getLotteryEntrants(campsiteId)
                        .map { it.userId }
                        .filter { it.isNotBlank() }
                        .distinct()
                }
            } catch (e: Exception) {
                status = "Couldn't load the waitlist"
            }
            loaded = true
        }
    }

    /**
     * Checks the message, then sends it to everyone on the waitlist except people who
     * turned notifications off. Shows how many it was sent to.
     */
    fun send() {
        if (sending || !isOwner) return
        val notice = WaitlistNotice(campsiteName, message)
        val error = notice.errorMessage(waitlistUserIds.size)
        if (error != null) {
            status = error
            return
        }
        sending = true
        status = ""
        viewModelScope.launch {
            try {
                val optedOut = waitlistUserIds.filter { !database.getUser(it).notificationsEnabled }.toSet()
                val recipients = WaitlistNotice.recipients(waitlistUserIds, optedOut)
                for (userId in recipients) {
                    database.sendOwnerNotification(userId, notice.fullText(), deviceId, campsiteId)
                }
                status = WaitlistNotice.sentSummary(recipients.size, optedOut.size)
                if (recipients.isNotEmpty()) message = ""
            } catch (e: Exception) {
                status = "Couldn't send the notification"
            }
            sending = false
        }
    }
}
