/**
 * Purpose: Unit tests for CampsiteForm's checks and conversion, including the promo
 * picture and map location (US 02.01.01 - US 02.01.03).
 * Outstanding issues: None known.
 * Parts of this file were written with help from Claude (Anthropic), 2026-11-03, updated 2026-11-24
 */
package com.example.clasicampsite

import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.CampsiteForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Unit tests for CampsiteForm, the register (US 02.01.01) and edit (US 02.01.03) campsite form. */
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
    private val mapError = "Latitude must be -90 to 90 and longitude -180 to 180"

    /** US 02.01.01: a filled in form has no error */
    @Test
    fun validFormHasNoError() {
        assertNull(validForm.errorMessage())
    }

    /** US 02.01.01: a campsite needs a name */
    @Test
    fun blankNameIsAnError() {
        assertEquals("Name is required", validForm.copy(name = "  ").errorMessage())
    }

    /** US 02.01.01: a campsite needs a location */
    @Test
    fun blankLocationIsAnError() {
        assertEquals("Location is required", validForm.copy(location = "").errorMessage())
    }

    /** US 02.01.01: the description and tags are optional */
    @Test
    fun descriptionAndTagsAreOptional() {
        assertNull(validForm.copy(description = "", tags = "").errorMessage())
    }

    /** US 02.01.01: max occupancy must be a whole number of at least 1 */
    @Test
    fun badOccupancyIsAnError() {
        assertEquals(occupancyError, validForm.copy(maxOccupancy = "").errorMessage())
        assertEquals(occupancyError, validForm.copy(maxOccupancy = "0").errorMessage())
        assertEquals(occupancyError, validForm.copy(maxOccupancy = "2.5").errorMessage())
        assertEquals(occupancyError, validForm.copy(maxOccupancy = "eight").errorMessage())
    }

    /** US 02.01.01: the price must be a number of 0 or more */
    @Test
    fun badPriceIsAnError() {
        assertEquals(priceError, validForm.copy(pricePerNight = "").errorMessage())
        assertEquals(priceError, validForm.copy(pricePerNight = "-5").errorMessage())
        assertEquals(priceError, validForm.copy(pricePerNight = "free").errorMessage())
    }

    /** US 02.01.01: the price can have a dollar sign or cents */
    @Test
    fun priceMayHaveDollarSignOrCents() {
        assertNull(validForm.copy(pricePerNight = "$65").errorMessage())
        assertNull(validForm.copy(pricePerNight = "65.50").errorMessage())
    }

    /** US 02.01.01: tags are split on commas, trimmed and not repeated */
    @Test
    fun tagsAreSplitTrimmedAndDeduplicated() {
        val form = validForm.copy(tags = " lake , pets,, lake ,")
        assertEquals(listOf("lake", "pets"), form.tagList())
    }

    /** US 02.01.03: saving an edit keeps the campsite's ID, owner and rating */
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

    /** US 02.01.03: opening a campsite to edit and saving it unchanged gives the same campsite */
    @Test
    fun fromThenApplyToGivesSameCampsite() {
        val campsite = Campsite(
            id = "abc", ownerId = "device-1", name = "Lakeview", location = "Jasper, AB",
            description = "Fire pit", maxOccupancy = 4, pricePerNight = 42.5, tags = listOf("lake"),
        )
        assertEquals(campsite, CampsiteForm.from(campsite).applyTo(campsite))
    }

    /** US 02.01.03: the price shown for editing has no extra zeros */
    @Test
    fun priceIsFormattedWithoutTrailingZeros() {
        assertEquals("65", CampsiteForm.formatPrice(65.0))
        assertEquals("65.50", CampsiteForm.formatPrice(65.5))
    }

    /** US 02.01.01: the map location and image are optional */
    @Test
    fun mapLocationAndImageAreOptional() {
        assertNull(validForm.errorMessage())
        assertNull(validForm.mapLocation())
        assertNull(validForm.applyTo(Campsite()).latitude)
        assertEquals("", validForm.applyTo(Campsite()).image)
    }

    /** US 02.01.01: a valid map location is read from the form */
    @Test
    fun validMapLocationIsRead() {
        val form = validForm.copy(latitude = " 53.40 ", longitude = "-117.58")
        assertNull(form.errorMessage())
        assertEquals(Pair(53.4, -117.58), form.mapLocation())
    }

    /** US 02.01.01: a map location out of range or half filled in is an error */
    @Test
    fun badMapLocationIsAnError() {
        assertEquals(mapError, validForm.copy(latitude = "95", longitude = "10").errorMessage())
        assertEquals(mapError, validForm.copy(latitude = "53", longitude = "-200").errorMessage())
        assertEquals(mapError, validForm.copy(latitude = "53.4", longitude = "").errorMessage())
        assertEquals(mapError, validForm.copy(latitude = "north", longitude = "west").errorMessage())
    }

    /** US 02.01.03: editing a campsite keeps its image and map location */
    @Test
    fun fromThenApplyToKeepsImageAndMapLocation() {
        val campsite = Campsite(
            id = "abc", ownerId = "device-1", name = "Lakeview", location = "Jasper, AB",
            maxOccupancy = 4, pricePerNight = 42.5,
            image = "aW1hZ2U=", latitude = 52.87, longitude = -118.08,
        )
        assertEquals(campsite, CampsiteForm.from(campsite).applyTo(campsite))
    }

    /** US 02.01.01: available dates have to be picked together */
    @Test
    fun onlyOneAvailableDateIsAnError() {
        assertEquals("Pick both available dates", validForm.copy(availableFrom = LocalDate.of(2027, 5, 1)).errorMessage())
    }

    /** US 02.01.01: the last available night can't be before the first */
    @Test
    fun datesInWrongOrderAreAnError() {
        val form = validForm.copy(availableFrom = LocalDate.of(2027, 5, 10), availableTo = LocalDate.of(2027, 5, 1))
        assertEquals("The last available date can't be before the first", form.errorMessage())
    }

    /** US 02.01.01, US 02.04.05: the number of winners has to be at least 1 if it is typed */
    @Test
    fun badNumberOfWinnersIsAnError() {
        val error = "Number of winners must be a whole number of at least 1"
        assertEquals(error, validForm.copy(numberOfWinners = "0").errorMessage())
        assertEquals(error, validForm.copy(numberOfWinners = "two").errorMessage())
        assertNull(validForm.copy(numberOfWinners = "3").errorMessage())
    }

    /** US 02.01.01: the dates and number of winners are saved with the campsite */
    @Test
    fun datesAndWinnersAreSaved() {
        val form = validForm.copy(
            availableFrom = LocalDate.of(2027, 5, 1), availableTo = LocalDate.of(2027, 9, 30), numberOfWinners = "4",
        )
        val saved = form.applyTo(Campsite())
        assertEquals(LocalDate.of(2027, 5, 1), saved.availableFrom)
        assertEquals(LocalDate.of(2027, 9, 30), saved.availableTo)
        assertEquals(4, saved.numberOfWinners)
    }

    /** US 02.01.03: editing a campsite fills in its dates and winners, and saving keeps them */
    @Test
    fun fromThenApplyToKeepsDatesAndWinners() {
        val campsite = Campsite(
            id = "abc", name = "Lakeview", location = "Jasper, AB", maxOccupancy = 4,
            availableFrom = LocalDate.of(2027, 6, 1), availableTo = LocalDate.of(2027, 6, 30), numberOfWinners = 3,
        )
        val form = CampsiteForm.from(campsite)
        assertEquals(LocalDate.of(2027, 6, 1), form.availableFrom)
        assertEquals("3", form.numberOfWinners)
        assertEquals(campsite, form.applyTo(campsite))
    }
}
