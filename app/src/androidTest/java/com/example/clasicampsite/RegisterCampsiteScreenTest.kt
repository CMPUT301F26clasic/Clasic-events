// The following tests are from, ChatGPT, "Write me test cases for my register campsite screen", 2026-11-06
/*
 * Purpose: Intent tests for the campsite registration screen (US 02.01.01, US 02.01.02).
 * Outstanding issues: Uses the real Firestore database, so it needs internet.
 */

package com.example.clasicampsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the campsite registration screen by using the app. */
@RunWith(AndroidJUnit4::class)
class RegisterCampsiteScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Waits up to 10 seconds for [text] to appear. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /** Opens the campsite registration screen from Home. */
    private fun openRegistrationScreen() {
        composeRule.onNodeWithTag("registerCampsiteButton")
            .performClick()

        waitForText("Register a Campground")
    }

    /** US 02.01.01: the campsite registration screen can be opened. */
    /** US 02.01.02: the registration form provides fields for campsite details. */
    @Test
    fun registrationFormDisplaysCampsiteDetailsFields() {
        openRegistrationScreen()

        composeRule.onNodeWithTag("nameField")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("locationField")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("occupancyField")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("registerButton")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("availableFromField")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("availableToField")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("numberOfWinnersField")
            .assertIsDisplayed()

    }
}

