// Purpose: Unit tests for CampsiteForm's checks and conversion (US 02.01.01 - US 02.01.03).

package com.example.clasicampsite

import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.CampsiteForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests CampsiteForm. */
class CampsiteFormTest {

    private val validForm = CampsiteForm(
        name = "Hinton Hideaway",
        location = "Hinton, AB",
        description = "Outdoor cookhouse, tennis court",
        maxOccupancy = "8",
        pricePerNight = "65",
        tags = "tennis, cookhouse",
    )

    private val occupancyError = "Max occupancy must be a whole number of at least 1"
    private val priceError = "Price must be a number of 0 or more"

    @Test
    fun validFormHasNoError() {
        assertNull(validForm.errorMessage())
    }

    @Test
    fun blankNameIsAnError() {
        assertEquals("Name is required", validForm.copy(name = "  ").errorMessage())
    }

    @Test
    fun blankLocationIsAnError() {
        assertEquals("Location is required", validForm.copy(location = "").errorMessage())
    }

    @Test
    fun descriptionAndTagsAreOptional() {
        assertNull(validForm.copy(description = "", tags = "").errorMessage())
    }

    @Test
    fun badOccupancyIsAnError() {
        assertEquals(occupancyError, validForm.copy(maxOccupancy = "").errorMessage())
        assertEquals(occupancyError, validForm.copy(maxOccupancy = "0").errorMessage())
        assertEquals(occupancyError, validForm.copy(maxOccupancy = "2.5").errorMessage())
        assertEquals(occupancyError, validForm.copy(maxOccupancy = "eight").errorMessage())
    }

    @Test
    fun badPriceIsAnError() {
        assertEquals(priceError, validForm.copy(pricePerNight = "").errorMessage())
        assertEquals(priceError, validForm.copy(pricePerNight = "-5").errorMessage())
        assertEquals(priceError, validForm.copy(pricePerNight = "free").errorMessage())
    }

    @Test
    fun priceMayHaveDollarSignOrCents() {
        assertNull(validForm.copy(pricePerNight = "$65").errorMessage())
        assertNull(validForm.copy(pricePerNight = "65.50").errorMessage())
    }

    @Test
    fun tagsAreSplitTrimmedAndDeduplicated() {
        val form = validForm.copy(tags = " lake , pets,, lake ,")
        assertEquals(listOf("lake", "pets"), form.tagList())
    }

    @Test
    fun applyToKeepsIdOwnerAndRating() {
        val original = Campsite(id = "abc", ownerId = "device-1", rating = 4.5)
        val saved = validForm.applyTo(original)
        assertEquals("abc", saved.id)
        assertEquals("device-1", saved.ownerId)
        assertEquals(4.5, saved.rating, 0.0)
        assertEquals("Hinton Hideaway", saved.name)
        assertEquals(8, saved.maxOccupancy)
        assertEquals(65.0, saved.pricePerNight, 0.0)
        assertEquals(listOf("tennis", "cookhouse"), saved.tags)
    }

    @Test
    fun fromThenApplyToGivesSameCampsite() {
        val campsite = Campsite(
            id = "abc", ownerId = "device-1", name = "Lakeview", location = "Jasper, AB",
            description = "Fire pit", maxOccupancy = 4, pricePerNight = 42.5, tags = listOf("lake"),
        )
        assertEquals(campsite, CampsiteForm.from(campsite).applyTo(campsite))
    }

    @Test
    fun priceIsFormattedWithoutTrailingZeros() {
        assertEquals("65", CampsiteForm.formatPrice(65.0))
        assertEquals("65.50", CampsiteForm.formatPrice(65.5))
    }
}