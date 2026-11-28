package com.example.clasicampsite.model

/**
 * A written review of a campsite.
 *
 * @property userId Device ID of the reviewer (also the document ID, so one review per user)
 * @property userName Reviewer's name at the time of posting
 * @property text The review text
 * @property postedAt Time posted, in milliseconds since 1970
 */
data class Review(
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val postedAt: Long = 0L
)