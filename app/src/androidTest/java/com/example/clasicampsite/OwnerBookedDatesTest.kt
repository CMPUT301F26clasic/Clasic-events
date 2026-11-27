/**
 * Purpose: Tests for viewing currently booked dates on a campsite owned
 * by the current owner (#25).
 * Outstanding issues: None right now.
 */
package com.example.clasicampsite

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import com.example.clasicampsite.model.Reservation
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Tests showing an owner the booked dates of their campsite (US 02.06.01). */
class OwnerBookedDatesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** US 02.06.01: cancelled reservations don't count as booked dates */
    @Test
    fun bookedDatesHelper_excludesCancelledReservations() {
        val reservations = listOf(
            Reservation(
                id = "r1",
                campsiteId = "c1",
                dates = listOf("2027-06-10", "2027-06-11"),
                status = "active",
            ),
            Reservation(
                id = "r2",
                campsiteId = "c1",
                dates = listOf("2027-06-15"),
                status = "cancelled",
            ),
        )

        val activeDates = Reservation.bookedDates(reservations)
        assert("2027-06-10" in activeDates)
        assert("2027-06-11" in activeDates)
        assert("2027-06-15" !in activeDates)
    }

    /** US 02.06.01: the owner's bookings screen opens */
    @Test
    fun ownerScheduleScreen_showsBookedDatesSection() {
        val sampleCampsiteId = LocalDate.now().toString()
        composeTestRule.setContent {
            val navController = rememberNavController()
            OwnerCampsiteScheduleScreen(
                navController = navController,
                campsiteId = sampleCampsiteId,
            )
        }

        composeTestRule.onNodeWithTag("ownerScheduleTitle").assertExists()
    }
}