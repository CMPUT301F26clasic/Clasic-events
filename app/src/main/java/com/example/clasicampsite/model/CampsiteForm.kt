/*
 * Purpose: Whatever the owner typed into the campsite form. Checks it makes sense
 * and turns it into a real Campsite (US 02.01.01 - 02.01.03).
 * Outstanding issues: No image or map location fields yet.
 *  */
package com.example.clasicampsite.model

/**
 * The form's text boxes. Numbers stay as text until saving so typing doesn't get blocked.
 *
 * @property name Campsite name, required
 * @property location Address or area, required
 * @property description Amenities, optional
 * @property maxOccupancy Whole number of guests, 1 or more
 * @property pricePerNight Dollars per night, 0 or more
 * @property tags Comma-separated tags, optional
 * @property image Promo picture as Base64 text, or "" for none (optional)
 * @property latitude Map latitude as text, -90 to 90 (optional)
 * @property longitude Map longitude as text, -180 to 180 (optional)
 */
data class CampsiteForm(
    val name: String = "",
    val location: String = "",
    val description: String = "",
    val maxOccupancy: String = "",
    val pricePerNight: String = "",
    val tags: String = "",
    val image: String = "",
    val latitude: String = "",
    val longitude: String = "",
) {

    /**
     * Makes sure the form is filled in properly.
     * @return the problem to show the user, or null if it's all good
     */
    fun errorMessage(): String? {
        if (name.isBlank()) return "Name is required"
        if (location.isBlank()) return "Location is required"
        val guests = maxOccupancy.trim().toIntOrNull()
        if (guests == null || guests < 1) return "Max occupancy must be a whole number of at least 1"
        val price = pricePerNight.trim().removePrefix("$").toDoubleOrNull()
        if (price == null || price < 0) return "Price must be a number of 0 or more"
        val mapTyped = latitude.isNotBlank() || longitude.isNotBlank()
        if (mapTyped && mapLocation() == null) {
            return "Latitude must be -90 to 90 and longitude -180 to 180"
        }
        return null
    }

    /**
     * Reads the map boxes.
     * @return (latitude, longitude), or null if either box is empty or out of range
     */
    fun mapLocation(): Pair<Double, Double>? {
        val lat = latitude.trim().toDoubleOrNull() ?: return null
        val lng = longitude.trim().toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return Pair(lat, lng)
    }

    /** Turns "lake, pets, lake" into [lake, pets] - trims spaces and drops blanks/repeats. */
    fun tagList(): List<String> =
        tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()


    /**
     * Copies the form onto [original] but keeps stuff the form doesn't touch (id, owner,
     * rating, dates). Only call this after [errorMessage] says it's ok.
     */
    fun applyTo(original: Campsite): Campsite = original.copy(
        name = name.trim(),
        location = location.trim(),
        description = description.trim(),
        maxOccupancy = maxOccupancy.trim().toInt(),
        pricePerNight = pricePerNight.trim().removePrefix("$").toDouble(),
        tags = tagList(),
        image = image,
        latitude = mapLocation()?.first,
        longitude = mapLocation()?.second,
    )

    companion object {
        /** Fills the form back in from a saved [campsite] so it can be viewed or edited. */
        fun from(campsite: Campsite): CampsiteForm = CampsiteForm(
            name = campsite.name,
            location = campsite.location,
            description = campsite.description,
            maxOccupancy = campsite.maxOccupancy.toString(),
            pricePerNight = formatPrice(campsite.pricePerNight),
            tags = campsite.tags.joinToString(", "),
            image = campsite.image,
            latitude = campsite.latitude?.toString() ?: "",
            longitude = campsite.longitude?.toString() ?: "",
        )

        /** Makes prices look normal: 65.0 shows as "65", 65.5 as "65.50". */
        fun formatPrice(price: Double): String =
            if (price % 1.0 == 0.0) price.toLong().toString()
            else String.format(java.util.Locale.US, "%.2f", price)
    }
}