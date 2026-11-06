// Purpose: Unit tests for Reservation: nights and booked dates.

package com.example.clasicampsite

import com.example.clasicampsite.model.Reservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationTest {

    private val reservation = Reservation(
        id = "res-1",
        lotteryId = "test-lottery",
        campsiteId = "hinton-1",
        campsiteName = "Grande Cache Ridge",
        userId = "device-a",
        dates = listOf("2027-05-08", "2027-05-07"),
    )

    @Test
    fun nightsIsTheNumberOfDates() {
        assertEquals(2, reservation.nights())
    }

    @Test
    fun newReservationIsActive() {
        assertEquals("active", Reservation().status)
    }

    @Test
    fun bookedDatesComeFromActiveReservations() {
        val other = Reservation(id = "res-2", dates = listOf("2027-05-14"))
        assertEquals(
            listOf("2027-05-08", "2027-05-07", "2027-05-14"),
            Reservation.bookedDates(listOf(reservation, other)),
        )
    }

    @Test
    fun cancelledReservationsDoNotBookDates() {
        val cancelled = Reservation(id = "res-3", dates = listOf("2027-05-21"), status = "cancelled")
        assertEquals(listOf("2027-05-08", "2027-05-07"), Reservation.bookedDates(listOf(reservation, cancelled)))
    }

    @Test
    fun noReservationsMeansNoBookedDates() {
        assertTrue(Reservation.bookedDates(emptyList()).isEmpty())
    }
}
