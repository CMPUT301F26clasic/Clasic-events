/*
 * Purpose: Intent tests for the lottery management screen
 * (US 02.04.05, US 02.09.01).
 *
 * Outstanding issues: Uses the real Firestore database, so it needs internet
 * and Firestore rules that allow the required reads and writes.
 */

package com.example.clasicampsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Tests the lottery management screen by using the app. */
@RunWith(AndroidJUnit4::class)
class LotteryManagementScreenTest {

    @get:Rule
    val composeRule = createComposeRule()
    private val firestore = FirebaseFirestore.getInstance()

    // Unique campsite ID so tests do not interfere with existing data.
    private val testCampsiteId = "lottery_test_${UUID.randomUUID()}"

    /** Waits up to 10 seconds for [text] to appear, because Firestore takes time. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Creates a campsite document for this test. */
    @Before
    fun createTestCampsite(): Unit {
        runBlocking {
            firestore.collection("campsites")
                .document(testCampsiteId)
                .set(
                    mapOf(
                        "id" to testCampsiteId,
                        "name" to "Lottery Test Campsite",
                        "numberOfWinners" to 2
                    )
                ).await()
        }
    }

    /** Deletes the test campsite and any entrants/users created for it. */
    @After
    fun deleteTestData(): Unit {
        runBlocking {
            val entrants = firestore.collection("lottery_entrants")
                .whereEqualTo("campsiteId", testCampsiteId).get().await()

            // Delete entrant documents and their associated test users.
            entrants.documents.forEach { entrant ->
                val userId = entrant.getString("userId")

                entrant.reference.delete().await()

                if (!userId.isNullOrBlank()) {
                    firestore.collection("users").document(userId).delete().await()
                }
            }

            firestore.collection("campsites").document(testCampsiteId).delete().await()
        }
    }

    /** Opens the lottery management screen for the test campsite. */
    private fun openLotteryManagementScreen() {
        composeRule.setContent {
            val navController = rememberNavController()

            LotteryManagementScreen(navController = navController, campsiteId = testCampsiteId)
        }

        waitForText("Manage Lottery")
    }

    /** Replaces the contents of a text field with [text]. */
    private fun type(tag: String, text: String) {
        composeRule.onNodeWithTag(tag).performTextClearance()
        composeRule.onNodeWithTag(tag).performTextInput(text)
    }

    /** Adds a test user and lottery entrant to Firestore. */
    private fun addTestEntrant(name: String): Unit {
        runBlocking {
            val userId = "lottery_test_user_${UUID.randomUUID()}"
            val entrantId = "lottery_test_entrant_${UUID.randomUUID()}"

            // Create the user document.
            firestore.collection("users")
                .document(userId)
                .set(mapOf("id" to userId, "name" to name, "email" to "test@example.com")).await()

            // Associate the user with the test campsite's lottery.
            firestore.collection("lottery_entrants")
                .document(entrantId)
                .set(mapOf("id" to entrantId, "campsiteId" to testCampsiteId, "userId" to userId))
                .await()
        }
    }

    /** US 02.04.05, US 02.09.01: the lottery management screen can be opened. */
    @Test
    fun lotteryManagementScreenOpens() {
        openLotteryManagementScreen()

        composeRule.onNodeWithText("Lottery Test Campsite").assertIsDisplayed()
        composeRule.onNodeWithTag("winnerCountField").assertIsDisplayed()
        composeRule.onNodeWithTag("saveWinnerCountButton").assertIsDisplayed()
        composeRule.onNodeWithText("Lottery Waitlist").assertIsDisplayed()
    }

    /** US 02.04.05: the campsite owner can update the number of lottery winners. */
    @Test
    fun winnerCountCanBeUpdated() {
        openLotteryManagementScreen()

        type("winnerCountField", "4")
        composeRule.onNodeWithTag("saveWinnerCountButton").performClick()
        waitForText("Number of winners successfully updated!")

        // Verify that the new winner count was saved to Firestore.
        val savedCount = runBlocking {
            firestore.collection("campsites").document(testCampsiteId).get().await()
                .getLong("numberOfWinners")
        }

        assertEquals(4L, savedCount)
    }

    /** US 02.04.05: a zero winner count cannot be saved. */
    @Test
    fun zeroWinnerCountIsRejected() {
        openLotteryManagementScreen()

        type("winnerCountField", "0")
        composeRule.onNodeWithTag("saveWinnerCountButton").performClick()
        composeRule.onNodeWithText("Please enter a number of winners greater than zero.").assertIsDisplayed()
    }

    /** US 02.04.05: an empty winner count cannot be saved. */
    @Test
    fun emptyWinnerCountIsRejected() {
        openLotteryManagementScreen()

        type("winnerCountField", "")
        composeRule.onNodeWithTag("saveWinnerCountButton").performClick()
        composeRule.onNodeWithText("Please enter a number of winners greater than zero.").assertIsDisplayed()
    }

    /** US 02.09.01: an empty lottery waitlist displays an appropriate message. */
    @Test
    fun emptyWaitlistMessageIsDisplayed() {
        openLotteryManagementScreen()
        waitForText("There are currently no entrants on the waitlist.")
        composeRule.onNodeWithText("There are currently no entrants on the waitlist.").assertIsDisplayed()
    }

    /** US 02.09.01: the campsite owner can view the names of lottery entrants. */
    @Test
    fun lotteryEntrantsAreDisplayed() {
        addTestEntrant("Lottery Test Entrant One")
        addTestEntrant("Lottery Test Entrant Two")

        openLotteryManagementScreen()

        waitForText("Lottery Test Entrant One")
        waitForText("Lottery Test Entrant Two")

        composeRule.onNodeWithText("Lottery Test Entrant One", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Lottery Test Entrant Two", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("2 entrants").assertIsDisplayed()
    }
}