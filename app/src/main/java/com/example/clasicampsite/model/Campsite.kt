/*
 * Purpose: Data model for a campsite listing, stored in the Firestore "campsites" collection.
 * Outstanding issues: Promo image (US 02.01.02) and map location are not added yet.
 */
package com.example.clasicampsite.model

import java.time.LocalDate

/**
 * Represents a campsite.
 *
 * @property id Unique document ID in Firestore
 * @property ownerId Device ID of the campsite owner who registered it
 * @property name Display name of the campsite
 * @property location Physical location / address of the campsite
 * @property description Amenities and other details written by the owner
 * @property maxOccupancy Maximum number of guests allowed
 * @property pricePerNight Price for one night, in dollars
 * @property tags Short labels such as "lakeside" or "pet friendly"
 * @property rating Average rating (e.g., 0.0 to 5.0)
 * @property availableFrom Start date of availability
 * @property availableTo End date of availability
 */
data class Campsite(
    var id: String = "",
    var ownerId: String = "",
    var name: String = "",
    var location: String = "",
    var description: String = "",
    var maxOccupancy: Int = 0,
    var pricePerNight: Double = 0.0,
    var tags: List<String> = emptyList(),
    var rating: Double = 0.0,
    val availableFrom: LocalDate = LocalDate.of(1970, 1, 1),
    val availableTo: LocalDate = LocalDate.of(1970, 1, 1),
    var numberOfWinners: Int = 0
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
