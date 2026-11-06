//Purpose: Intent tests for the admin profiles screen (US 03.05.01, US 03.02.01).

package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the admin profiles screen by using the app. */
@RunWith(AndroidJUnit4::class)
class AdminProfilesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Waits up to 10 seconds for [text] to appear, because Firestore takes time. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** This emulator's device ID, the same one the app saves profiles under. */
    private fun deviceId(): String =
        Settings.Secure.getString(composeRule.activity.contentResolver, Settings.Secure.ANDROID_ID)

    /** Saves a profile called [name] through the Profile screen, then goes back Home. */
    private fun saveProfile(name: String) {
        composeRule.onNodeWithTag("profileButton").performClick()
        waitForText("Update info")
        composeRule.onNodeWithTag("nameField").performTextClearance()
        composeRule.onNodeWithTag("nameField").performTextInput(name)
        composeRule.onNodeWithTag("phoneField").performTextClearance()
        composeRule.onNodeWithTag("emailField").performTextClearance()
        composeRule.onNodeWithTag("emailField").performTextInput("admin.test@ualberta.ca")
        composeRule.onNodeWithTag("saveButton").performClick()
        waitForText("Profile saved")
        composeRule.onNodeWithTag("backButton").performClick()
    }

    /** US 03.05.01: the admin list shows saved profiles. */
    @Test
    fun browseShowsSavedProfile() {
        saveProfile("Admin Test User")
        composeRule.onNodeWithTag("adminProfilesButton").performClick()
        waitForText("Admin Test User")
        composeRule.onNodeWithText("Device ID: ${deviceId()}").assertIsDisplayed()
    }

    /** US 03.02.01: removing asks first, then takes the profile off the list. */
    @Test
    fun removeAsksFirstThenRemovesProfile() {
        saveProfile("Remove Me")
        composeRule.onNodeWithTag("adminProfilesButton").performClick()
        waitForText("Remove Me")
        composeRule.onNodeWithTag("removeButton_${deviceId()}").performClick()
        composeRule.onNodeWithText("Remove profile?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmRemoveButton").performClick()
        waitForText("Profile removed")
        composeRule.onNodeWithText("Remove Me").assertDoesNotExist()
    }
}