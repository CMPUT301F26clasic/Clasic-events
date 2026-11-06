/*
 * Purpose: Data model for a campsite listing, stored in the Firestore "campsites" collection.
 */
package com.example.clasicampsite.model

import java.time.LocalDate

/**
 * Represents a campsite.
 *
 * @property id Unique document ID in Firestore
 * @property name Display name of the campsite
 * @property location Physical location / address of the campsite
 * @property maxOccupancy Maximum number of guests allowed
 * @property rating Average rating (e.g., 0.0 to 5.0)
 * @property availableFrom Start date of availability
 * @property availableTo End date of availability
 */
data class Campsite(
    var id: String = "",
    var name: String = "",
    var location: String = "",
    var maxOccupancy: Int = 0,
    var rating: Double = 0.0,
    val availableFrom: LocalDate = LocalDate.of(1970, 1, 1),
    val availableTo: LocalDate = LocalDate.of(1970, 1, 1)
)

data class CampsiteFilter(
    val location: String = "",
    val guests: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
) {
    val activeCount: Int
        get() = listOf(
            location.isNotBlank(),
            guests != null,
            startDate != null,
            endDate != null
        ).count { it }
}
