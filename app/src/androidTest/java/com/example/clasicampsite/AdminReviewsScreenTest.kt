/**
 * Purpose: Intent tests for the admin campsite reviews moderation screen (#71).
 * Makes this emulator an admin, seeds a test review in Firestore, and verifies
 * the admin can remove policy-violating reviews.
 * Outstanding issues: None.
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests admin campsite review browsing and removal. */
@RunWith(AndroidJUnit4::class)
class AdminReviewsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val testCampsiteId = "admin_review_test_site"
    private val testCampsiteName = "Review Moderation Site"
    private val testReviewerId = "bad_reviewer_001"
    private val testComment = "Spam link violating policy"

    /** Makes this emulator an admin and creates a test campsite with a review. */
    @Before
    fun setUp() = runBlocking {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("campsites").document(testCampsiteId).set(
            mapOf(
                "name" to testCampsiteName,
                "location" to "Banff, AB",
                "rating" to 1.0,
            )
        ).await()

        firestore.collection("campsites").document(testCampsiteId)
            .collection("ratings").document(testReviewerId)
            .set(
                mapOf(
                    "userId" to testReviewerId,
                    "rating" to 1,
                    "comment" to testComment,
                )
            ).await()

        val deviceId = Settings.Secure.getString(
            composeRule.activity.contentResolver, Settings.Secure.ANDROID_ID
        )
        firestore.collection("users").document(deviceId)
            .set(mapOf("admin" to true), SetOptions.merge()).await()
        Unit
    }

    /** Deletes test review and campsite after each test. */
    @After
    fun tearDown() = runBlocking {
        val db = DatabaseController()
        db.deleteCampsiteReview(testCampsiteId, testReviewerId)
        db.deleteCampsite(testCampsiteId)
    }

    /**
     * Waits up to 10 seconds for [text] to appear, because Firestore takes time.
     *
     * @param text the text to wait for
     */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens the admin reviews screen from Home and waits for it to load. */
    private fun openAdminReviews() {
        composeRule.onNodeWithTag("profileButton").performClick()
        composeRule.onNodeWithTag("backButton").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("adminReviewsButton").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("adminReviewsButton").performClick()
        waitForText("All campsite reviews")
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("adminReviewList").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("adminReviewList").performScrollToNode(hasText(testCampsiteName))
    }

    /** US 03.10.01: admin sees campsite reviews in the moderation list. */
    @Test
    fun adminCanViewCampsiteReviews() {
        openAdminReviews()
        composeRule.onNodeWithText(testCampsiteName).assertIsDisplayed()
        composeRule.onNodeWithText("Comment: $testComment").assertIsDisplayed()
    }

    /** US 03.10.01: admin confirms removal and the review is deleted from Firestore. */
    @Test
    fun adminCanRemoveViolatingReview() {
        openAdminReviews()
        composeRule.onNodeWithTag("removeReviewButton_${testCampsiteId}_${testReviewerId}").performClick()
        composeRule.onNodeWithText("Remove review?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmRemoveReviewButton").performClick()

        waitForText("Review removed")
        composeRule.onNodeWithText("Comment: $testComment").assertDoesNotExist()

        val ratingAfter = runBlocking {
            DatabaseController().getUserRating(testCampsiteId, testReviewerId)
        }
        assertNull(ratingAfter)
    }
}