// Purpose: Unit tests for Lottery: who won and which dates are still free.

package com.example.clasicampsite

import com.example.clasicampsite.model.Lottery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LotteryTest {

    private val lottery = Lottery(
        id = "test-lottery",
        title = "May weekends",
        campsiteIds = listOf("hinton-1", "jasper-1"),
        winners = listOf("device-a"),
        availableDates = listOf("2027-05-14", "2027-05-07", "2027-05-08", "2027-05-15"),
    )

    @Test
    fun winnerIsRecognized() {
        assertTrue(lottery.isWinner("device-a"))
    }

    @Test
    fun someoneWhoDidNotWinIsNotAWinner() {
        assertFalse(lottery.isWinner("device-b"))
    }

    @Test
    fun lotteryWithTwoCampsitesHasMultipleCampsites() {
        assertTrue(lottery.hasMultipleCampsites())
        assertFalse(lottery.copy(campsiteIds = listOf("hinton-1")).hasMultipleCampsites())
    }

    @Test
    fun bookedDatesAreNotFree() {
        assertEquals(
            listOf("2027-05-07", "2027-05-14", "2027-05-15"),
            lottery.freeDates("2027-05-01", listOf("2027-05-08")),
        )
    }

    @Test
    fun pastDatesAreNotFree() {
        assertEquals(listOf("2027-05-14", "2027-05-15"), lottery.freeDates("2027-05-10", emptyList()))
    }

    @Test
    fun todayIsStillFree() {
        assertEquals(listOf("2027-05-14", "2027-05-15"), lottery.freeDates("2027-05-14", emptyList()))
    }

    @Test
    fun noDatesLeftGivesAnEmptyList() {
        assertTrue(lottery.freeDates("2027-06-01", emptyList()).isEmpty())
        assertTrue(Lottery().freeDates("2027-05-01", emptyList()).isEmpty())
    }
}
