/**
 * Purpose: Represents a user who has entered a campsite lottery.
 *
 * @property id Unique Firestore document ID
 * @property campsiteId ID of the campsite lottery entered
 * @property userId Device ID of the user who entered
 */
package com.example.clasicampsite.model

/** One user's entry in a campsite lottery. */
data class LotteryEntrant(
    var id: String = "",
    var campsiteId: String = "",
    var userId: String = ""
)