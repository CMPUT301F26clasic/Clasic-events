/**
 * Purpose: The notifications a campsite owner has sent, for the Sent Notifications list
 * (US 02.10.02). Firestore keeps one notification per entrant, so this groups each send
 * back into one item with how many entrants got it.
 * Outstanding issues: None known.
 * Parts of this file were written with help from Claude (Anthropic), 2026-11-27
 */
package com.example.clasicampsite.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One notification an owner sent to one entrant, as saved in Firestore.
 *
 * @property userId Device ID of the entrant who got it
 * @property message The text the entrant saw
 * @property sentAt When it was sent, in milliseconds since 1970
 * @property campsiteId The campsite it was about
 */
data class SentNotification(
    val userId: String = "",
    val message: String = "",
    val sentAt: Long = 0L,
    val campsiteId: String = "",
)

/**
 * One send from the owner, which may have gone to many entrants.
 *
 * @property message The text the entrants saw
 * @property campsiteId The campsite it was about
 * @property sentAt When it was sent, in milliseconds since 1970
 * @property recipientCount How many entrants got it
 */
data class SentNotice(
    val message: String = "",
    val campsiteId: String = "",
    val sentAt: Long = 0L,
    val recipientCount: Int = 0,
) {

    /** e.g. "Sent to 1 entrant" or "Sent to 4 entrants" */
    fun recipientsText(): String =
        if (recipientCount == 1) "Sent to 1 entrant" else "Sent to $recipientCount entrants"

    /** When it was sent, e.g. "Nov 27, 2:15 PM", in the phone's time zone unless [zone] is given. */
    fun timeText(zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(sentAt).atZone(zone).format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US))

    companion object {
        /**
         * Groups single notifications into sends: the same campsite, message and send time
         * count as one send. Each entrant is counted once.
         * @return the sends, newest first
         */
        fun group(sent: List<SentNotification>): List<SentNotice> =
            sent.groupBy { Triple(it.campsiteId, it.message, it.sentAt) }
                .map { (key, notifications) ->
                    SentNotice(
                        message = key.second,
                        campsiteId = key.first,
                        sentAt = key.third,
                        recipientCount = notifications.map { it.userId }.distinct().size,
                    )
                }
                .sortedByDescending { it.sentAt }
    }
}
