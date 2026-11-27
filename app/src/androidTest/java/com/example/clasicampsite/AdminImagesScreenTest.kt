/**
 * Purpose: Intent tests for the admin images screen (US 03.06.01, US 03.03.01).
 * Saves a test user with a picture first and deletes them after. Makes this emulator an admin.
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
import com.example.clasicampsite.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the admin images screen by using the app. */
@RunWith(AndroidJUnit4::class)
class AdminImagesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val testId = "admin_test_image_user"
    private val testName = "Image Test User"

    /** A tiny 4x4 green picture, stored as Base64 text the same way the app stores pictures */
    private val testImage =
        "iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAIAAAAmkwkpAAAAEElEQVR4nGPQ6w6HIwbiOAC3sxEBvcZG2wAAAABJRU5ErkJggg=="

    /** Saves a test user with a picture, and makes this emulator an admin. */
    @Before
    fun addTestUser() = runBlocking {
        val database = FirebaseFirestore.getInstance()
        database.collection("users").document(testId).set(
            User(id = testId, name = testName, email = "image.test@ualberta.ca", profileImage = testImage)
        ).await()
        val deviceId = Settings.Secure.getString(
            composeRule.activity.contentResolver, Settings.Secure.ANDROID_ID
        )
        database.collection("users").document(deviceId)
            .set(mapOf("admin" to true), SetOptions.merge()).await()
        Unit
    }

    /** Deletes the test user. */
    @After
    fun removeTestUser() = runBlocking {
        DatabaseController().deleteUser(testId)
    }

    /** Waits up to 10 seconds for [text] to appear, because Firestore takes time. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens the admin images screen from Home and scrolls to the test user's picture. */
    private fun openAdminImages() {
        /** Leave Home and come back so it checks admin again */
        composeRule.onNodeWithTag("profileButton").performClick()
        composeRule.onNodeWithTag("backButton").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("adminImagesButton").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("adminImagesButton").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("imageList").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("imageList").performScrollToNode(hasText("Uploaded by: $testName"))
    }

    /** US 03.06.01: the admin sees uploaded pictures and who uploaded them. */
    @Test
    fun browseShowsUploadedImage() {
        openAdminImages()
        composeRule.onNodeWithText("Uploaded by: $testName").assertIsDisplayed()
        composeRule.onNodeWithTag("image_$testId").assertIsDisplayed()
    }

    /** US 03.03.01: removing asks first, then removes the picture but keeps the profile. */
    @Test
    fun removeImageAsksFirstThenRemovesIt() {
        openAdminImages()
        composeRule.onNodeWithTag("removeImageButton_$testId").performClick()
        composeRule.onNodeWithText("Remove image?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmRemoveImageButton").performClick()
        waitForText("Image removed")
        composeRule.onNodeWithText("Uploaded by: $testName").assertDoesNotExist()

        val user = runBlocking { DatabaseController().getUser(testId) }
        assertEquals("", user.profileImage)
        /** the profile itself is still there */
        assertEquals(testName, user.name)
    }
}