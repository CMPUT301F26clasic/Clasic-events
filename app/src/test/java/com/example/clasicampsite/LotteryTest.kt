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
        campsiteId = "hinton-1",
        winners = listOf("device-a"),
        availableDates = listOf("2027-05-14", "2027-05-07", "2027-05-08", "2027-05-15"),
        bookedDates = listOf("2027-05-08"),
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
    fun bookedDatesAreNotFree() {
        assertEquals(listOf("2027-05-07", "2027-05-14", "2027-05-15"), lottery.freeDates("2027-05-01"))
    }

    @Test
    fun pastDatesAreNotFree() {
        assertEquals(listOf("2027-05-14", "2027-05-15"), lottery.freeDates("2027-05-10"))
    }

    @Test
    fun todayIsStillFree() {
        assertEquals(listOf("2027-05-14", "2027-05-15"), lottery.freeDates("2027-05-14"))
    }

    @Test
    fun noDatesLeftGivesAnEmptyList() {
        assertTrue(lottery.freeDates("2027-06-01").isEmpty())
        assertTrue(Lottery().freeDates("2027-05-01").isEmpty())
    }
}