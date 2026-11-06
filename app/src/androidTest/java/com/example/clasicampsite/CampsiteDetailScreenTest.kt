//Purpose: Intent tests for the campsite details screen: name, location, max occupancy, rating.
//Outstanding issues: Uses the real Firestore database, so it needs internet. It saves one
//test campsite and delete.

package com.example.clasicampsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clasicampsite.data.DatabaseController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the campsite details screen by using it. */
@RunWith(AndroidJUnit4::class)
class CampsiteDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testId = "details_test_campsite"

    /** Saves a known campsite so the tests have something to look at. */
    @Before
    fun addTestCampsite() = runBlocking {
        FirebaseFirestore.getInstance().collection("campsites").document(testId).set(
            mapOf(
                "name" to "Details Test Campsite",
                "location" to "Test Location, AB",
                "maxOccupancy" to 6,
                "rating" to 4.5,
                "availableFrom" to "2027-05-01",
                "availableTo" to "2027-09-30",
            )
        ).await()
        Unit
    }

    /** Removes the test campsite again. */
    @After
    fun removeTestCampsite() = runBlocking {
        DatabaseController().deleteCampsite(testId)
    }

    private fun openDetails(campsiteId: String) {
        composeRule.setContent {
            val navController = rememberNavController()
            CampsiteDetailScreen(navController, campsiteId = campsiteId)
        }
    }

    /** Waits up to 10 seconds for a node with [tag], because Firestore takes time. */
    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The screen shows the campsite's name, location, max occupancy and rating. */
    @Test
    fun showsSavedCampsiteDetails() {
        openDetails(testId)
        waitForTag("campsiteDetailsTitle")
        composeRule.onNodeWithTag("campsiteName").assertTextEquals("Details Test Campsite")
        composeRule.onNodeWithTag("campsiteLocation").assertTextEquals("Test Location, AB")
        composeRule.onNodeWithTag("campsiteMaxOccupancy").assertTextEquals("6 people")
        composeRule.onNodeWithTag("campsiteRating").assertTextEquals("★ 4.5 / 5.0")
    }

    /** Each detail is on screen, not just in the semantics tree. */
    @Test
    fun allFourDetailsAreDisplayed() {
        openDetails(testId)
        waitForTag("campsiteDetailsTitle")
        composeRule.onNodeWithTag("campsiteName").assertIsDisplayed()
        composeRule.onNodeWithTag("campsiteLocation").assertIsDisplayed()
        composeRule.onNodeWithTag("campsiteMaxOccupancy").assertIsDisplayed()
        composeRule.onNodeWithTag("campsiteRating").assertIsDisplayed()
    }

    /** A campsite that doesn't exist shows a message instead of crashing. */
    @Test
    fun missingCampsiteShowsNotFoundMessage() {
        openDetails("no-such-campsite")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Campsite not found.").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** With no ID, the screen shows its built-in sample campsite. */
    @Test
    fun noIdShowsSampleCampsite() {
        openDetails("")
        waitForTag("campsiteDetailsTitle")
        composeRule.onNodeWithTag("campsiteName").assertTextEquals("Pine Ridge Campsite")
        composeRule.onNodeWithTag("campsiteMaxOccupancy").assertTextEquals("6 people")
    }
}