/*
 * Purpose: Intent tests for updating a campsite lottery listing and its optional waitlist capacity.
 * User stories:
 * US 02.04.06 As a campsite owner, I want to update my campsite lottery listing.
 * US 02.04.07 As a campsite owner, I want to optionally set a capacity for my lottery waitlist.
 *
 * Outstanding issues: Uses the real Firestore database, so it needs internet and Firestore
 * rules that allow the required reads and writes.
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
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Tests the campsite owner's lottery listing and waitlist capacity settings. */
@RunWith(AndroidJUnit4::class)
class LotteryCapacityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val firestore = FirebaseFirestore.getInstance()
    private val testCampsiteId = "capacity_test_${UUID.randomUUID()}"

    /** Waits up to 10 seconds for [text] to appear, because Firestore takes time. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Creates a campsite document for this test. */
    @Before
    fun createTestCampsite() = runBlocking {
        firestore.collection("campsites").document(testCampsiteId).set(
            mapOf(
                "id" to testCampsiteId,
                "name" to "Capacity Test Campsite",
                "ownerId" to "capacity_test_owner",
                "numberOfWinners" to 2,
                "availableFrom" to "2026-01-01",
                "availableTo" to "2026-12-31",
                "lotteryCapacity" to 5
            )
        ).await()
    }

    /** Deletes this test campsite and its nested lottery entries. */
    @After
    fun deleteTestData() = runBlocking {
        val entrants = firestore.collection("campsites")
            .document(testCampsiteId)
            .collection("lottery")
            .get()
            .await()

        entrants.documents.forEach { it.reference.delete().await() }
        firestore.collection("campsites").document(testCampsiteId).delete().await()
    }

    /** Opens the lottery management screen for the test campsite. */
    private fun openLotteryManagementScreen() {
        composeRule.setContent {
            val navController = rememberNavController()
            LotteryManagementScreen(navController = navController, campsiteId = testCampsiteId)
        }

        waitForText("Manage Lottery")
        waitForText("Capacity Test Campsite")
    }

    /** Replaces the contents of a text field with [text]. */
    private fun type(tag: String, text: String) {
        composeRule.onNodeWithTag(tag).performTextClearance()
        if (text.isNotEmpty()) {
            composeRule.onNodeWithTag(tag).performTextInput(text)
        }
    }

    /** Adds a nested lottery entry, matching the app's Firestore structure. */
    private fun addEntrant(userId: String) = runBlocking {
        firestore.collection("campsites")
            .document(testCampsiteId)
            .collection("lottery")
            .document(userId)
            .set(mapOf("userId" to userId))
            .await()
    }

    /** US 02.04.06: the campsite owner can update the lottery listing's winner count. */
    @Test
    fun ownerCanUpdateLotteryListingWinnerCount() {
        openLotteryManagementScreen()

        type("winnerCountField", "4")
        composeRule.onNodeWithTag("saveWinnerCountButton").performClick()
        waitForText("Number of winners successfully updated!")

        val savedCount = runBlocking {
            firestore.collection("campsites").document(testCampsiteId)
                .get().await().getLong("numberOfWinners")
        }
        assertEquals(4L, savedCount)
    }

    /** US 02.04.06, US 02.04.07: the owner can see and edit the lottery listing capacity. */
    @Test
    fun capacityControlsAreDisplayed() {
        openLotteryManagementScreen()

        composeRule.onNodeWithTag("lotteryCapacityField").assertIsDisplayed()
        composeRule.onNodeWithTag("saveLotteryCapacityButton").assertIsDisplayed()
        composeRule.onNodeWithText("Capacity: 5").assertIsDisplayed()
    }

    /** US 02.04.06, US 02.04.07: the owner can set a waitlist capacity. */
    @Test
    fun ownerCanSetWaitlistCapacity() {
        openLotteryManagementScreen()

        type("lotteryCapacityField", "8")
        composeRule.onNodeWithTag("saveLotteryCapacityButton").performClick()
        waitForText("Waitlist capacity updated to 8.")

        val savedCapacity = runBlocking {
            firestore.collection("campsites").document(testCampsiteId)
                .get().await().getLong("lotteryCapacity")
        }
        assertEquals(8L, savedCapacity)
    }

    /** US 02.04.07: leaving capacity blank removes the limit. */
    @Test
    fun blankCapacitySetsWaitlistToUnlimited() {
        openLotteryManagementScreen()

        type("lotteryCapacityField", "")
        composeRule.onNodeWithTag("saveLotteryCapacityButton").performClick()
        waitForText("Waitlist capacity removed; unlimited entrants allowed.")

        val savedCapacity = runBlocking {
            firestore.collection("campsites").document(testCampsiteId)
                .get().await().getLong("lotteryCapacity")
        }
        assertNull(savedCapacity)
    }

    /** US 02.04.07: a capacity of zero is rejected. */
    @Test
    fun zeroCapacityIsRejected() {
        openLotteryManagementScreen()

        type("lotteryCapacityField", "0")
        composeRule.onNodeWithTag("saveLotteryCapacityButton").performClick()
        composeRule.onNodeWithText(
            "Enter a capacity greater than zero, or leave it blank."
        ).assertIsDisplayed()

        val savedCapacity = runBlocking {
            firestore.collection("campsites").document(testCampsiteId)
                .get().await().getLong("lotteryCapacity")
        }
        assertEquals(5L, savedCapacity)
    }

    /** US 02.04.07: capacity cannot be lowered below the current waitlist size. */
    @Test
    fun capacityCannotBeLowerThanExistingEntrants() {
        addEntrant("capacity_test_user_1")
        addEntrant("capacity_test_user_2")

        openLotteryManagementScreen()
        type("lotteryCapacityField", "1")
        composeRule.onNodeWithTag("saveLotteryCapacityButton").performClick()
        waitForText("Capacity cannot be lower than the current waitlist size (2).")

        val savedCapacity = runBlocking {
            firestore.collection("campsites").document(testCampsiteId)
                .get().await().getLong("lotteryCapacity")
        }
        assertEquals(5L, savedCapacity)
    }
}
