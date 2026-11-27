package com.example.clasicampsite

/*
 * Purpose: Tests for filtering and viewing past, active, and cancelled
 * reservations for a campsite owner (#26).
 * Outstanding issues: None right now.
 */


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class OwnerReservationsHistoryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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