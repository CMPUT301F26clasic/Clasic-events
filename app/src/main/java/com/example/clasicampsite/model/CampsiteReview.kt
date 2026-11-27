/*
Purpose: Data model for a campsite rating/review stored in the
campsites/{campsiteId}/ratings subcollection (US 03.05.01 / #71).
Outstanding issues: None.
 */
package com.example.clasicampsite.model

/**
 * Represents a user's rating and optional review comment on a campsite.
 *
 * @property id Firestore document ID (usually the reviewer's device ID)
 * @property campsiteId ID of the campsite that was reviewed
 * @property campsiteName Name of the campsite shown in the admin review list
 * @property userId Device ID of the user who wrote the review
 * @property rating Star rating from 1 to 5
 * @property comment Optional text review written by the user
 */
data class CampsiteReview(
    var id: String = "",
    var campsiteId: String = "",
    var campsiteName: String = "",
    var userId: String = "",
    var rating: Int = 0,
    var comment: String = "",
)