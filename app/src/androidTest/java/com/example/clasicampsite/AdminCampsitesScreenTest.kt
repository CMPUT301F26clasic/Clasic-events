/**
 * Purpose: Intent tests for the admin campsites screen (US 03.04.01: admin browses every campsite).
 * The tests make this emulator an admin first, since only admins see the admin buttons (US 03.11.01).
 */
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clasicampsite.data.DatabaseController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests admin campsites screen by using app. */
@RunWith(AndroidJUnit4::class)
class AdminCampsitesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val testId = "admin_test_campsite"
    private val testName = "Admin Test Campsite"

    /** Saves a known campsite in Firestore and makes this emulator an admin. */
    @Before
    fun addTestCampsite() = runBlocking {
        FirebaseFirestore.getInstance().collection("campsites").document(testId).set(
            mapOf(
                "name" to testName,
                "location" to "Test Location, AB",
                "maxOccupancy" to 6,
                "rating" to 4.5,
                "availableFrom" to "2027-05-01",
                "availableTo" to "2027-09-30",
            )
        ).await()
        val deviceId = Settings.Secure.getString(
            composeRule.activity.contentResolver, Settings.Secure.ANDROID_ID
        )
        FirebaseFirestore.getInstance().collection("users").document(deviceId)
            .set(mapOf("admin" to true), SetOptions.merge()).await()
        Unit
    }

    /** Removes the test campsite */
    @After
    fun removeTestCampsite() = runBlocking {
        DatabaseController().deleteCampsite(testId)
    }

    /** Wait 10s because Firestore takes time. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens the admin campsites screen from Home and waits for the list to load. */
    private fun openAdminCampsites() {
        /** Leave Home and come back so it checks admin again, then wait for the admin button. */
        composeRule.onNodeWithTag("profileButton").performClick()
        composeRule.onNodeWithTag("backButton").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("adminCampsitesButton").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("adminCampsitesButton").performClick()
        waitForText("All campsites")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("adminCampsiteList").fetchSemanticsNodes().isNotEmpty()
        }
        /** The list is lazy, so scroll to the test campsite in case there are many. */
        composeRule.onNodeWithTag("adminCampsiteList").performScrollToNode(hasText(testName))
    }

    /** US 03.04.01: Home button opens admin campsites screen. */
    @Test
    fun homeButtonOpensAdminCampsites() {
        openAdminCampsites()
        composeRule.onNodeWithText("All campsites").assertIsDisplayed()
    }

    /** US 03.04.01: The list shows a saved campsite with its details. */
    @Test
    fun browseShowsSavedCampsite() {
        openAdminCampsites()
        val card = hasText(testName)
        composeRule.onNode(card).assertExists()
        composeRule.onNode(card and hasText("Location: Test Location, AB")).assertExists()
        composeRule.onNode(card and hasText("Max occupancy: 6")).assertExists()
        composeRule.onNode(card and hasText("Rating: 4.5")).assertExists()
        composeRule.onNode(card and hasText("Available: 2027-05-01 to 2027-09-30")).assertExists()
    }

    /** US 03.04.01: Back button to return Home. */
    @Test
    fun backButtonReturnsHome() {
        openAdminCampsites()
        composeRule.onNodeWithTag("backButton").performClick()
        waitForText("clasiCampsite Home")
        composeRule.onNodeWithText("clasiCampsite Home").assertIsDisplayed()
    }

    /** US 03.04.01: tapping a campsite opens its details. May fail until getCampsite() reads dates safely. */
    @Test
    fun tappingCampsiteOpensDetails() {
        openAdminCampsites()
        composeRule.onNodeWithText(testName).performClick()
        waitForText("Campsite Details")
        composeRule.onNodeWithTag("campsiteName").assertIsDisplayed()
    }
}