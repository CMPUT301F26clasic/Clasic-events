/*
 * Purpose: The text a campsite owner types when registering or editing a campsite
 *          (US 02.01.01, US 02.01.02, US 02.01.03). Checks it and turns it into a Campsite.
 * Outstanding issues: No image or map location fields yet.
 */
package com.example.clasicampsite.model

/**
 * What the owner has typed into the campsite form. Numbers are kept as text until saved,
 * so the owner can type freely.
 *
 * @property name Campsite name, required
 * @property location Address or area, required
 * @property description Amenities, optional
 * @property maxOccupancy Whole number of guests, 1 or more
 * @property pricePerNight Dollars per night, 0 or more
 * @property tags Comma-separated tags, optional
 */
data class CampsiteForm(
    val name: String = "",
    val location: String = "",
    val description: String = "",
    val maxOccupancy: String = "",
    val pricePerNight: String = "",
    val tags: String = "",
) {
    /**
     * Checks the form.
     * @return What is wrong with it, or null if everything is valid.
     */
    fun errorMessage(): String? {
        if (name.isBlank()) return "Name is required"
        if (location.isBlank()) return "Location is required"
        val guests = maxOccupancy.trim().toIntOrNull()
        if (guests == null || guests < 1) return "Max occupancy must be a whole number of at least 1"
        val price = pricePerNight.trim().removePrefix("$").toDoubleOrNull()
        if (price == null || price < 0) return "Price must be a number of 0 or more"
        return null
    }

    /** Splits the tags text on commas, dropping blanks and repeats. */
    fun tagList(): List<String> =
        tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /**
     * Copies the form onto [original], keeping fields the form doesn't cover (id, owner, rating).
     * Call only when [errorMessage] is null.
     */
    fun applyTo(original: Campsite): Campsite = original.copy(
        name = name.trim(),
        location = location.trim(),
        description = description.trim(),
        maxOccupancy = maxOccupancy.trim().toInt(),
        pricePerNight = pricePerNight.trim().removePrefix("$").toDouble(),
        tags = tagList(),
    )

    companion object {
        /** Fills a form from a saved [campsite], so the owner can view or edit it. */
        fun from(campsite: Campsite): CampsiteForm = CampsiteForm(
            name = campsite.name,
            location = campsite.location,
            description = campsite.description,
            maxOccupancy = campsite.maxOccupancy.toString(),
            pricePerNight = formatPrice(campsite.pricePerNight),
            tags = campsite.tags.joinToString(", "),
        )

        /** Shows 65.0 as "65" and 65.5 as "65.50". */
        fun formatPrice(price: Double): String =
            if (price % 1.0 == 0.0) price.toLong().toString()
            else String.format(java.util.Locale.US, "%.2f", price)
    }
}