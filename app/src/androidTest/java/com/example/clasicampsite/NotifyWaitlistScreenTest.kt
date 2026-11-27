/**
 * Purpose: Intent tests for the owner notifying their campsite's waitlist (US 02.10.01).
 * Outstanding issues: Uses the real Firestore database, so it needs internet. Before and
 * after each test it deletes this phone's campsite named CAMPSITE_NAME
 * and the notifications sent to ENTRANT_ID.
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the Notify Waitlist screen by using the app. */
@RunWith(AndroidJUnit4::class)
class NotifyWaitlistScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val database = DatabaseController()

    /** This phone's ID, which the app uses as the owner ID. */
    private fun ownerId(): String =
        Settings.Secure.getString(composeRule.activity.contentResolver, Settings.Secure.ANDROID_ID)

    /** Makes a campsite owned by this phone with one entrant on its waitlist. */
    @Before
    fun setUp() {
        runBlocking {
            cleanUp()
            val campsite = Campsite(
                name = CAMPSITE_NAME,
                location = "Canmore, AB",
                description = "Creekside tent sites with fire pits",
                maxOccupancy = 4,
                pricePerNight = 40.0,
                ownerId = ownerId(),
            )
            database.saveCampsite(campsite)
            database.joinLottery(campsite.id, ENTRANT_ID)
        }
    }

    /** Deletes what the test made, so it doesn't pile up in Firestore. */
    @After
    fun tearDown() {
        runBlocking { cleanUp() }
    }

    /** Deletes this phone's test campsite (and its waitlist entry) and the test entrant's notifications. */
    private suspend fun cleanUp() {
        /** If cleanup fails (e.g. no internet), the tests themselves still decide pass/fail. */
        runCatching {
            database.getCampsitesByOwner(ownerId()).filter { it.name == CAMPSITE_NAME }.forEach {
                database.leaveLottery(it.id, ENTRANT_ID)
                database.deleteCampsite(it.id)
            }
            FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", ENTRANT_ID).get().await()
                .documents.forEach { it.reference.delete().await() }
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

    /** Goes Home -> My Campsites -> the test campsite -> Notify waitlist, and waits for it to load. */
    private fun openNotifyWaitlist() {
        composeRule.onNodeWithTag("myCampsitesButton").performClick()
        composeRule.waitUntil(10_000) {
            try {
                composeRule.onNodeWithTag("myCampsitesList").performScrollToNode(hasText(CAMPSITE_NAME))
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeRule.onNodeWithText(CAMPSITE_NAME).performClick()
        waitForTag("notifyWaitlistButton")
        composeRule.onNodeWithTag("notifyWaitlistButton").performScrollTo().performClick()
        waitForText("1 entrant on the waitlist")
    }

    /** US 02.10.01: the owner sends a message and the entrant on the waitlist gets it. */
    @Test
    fun ownerCanNotifyWaitlist() {
        openNotifyWaitlist()
        composeRule.onNodeWithTag("waitlistMessageField").performTextInput(MESSAGE)
        composeRule.onNodeWithTag("sendWaitlistNotificationButton").performClick()
        waitForText("Sent to 1 entrant")

        /** Check the entrant really got it in Firestore. */
        val received = runBlocking { database.getNotificationsForUser(ENTRANT_ID) }.map { it.message }
        assertTrue(received.contains("$CAMPSITE_NAME: $MESSAGE"))
    }

    /** US 02.10.01: an empty message is not sent. */
    @Test
    fun blankMessageShowsError() {
        openNotifyWaitlist()
        composeRule.onNodeWithTag("sendWaitlistNotificationButton").performClick()
        composeRule.onNodeWithText("Type a message first").performScrollTo().assertIsDisplayed()
    }

    private companion object {
        /** Realistic test campsite, made and deleted by these tests. */
        const val CAMPSITE_NAME = "Canmore Creekside Camp"

        /** A pretend entrant's device ID that only these tests use. */
        const val ENTRANT_ID = "intent-test-waitlist-entrant"

        const val MESSAGE = "Quiet hours start at 10 pm this weekend."
    }
}
