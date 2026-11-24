/*
 * File: OwnerCampsiteScreenTest.kt
 * Purpose: Intent tests for the owner's campsite screens: register with an optional map
 *          location (US 02.01.01, US 02.01.02), view and update (US 02.01.03, US 02.01.04),
 *          and My Campsites (US 02.03.01).
 * Outstanding issues: Uses the real Firestore database, so it needs internet. Each test
 *                     deletes this phone's campsites named in TEST_NAMES before and after
 *                     it runs, so don't use those names for real campsites. Picking a photo
 *                     isn't tested, because Android's photo picker is outside the app.
 * Parts of this file were written with help from Claude (Anthropic), 2026-11-03, updated 2026-11-24
 */
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clasicampsite.data.DatabaseController
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the owner's campsite screens by using the app. */
@RunWith(AndroidJUnit4::class)
class OwnerCampsiteScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val database = DatabaseController()

    /** Clears out campsites left behind by an earlier run, so the list starts short. */
    @Before
    fun setUp() {
        deleteTestCampsites()
    }

    /** Deletes the campsites this test made, so they don't pile up in Firestore. */
    @After
    fun tearDown() {
        deleteTestCampsites()
    }

    /**
     * Deletes this phone's campsites made by these tests: the names in [TEST_NAMES], plus
     * the "Test Site ..." and "Edit Site ..." ones that older versions of this file left behind.
     */
    private fun deleteTestCampsites() {
        val ownerId = Settings.Secure.getString(
            composeRule.activity.contentResolver, Settings.Secure.ANDROID_ID
        )
        runBlocking {
            // If cleanup fails (e.g. no internet), the tests themselves still decide pass/fail.
            runCatching {
                database.getCampsitesByOwner(ownerId)
                    .filter { site -> site.name in TEST_NAMES || OLD_PREFIXES.any { site.name.startsWith(it) } }
                    .forEach { database.deleteCampsite(it.id) }
            }
        }
    }

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

    /**
     * Waits up to 10 seconds for the campsite called [name] to load, and scrolls the
     * My Campsites list to it so it's on screen even when the owner has many campsites.
     */
    private fun scrollToCampsite(name: String) {
        composeRule.waitUntil(10_000) {
            try {
                composeRule.onNodeWithTag("myCampsitesList").performScrollToNode(hasText(name))
                true
            } catch (e: AssertionError) {
                false
            }
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

    /**
     * Fills in and submits the register form with a campsite called [name] at [location].
     * [latitude] and [longitude] are optional and left blank if not given.
     */
    private fun registerCampsite(name: String, location: String, latitude: String = "", longitude: String = "") {
        composeRule.onNodeWithTag("registerCampsiteButton").performClick()
        type("campsiteNameField", name)
        type("campsiteLocationField", location)
        type("campsiteDescriptionField", "Lakeside fire pits, picnic tables and a boat launch")
        type("campsiteOccupancyField", "6")
        type("campsitePriceField", "45")
        type("campsiteTagsField", "lake, fire pit, boating")
        if (latitude.isNotEmpty()) {
            type("campsiteLatitudeField", latitude)
            type("campsiteLongitudeField", longitude)
        }
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
        openMyCampsites()
        registerCampsite(REGISTER_NAME, "Pigeon Lake, AB")
        scrollToCampsite(REGISTER_NAME)
        composeRule.onNodeWithText(REGISTER_NAME).assertIsDisplayed()
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
        composeRule.onNodeWithText("Name is required").performScrollTo().assertIsDisplayed()
    }

    /** US 02.01.03, US 02.01.04: an owner can view their campsite, edit it and save. */
    @Test
    fun ownerCanViewAndUpdateCampsite() {
        openMyCampsites()
        registerCampsite(EDIT_NAME, "Sylvan Lake, AB")
        scrollToCampsite(EDIT_NAME)

        composeRule.onNodeWithText(EDIT_NAME).performClick()
        // The title shows right away, but the fields only appear once Firestore has loaded.
        waitForTag("editCampsiteButton")
        composeRule.onNodeWithTag("campsiteLocationField").assertTextContains("Sylvan Lake, AB")

        composeRule.onNodeWithTag("editCampsiteButton").performScrollTo().performClick()
        type("campsitePriceField", "55")
        composeRule.onNodeWithTag("saveCampsiteButton").performScrollTo().performClick()
        waitForText("Campsite updated")
    }
    /** US 02.01.02: a map location the owner types is saved and shown with the campsite. */
    @Test
    fun mapLocationIsSavedWithCampsite() {
        openMyCampsites()
        registerCampsite(MAP_NAME, "Waterton Park, AB", "49.05", "-113.91")
        scrollToCampsite(MAP_NAME)

        composeRule.onNodeWithText(MAP_NAME).performClick()
        waitForTag("editCampsiteButton")
        composeRule.onNodeWithTag("campsiteLatitudeField").performScrollTo().assertTextContains("49.05")
        composeRule.onNodeWithTag("campsiteLongitudeField").assertTextContains("-113.91")
        composeRule.onNodeWithTag("viewOnMapButton").performScrollTo().assertIsDisplayed()
    }
    private companion object {
        /** Realistic campsite names the tests register, then delete. */
        const val REGISTER_NAME = "Pigeon Lake Retreat"
        const val EDIT_NAME = "Sylvan Lake Pines"
        const val MAP_NAME = "Waterton Shoreline Camp"
        val TEST_NAMES = listOf(REGISTER_NAME, EDIT_NAME, MAP_NAME)

        /** Name starts used by older versions of these tests. */
        val OLD_PREFIXES = listOf("Test Site ", "Edit Site ")
    }
}