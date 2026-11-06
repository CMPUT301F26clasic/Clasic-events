//Purpose: Unit tests for the campsite search bar and the location, date and guests filter.

package com.example.clasicampsite

import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.CampsiteFilter
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampsiteSearchFilterTest {

    private val pineRidge = Campsite(
        "1", "Pine Ridge", "Jasper, AB", 6, 3.0,
        LocalDate.of(2027, 5, 1), LocalDate.of(2027, 9, 30),
    )

    // ----- Search bar -----

    @Test
    fun blankSearchMatchesEverything() {
        assertTrue(pineRidge.matchesSearch(""))
        assertTrue(pineRidge.matchesSearch("   "))
    }

    @Test
    fun searchMatchesName() {
        assertTrue(pineRidge.matchesSearch("Pine"))
        assertFalse(pineRidge.matchesSearch("Cedar"))
    }

    @Test
    fun searchMatchesLocation() {
        assertTrue(pineRidge.matchesSearch("Jasper"))
        assertFalse(pineRidge.matchesSearch("Banff"))
    }

    @Test
    fun searchIgnoresCaseAndSurroundingSpaces() {
        assertTrue(pineRidge.matchesSearch("  pINe rIdGe  "))
    }

    @Test
    fun numberSearchMatchesCampsitesThatHoldThatManyGuests() {
        assertTrue(pineRidge.matchesSearch("6"))
        assertTrue(pineRidge.matchesSearch("4"))
        assertFalse(pineRidge.matchesSearch("7"))
    }

    // ----- Filter: location -----

    @Test
    fun emptyFilterMatchesEverything() {
        assertTrue(pineRidge.matchesFilter(CampsiteFilter()))
    }

    @Test
    fun locationFilterIgnoresCase() {
        assertTrue(pineRidge.matchesFilter(CampsiteFilter(location = "jasper")))
        assertFalse(pineRidge.matchesFilter(CampsiteFilter(location = "Banff")))
    }

    // ----- Filter: guests -----

    @Test
    fun guestsFilterNeedsEnoughRoom() {
        assertTrue(pineRidge.matchesFilter(CampsiteFilter(guests = 6)))
        assertFalse(pineRidge.matchesFilter(CampsiteFilter(guests = 7)))
    }

    // ----- Filter: dates -----

    @Test
    fun stayInsideAvailabilityMatches() {
        val filter = CampsiteFilter(
            startDate = LocalDate.of(2027, 6, 1),
            endDate = LocalDate.of(2027, 6, 5),
        )
        assertTrue(pineRidge.matchesFilter(filter))
    }

    @Test
    fun stayStartingBeforeAvailabilityDoesNotMatch() {
        val filter = CampsiteFilter(
            startDate = LocalDate.of(2027, 4, 30),
            endDate = LocalDate.of(2027, 5, 5),
        )
        assertFalse(pineRidge.matchesFilter(filter))
    }

    @Test
    fun stayEndingAfterAvailabilityDoesNotMatch() {
        val filter = CampsiteFilter(
            startDate = LocalDate.of(2027, 9, 28),
            endDate = LocalDate.of(2027, 10, 1),
        )
        assertFalse(pineRidge.matchesFilter(filter))
    }

    @Test
    fun firstAndLastAvailableDaysCount() {
        val filter = CampsiteFilter(
            startDate = LocalDate.of(2027, 5, 1),
            endDate = LocalDate.of(2027, 9, 30),
        )
        assertTrue(pineRidge.matchesFilter(filter))
    }

    @Test
    fun singleDateMustBeAvailable() {
        assertTrue(pineRidge.matchesFilter(CampsiteFilter(startDate = LocalDate.of(2027, 7, 4))))
        assertFalse(pineRidge.matchesFilter(CampsiteFilter(startDate = LocalDate.of(2027, 12, 25))))
        assertTrue(pineRidge.matchesFilter(CampsiteFilter(endDate = LocalDate.of(2027, 7, 4))))
        assertFalse(pineRidge.matchesFilter(CampsiteFilter(endDate = LocalDate.of(2027, 1, 1))))
    }

    // ----- Filters combined -----

    @Test
    fun everyFilterMustMatchAtOnce() {
        val allMatch = CampsiteFilter("Jasper", 4, LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 5))
        val wrongLocation = allMatch.copy(location = "Banff")
        val tooManyGuests = allMatch.copy(guests = 12)
        assertTrue(pineRidge.matchesFilter(allMatch))
        assertFalse(pineRidge.matchesFilter(wrongLocation))
        assertFalse(pineRidge.matchesFilter(tooManyGuests))
    }
}