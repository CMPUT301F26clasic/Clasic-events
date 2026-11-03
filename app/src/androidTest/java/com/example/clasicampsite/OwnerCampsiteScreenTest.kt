/*
 * File: OwnerCampsiteScreenTest.kt
 * Purpose: Intent tests for the owner's campsite screens: register (US 02.01.01, US 02.01.02),
 *          view and update (US 02.01.03, US 02.01.04), and My Campsites (US 02.03.01).
 * Outstanding issues: Uses the real Firestore database, so it needs internet, and the
 *                     campsites it registers are left in the database.
 */
package com.example.clasicampsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the owner's campsite screens by using the app. */
@RunWith(AndroidJUnit4::class)
class OwnerCampsiteScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Waits up to 10 seconds for [text] to appear, because Firestore takes a moment. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits up to 10 seconds for the node with [tag] to appear. */
    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Replaces the text in the field with [tag]. */
    private fun type(tag: String, text: String) {
        composeRule.onNodeWithTag(tag).performScrollTo().performTextClearance()
        composeRule.onNodeWithTag(tag).performTextInput(text)
    }

    /** Opens My Campsites from Home. */
    private fun openMyCampsites() {
        composeRule.onNodeWithTag("myCampsitesButton").performClick()
        composeRule.onNodeWithTag("myCampsitesTitle").assertIsDisplayed()
    }

    /** Fills in and submits the register form with a campsite called [name]. */
    private fun registerCampsite(name: String) {
        composeRule.onNodeWithTag("registerCampsiteButton").performClick()
        type("campsiteNameField", name)
        type("campsiteLocationField", "Hinton, AB")
        type("campsiteDescriptionField", "Outdoor cookhouse and tennis court")
        type("campsiteOccupancyField", "8")
        type("campsitePriceField", "65")
        type("campsiteTagsField", "tennis, cookhouse")
        composeRule.onNodeWithTag("saveCampsiteButton").performScrollTo().performClick()
        // Saving goes back to My Campsites; wait for it before checking the list.
        waitForTag("myCampsitesTitle")
        waitForTag("myCampsiteItem")
    }

    /** US 02.03.01: My Campsites opens from Home and has a register button. */
    @Test
    fun myCampsitesShowsRegisterButton() {
        openMyCampsites()
        composeRule.onNodeWithTag("registerCampsiteButton").assertIsDisplayed()
    }

    /** US 02.01.01, US 02.01.02, US 02.03.01: a registered campsite appears in My Campsites. */
    @Test
    fun registeredCampsiteAppearsInMyCampsites() {
        val name = "Test Site ${System.currentTimeMillis()}"
        openMyCampsites()
        registerCampsite(name)
        waitForText(name)
        composeRule.onNodeWithText(name).assertIsDisplayed()
    }

    /** US 02.01.02: a campsite without a name is not saved. */
    @Test
    fun blankNameShowsError() {
        openMyCampsites()
        composeRule.onNodeWithTag("registerCampsiteButton").performClick()
        type("campsiteLocationField", "Hinton, AB")
        type("campsiteOccupancyField", "8")
        type("campsitePriceField", "65")
        composeRule.onNodeWithTag("saveCampsiteButton").performScrollTo().performClick()
        composeRule.onNodeWithText("Name is required").assertIsDisplayed()
    }

    /** US 02.01.03, US 02.01.04: an owner can view their campsite, edit it and save. */
    @Test
    fun ownerCanViewAndUpdateCampsite() {
        val name = "Edit Site ${System.currentTimeMillis()}"
        openMyCampsites()
        registerCampsite(name)
        waitForText(name)

        composeRule.onNodeWithText(name).performClick()
        waitForText("Campsite Details")
        composeRule.onNodeWithText("Hinton, AB").assertIsDisplayed()

        composeRule.onNodeWithTag("editCampsiteButton").performScrollTo().performClick()
        type("campsitePriceField", "80")
        composeRule.onNodeWithTag("saveCampsiteButton").performScrollTo().performClick()
        waitForText("Campsite updated")
    }
}