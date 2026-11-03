/*
 * Purpose: Data model for a campsite listing, stored in the Firestore "campsites" collection.
 */
package com.example.clasicampsite.model

/**
 * Represents a campsite.
 *
 * @property id Unique document ID in Firestore
 * @property name Display name of the campsite
 * @property location Physical location / address of the campsite
 * @property maxOccupancy Maximum number of guests allowed
 * @property rating Average rating (e.g., 0.0 to 5.0)
 */
data class Campsite(
    var id: String = "",
    var name: String = "",
    var location: String = "",
    var maxOccupancy: Int = 0,
    var rating: Double = 0.0
)
