/**
 * Purpose: Unit tests for Lottery: who won, who declined, which dates are still free, and
 * drawing winners for a campsite.
 * Outstanding issues: None.
 */
package com.example.clasicampsite

import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.Lottery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/** Unit tests for the Lottery model class. */
class LotteryTest {

    private val lottery = Lottery(
        id = "test-lottery",
        title = "May weekends",
        campsiteIds = listOf("hinton-1", "jasper-1"),
        winners = listOf("device-a", "device-c"),
        declined = listOf("device-c"),
        availableDates = listOf("2027-05-14", "2027-05-07", "2027-05-08", "2027-05-15"),
    )

    /** US 01.04.01: a winner is recognized as a winner */
    @Test
    fun winnerIsRecognized() {
        assertTrue(lottery.isWinner("device-a"))
    }

    /** US 01.04.01: someone who didn't win is not a winner */
    @Test
    fun someoneWhoDidNotWinIsNotAWinner() {
        assertFalse(lottery.isWinner("device-b"))
    }

    /** US 01.04.03: a winner who declined is no longer a winner */
    @Test
    fun winnerWhoDeclinedIsNoLongerAWinner() {
        assertFalse(lottery.isWinner("device-c"))
        assertTrue(lottery.hasDeclined("device-c"))
        assertFalse(lottery.hasDeclined("device-a"))
    }

    /** US 01.04.02: a lottery with two campsites lets the winner pick between them */
    @Test
    fun lotteryWithTwoCampsitesHasMultipleCampsites() {
        assertTrue(lottery.hasMultipleCampsites())
        assertFalse(lottery.copy(campsiteIds = listOf("hinton-1")).hasMultipleCampsites())
    }

    /** US 01.04.01: dates that are already booked are not free */
    @Test
    fun bookedDatesAreNotFree() {
        assertEquals(
            listOf("2027-05-07", "2027-05-14", "2027-05-15"),
            lottery.freeDates("2027-05-01", listOf("2027-05-08")),
        )
    }

    /** US 01.04.01: dates that have passed are not free */
    @Test
    fun pastDatesAreNotFree() {
        assertEquals(listOf("2027-05-14", "2027-05-15"), lottery.freeDates("2027-05-10", emptyList()))
    }

    /** US 01.04.01: today can still be booked */
    @Test
    fun todayIsStillFree() {
        assertEquals(listOf("2027-05-14", "2027-05-15"), lottery.freeDates("2027-05-14", emptyList()))
    }

    /** US 01.04.01: with no dates left, the list of free dates is empty */
    @Test
    fun noDatesLeftGivesAnEmptyList() {
        assertTrue(lottery.freeDates("2027-06-01", emptyList()).isEmpty())
        assertTrue(Lottery().freeDates("2027-05-01", emptyList()).isEmpty())
    }

    /** US 02.05.01: the draw picks the number of winners asked for, all different */
    @Test
    fun pickWinnersTakesThatManyDifferentEntrants() {
        val winners = Lottery.pickWinners(listOf("a", "b", "c", "d"), 2, Random(1))
        assertEquals(2, winners.size)
        assertEquals(2, winners.distinct().size)
        assertTrue(winners.all { it in listOf("a", "b", "c", "d") })
    }

    /** US 02.05.01: with fewer entrants than winners, everyone wins */
    @Test
    fun pickWinnersWithFewEntrantsPicksEveryone() {
        assertEquals(setOf("a", "b"), Lottery.pickWinners(listOf("a", "b"), 5, Random(1)).toSet())
    }

    /** US 02.05.01: blank IDs and people who joined twice only count once */
    @Test
    fun pickWinnersSkipsBlanksAndRepeats() {
        assertEquals(listOf("a"), Lottery.pickWinners(listOf("a", "", "a"), 3, Random(1)))
    }

    /** US 02.05.01: a lottery with no winners yet hasn't been drawn */
    @Test
    fun hasBeenDrawnOnlyOnceThereAreWinners() {
        assertTrue(lottery.hasBeenDrawn())
        assertFalse(Lottery().hasBeenDrawn())
    }

    /** US 01.04.01: the nights winners can book run from the first to the last available night */
    @Test
    fun datesBetweenListsEveryNight() {
        assertEquals(
            listOf("2027-05-30", "2027-05-31", "2027-06-01"),
            Lottery.datesBetween(LocalDate.of(2027, 5, 30), LocalDate.of(2027, 6, 1), LocalDate.of(2027, 1, 1)),
        )
    }

    /** US 01.04.01: nights that already passed are left out */
    @Test
    fun datesBetweenSkipsPastNights() {
        assertEquals(
            listOf("2027-05-02", "2027-05-03"),
            Lottery.datesBetween(LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 3), LocalDate.of(2027, 5, 2)),
        )
        assertTrue(
            Lottery.datesBetween(LocalDate.of(1970, 1, 1), LocalDate.of(1970, 1, 1), LocalDate.of(2027, 1, 1)).isEmpty()
        )
    }

    /** US 02.05.01: drawing for a campsite saves a lottery with its ID, name, winners and dates */
    @Test
    fun forCampsiteBuildsTheLotteryToSave() {
        val campsite = Campsite(
            id = "camp-1", name = "Pine Ridge",
            availableFrom = LocalDate.of(2027, 7, 1), availableTo = LocalDate.of(2027, 7, 2),
        )
        val drawn = Lottery.forCampsite(campsite, listOf("device-a"), LocalDate.of(2027, 1, 1))
        assertEquals("camp-1", drawn.id)
        assertEquals("Pine Ridge", drawn.title)
        assertEquals(listOf("camp-1"), drawn.campsiteIds)
        assertEquals(listOf("device-a"), drawn.winners)
        assertEquals(listOf("2027-07-01", "2027-07-02"), drawn.availableDates)
    }
}
