/**
 * Purpose: Intent tests for the owner's Sent Notifications screen (US 02.10.02), including
 * sending from Notify Waitlist (US 02.10.01) and then finding it in the list.
 * Outstanding issues: Uses the real Firestore database, so it needs internet. Before and
 * after each test it deletes this phone's campsite named CAMPSITE_NAME
 * and the notifications sent to the two test entrants.
 * Parts of this file were written with help from Claude (Anthropic), 2026-11-27
 */
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the Sent Notifications screen by using the app. */
@RunWith(AndroidJUnit4::class)
class SentNotificationsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val database = DatabaseController()
    private var campsiteId = ""

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
                location = "Bragg Creek, AB",
                description = "Forest tent sites next to the river",
                maxOccupancy = 6,
                pricePerNight = 35.0,
                ownerId = ownerId(),
            )
            database.saveCampsite(campsite)
            campsiteId = campsite.id
            database.joinLottery(campsiteId, ENTRANT_1)
        }
    }

    /** Deletes what the test made, so it doesn't pile up in Firestore. */
    @After
    fun tearDown() {
        runBlocking { cleanUp() }
    }

    /** Deletes this phone's test campsite (and its waitlist entry) and the test entrants' notifications. */
    private suspend fun cleanUp() {
        /** If cleanup fails (e.g. no internet), the tests themselves still decide pass/fail. */
        runCatching {
            database.getCampsitesByOwner(ownerId()).filter { it.name == CAMPSITE_NAME }.forEach {
                database.leaveLottery(it.id, ENTRANT_1)
                database.deleteCampsite(it.id)
            }
            for (entrant in listOf(ENTRANT_1, ENTRANT_2)) {
                FirebaseFirestore.getInstance().collection("notifications")
                    .whereEqualTo("userId", entrant).get().await()
                    .documents.forEach { it.reference.delete().await() }
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

    /** Waits up to 10 seconds for the list with [listTag] to load [text], scrolling to it. */
    private fun scrollTo(listTag: String, text: String) {
        composeRule.waitUntil(10_000) {
            try {
                composeRule.onNodeWithTag(listTag).performScrollToNode(hasText(text))
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    /** US 02.10.02: one send to two entrants shows as one item saying it went to 2 entrants. */
    @Test
    fun sentNotificationShowsInList() {
        val sentAt = System.currentTimeMillis()
        runBlocking {
            database.sendOwnerNotification(ENTRANT_1, EARLIER_MESSAGE, ownerId(), campsiteId, sentAt)
            database.sendOwnerNotification(ENTRANT_2, EARLIER_MESSAGE, ownerId(), campsiteId, sentAt)
        }

        composeRule.onNodeWithTag("myCampsitesButton").performClick()
        composeRule.onNodeWithTag("sentNotificationsButton").performClick()
        scrollTo("sentNotificationsList", EARLIER_MESSAGE)

        composeRule.onNode(
            hasTestTag("sentNotificationItem") and
                hasAnyDescendant(hasText(EARLIER_MESSAGE)) and
                hasAnyDescendant(hasText("Sent to 2 entrants", substring = true))
        ).assertIsDisplayed()
    }

    /** US 02.10.01 then US 02.10.02: after notifying the waitlist, the owner finds it in Sent Notifications. */
    @Test
    fun notifyWaitlistThenSeeItInSentNotifications() {
        composeRule.onNodeWithTag("myCampsitesButton").performClick()
        scrollTo("myCampsitesList", CAMPSITE_NAME)
        composeRule.onNodeWithText(CAMPSITE_NAME).performClick()
        waitForTag("notifyWaitlistButton")
        composeRule.onNodeWithTag("notifyWaitlistButton").performScrollTo().performClick()
        waitForText("1 entrant on the waitlist")

        composeRule.onNodeWithTag("waitlistMessageField").performTextInput(NEW_MESSAGE)
        composeRule.onNodeWithTag("sendWaitlistNotificationButton").performClick()
        waitForText("Sent to 1 entrant")

        composeRule.onNodeWithTag("viewSentNotificationsButton").performScrollTo().performClick()
        scrollTo("sentNotificationsList", "$CAMPSITE_NAME: $NEW_MESSAGE")
        composeRule.onNodeWithText("$CAMPSITE_NAME: $NEW_MESSAGE").assertIsDisplayed()
    }

    private companion object {
        /** Realistic test campsite, made and deleted by these tests. */
        const val CAMPSITE_NAME = "Bragg Creek Riverside"

        /** Pretend entrants' device IDs that only these tests use. */
        const val ENTRANT_1 = "intent-test-sent-entrant-1"
        const val ENTRANT_2 = "intent-test-sent-entrant-2"

        const val EARLIER_MESSAGE = "Bragg Creek Riverside: Firewood is free at the front gate."
        const val NEW_MESSAGE = "The river trail is closed for repairs this week."
    }
}
