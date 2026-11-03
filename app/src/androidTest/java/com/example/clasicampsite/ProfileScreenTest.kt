/*
 * File: ProfileScreenTest.kt
 * Purpose: Intent tests for the Profile and Policy screens (US 01.07.03, US 01.08.01,
 *          US 01.09.01, US 01.10.01, US 01.11.01).
 * Outstanding issues: Uses the real Firestore database, so it needs internet. The delete test
 *                     deletes the emulator's saved profile.
 */
package com.example.clasicampsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the Profile screen by using the app. */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Waits up to 10 seconds for [text] to appear, because Firestore takes a moment. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens Profile from the Home screen and waits for it to load. */
    private fun openProfile() {
        composeRule.onNodeWithTag("profileButton").performClick()
        waitForText("Update info")
    }

    /** Replaces the text in the field with [tag]. */
    private fun type(tag: String, text: String) {
        composeRule.onNodeWithTag(tag).performTextClearance()
        composeRule.onNodeWithTag(tag).performTextInput(text)
    }

    /** US 01.07.03, US 01.10.01: saving valid details. */
    @Test
    fun savingValidDetailsShowsProfileSaved() {
        openProfile()
        type("nameField", "Jordan Lee")
        type("emailField", "jordan.lee@ualberta.ca")
        type("phoneField", "780-555-0123")
        composeRule.onNodeWithTag("notificationsSwitch").performClick()
        composeRule.onNodeWithTag("saveButton").performClick()
        waitForText("Profile saved")
    }

    /** US 01.07.03: an invalid email is not saved. */
    @Test
    fun invalidEmailShowsError() {
        openProfile()
        type("nameField", "Jordan Lee")
        type("emailField", "jordan")
        composeRule.onNodeWithTag("saveButton").performClick()
        composeRule.onNodeWithText("Enter a valid email", substring = true).assertIsDisplayed()
    }

    /** US 01.07.03: a phone number without 10 digits is not saved. */
    @Test
    fun shortPhoneShowsError() {
        openProfile()
        type("nameField", "Jordan Lee")
        type("emailField", "jordan.lee@ualberta.ca")
        type("phoneField", "555-0123")
        composeRule.onNodeWithTag("saveButton").performClick()
        composeRule.onNodeWithText("Phone number must have 10 digits").assertIsDisplayed()
    }

    /** US 01.09.01: the screen shows the device ID. */
    @Test
    fun showsDeviceId() {
        openProfile()
        composeRule.onNodeWithText("Device ID:", substring = true).assertIsDisplayed()
    }

    /** US 01.11.01: the policy button opens the policy. */
    @Test
    fun policyButtonOpensPolicy() {
        openProfile()
        composeRule.onNodeWithTag("policyButton").performClick()
        composeRule.onNodeWithTag("policyTitle").assertIsDisplayed()
    }

    /** US 01.08.01: delete asks first, then goes back to Home. */
    @Test
    fun deleteAsksFirstThenGoesHome() {
        openProfile()
        composeRule.onNodeWithTag("deleteButton").performClick()
        composeRule.onNodeWithText("Delete profile?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmDeleteButton").performClick()
        waitForText("clasiCampsite Home")
    }
}