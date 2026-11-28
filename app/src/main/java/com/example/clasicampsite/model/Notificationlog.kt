package com.example.clasicampsite.model

/**
 * One entry in the notification log.
 *
 * @property id Firestore document ID
 * @property recipientId Device ID of the user who received it
 * @property senderId Device ID of the sender, or empty for system notifications
 * @property message The text that was sent
 * @property sentAt Time sent, in milliseconds since 1970
 * @property campsiteId Campsite the notification relates to, or empty
 * @property campsiteName Name of that campsite, or empty
 * @property type "owner_message", "lottery_result" or "system"
 */
data class NotificationLog(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val message: String = "",
    val sentAt: Long = 0L,
    val campsiteId: String = "",
    val campsiteName: String = "",
    val type: String = "system"
)