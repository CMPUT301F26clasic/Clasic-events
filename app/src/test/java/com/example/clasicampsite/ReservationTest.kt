/**
 * Purpose: Unit tests for Reservation: nights and booked dates (US 01.04.01, US 01.04.02),
 * the CSV download (US 01.05.02), and splitting reservations into active and past
 * (US 01.06.01, US 01.06.02), which reservations can be cancelled (US 01.07.01), and the
 * cancellation confirmation message and CSV (US 01.07.02).
 * Outstanding issues: None.
 */

package com.example.clasicampsite

import com.example.clasicampsite.model.Reservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the Reservation model class. */
class ReservationTest {

    /** A two-night reservation used by most of the tests */
    private val reservation = Reservation(
        id = "res-1",
        lotteryId = "test-lottery",
        campsiteId = "hinton-1",
        campsiteName = "Grande Cache Ridge",
        userId = "device-a",
        dates = listOf("2027-05-08", "2027-05-07"),
    )

    /** US 01.04.02: a reservation counts one night per date */
    @Test
    fun nightsIsTheNumberOfDates() {
        assertEquals(2, reservation.nights())
    }

    /** US 01.04.02: a new reservation starts out active */
    @Test
    fun newReservationIsActive() {
        assertEquals("active", Reservation().status)
    }

    /** US 01.04.01: active reservations take their dates out of the available ones */
    @Test
    fun bookedDatesComeFromActiveReservations() {
        val other = Reservation(id = "res-2", dates = listOf("2027-05-14"))
        assertEquals(
            listOf("2027-05-08", "2027-05-07", "2027-05-14"),
            Reservation.bookedDates(listOf(reservation, other)),
        )
    }

    /** US 01.04.01: a cancelled reservation frees its dates again */
    @Test
    fun cancelledReservationsDoNotBookDates() {
        val cancelled = Reservation(id = "res-3", dates = listOf("2027-05-21"), status = "cancelled")
        assertEquals(listOf("2027-05-08", "2027-05-07"), Reservation.bookedDates(listOf(reservation, cancelled)))
    }

    /** US 01.04.01: with no reservations, every date is free */
    @Test
    fun noReservationsMeansNoBookedDates() {
        assertTrue(Reservation.bookedDates(emptyList()).isEmpty())
    }

    /** US 01.05.02: the downloaded CSV has a header row and one row for the reservation */
    @Test
    fun csvHasAHeaderAndOneRow() {
        val lines = reservation.toCsv().trim().lines()
        assertEquals(2, lines.size)
        assertEquals("Reservation ID,Campsite,Dates,Nights,Status", lines[0])
        assertEquals("res-1,\"Grande Cache Ridge\",2027-05-07 2027-05-08,2,active", lines[1])
    }

    /** US 01.06.01: a reservation whose nights are still coming up is active */
    @Test
    fun upcomingReservationIsActive() {
        val today = "2027-05-01"
        assertEquals(listOf(reservation), Reservation.activeReservations(listOf(reservation), today))
        assertTrue(Reservation.pastReservations(listOf(reservation), today).isEmpty())
    }

    /** US 01.06.01: a reservation is still active on its last night */
    @Test
    fun reservationIsStillActiveOnItsLastNight() {
        assertTrue(reservation.isUpcoming("2027-05-08"))
    }

    /** US 01.06.02: once its last night has passed, a reservation is past */
    @Test
    fun finishedReservationIsPast() {
        val today = "2027-06-01"
        assertTrue(Reservation.activeReservations(listOf(reservation), today).isEmpty())
        assertEquals(listOf(reservation), Reservation.pastReservations(listOf(reservation), today))
    }

    /** US 01.06.02: a cancelled reservation shows as past, even if its dates haven't happened yet */
    @Test
    fun cancelledReservationIsPast() {
        val cancelled = reservation.copy(id = "res-4", status = "cancelled")
        assertEquals(listOf(cancelled), Reservation.pastReservations(listOf(cancelled), "2027-05-01"))
    }

    /** US 01.06.01: active reservations are listed soonest first */
    @Test
    fun activeReservationsAreSortedSoonestFirst() {
        val later = Reservation(id = "res-5", campsiteName = "Two Jack Lakeside", dates = listOf("2027-07-03"))
        val active = Reservation.activeReservations(listOf(later, reservation), "2027-05-01")
        assertEquals(listOf("res-1", "res-5"), active.map { it.id })
    }

    /** US 01.06.02: past reservations are listed most recent first */
    @Test
    fun pastReservationsAreSortedMostRecentFirst() {
        val older = Reservation(id = "res-6", campsiteName = "Lake Louise Campground", dates = listOf("2026-08-14"))
        val past = Reservation.pastReservations(listOf(older, reservation), "2027-09-01")
        assertEquals(listOf("res-1", "res-6"), past.map { it.id })
    }

    /** US 01.07.01: an active reservation that hasn't happened yet can be cancelled */
    @Test
    fun upcomingReservationCanBeCancelled() {
        assertTrue(reservation.canCancel("2027-05-01"))
    }

    /** US 01.07.01: a reservation that has already ended can't be cancelled */
    @Test
    fun finishedReservationCannotBeCancelled() {
        assertFalse(reservation.canCancel("2027-06-01"))
    }

    /** US 01.07.01: a reservation that is already cancelled can't be cancelled again */
    @Test
    fun cancelledReservationCannotBeCancelledAgain() {
        val cancelled = reservation.copy(status = "cancelled")
        assertFalse(cancelled.canCancel("2027-05-01"))
    }

    /** US 01.07.02: the cancellation message names the campsite and the cancelled dates */
    @Test
    fun cancellationMessageNamesCampsiteAndDates() {
        val cancelled = reservation.copy(status = "cancelled")
        assertEquals(
            "Your reservation at Grande Cache Ridge for 2027-05-07, 2027-05-08 was cancelled.",
            cancelled.cancellationMessage(),
        )
    }

    /** US 01.07.02: the CSV of a cancelled reservation shows the cancelled status */
    @Test
    fun cancelledReservationCsvShowsCancelledStatus() {
        val lines = reservation.copy(status = "cancelled").toCsv().trim().lines()
        assertEquals("res-1,\"Grande Cache Ridge\",2027-05-07 2027-05-08,2,cancelled", lines[1])
    }
}
