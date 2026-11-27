package com.example.clasicampsite

/*
 * Purpose: Tests for viewing currently booked dates on a campsite owned
 * by the current owner (#25).
 * Outstanding issues: None right now.
 */

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import com.example.clasicampsite.model.Reservation
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class OwnerBookedDatesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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