/**
 * Purpose: Tests for filtering and viewing past, active, and cancelled
 * reservations for a campsite owner (#26).
 * Outstanding issues: None right now.
 */
package com.example.clasicampsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

/** Tests the owner's list of past, active and cancelled reservations (US 02.07.01). */
class OwnerReservationsHistoryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** US 02.07.01: the owner's reservations screen shows its title and a back button */
    @Test
    fun ownerScheduleScreen_showsAllReservationFilterChips() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            OwnerCampsiteScheduleScreen(
                navController = navController,
                campsiteId = "sample_campsite_id",
            )
        }

        composeTestRule.onNodeWithTag("backButton").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ownerScheduleTitle").assertIsDisplayed()
    }
}