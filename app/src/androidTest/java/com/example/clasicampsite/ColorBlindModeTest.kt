/*
Purpose: Intent tests for color blind mode. The Profile switch turns it on right
away, and the setting is saved with the profile.
Outstanding issues: Changes this emulator's saved profile, then turns color blind mode back off.
 */
package com.example.clasicampsite

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clasicampsite.data.DatabaseController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests colour blind mode by using the app. */
@RunWith(AndroidJUnit4::class)
class ColorBlindModeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** This emulator's device ID, the same one the app saves profiles under. */
    private fun deviceId(): String = AdminAccess.deviceId(composeRule.activity)

    /** Sets this emulator's saved colour blind setting, keeping its other fields. */
    private fun setSaved(value: Boolean) = runBlocking {
        FirebaseFirestore.getInstance().collection("users").document(deviceId())
            .set(mapOf("colorBlindMode" to value), SetOptions.merge()).await()
        Unit
    }

    /** Waits up to 10 seconds for [text] to appear, because Firestore takes time. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens Profile from Home and waits for it to load. */
    private fun openProfile() {
        composeRule.onNodeWithTag("profileButton").performClick()
        waitForText("Update info")
    }

    /** Turns colour blind mode back off so other tests use the normal colours. */
    @After
    fun turnOff() {
        setSaved(false)
        ColorBlindMode.enabled = false
    }

    /** The switch turns colour blind mode on right away. */
    @Test
    fun switchTurnsColorBlindModeOn() {
        setSaved(false)
        openProfile()
        composeRule.onNodeWithTag("colorBlindSwitch").assertIsOff()
        composeRule.onNodeWithTag("colorBlindSwitch").performClick()
        composeRule.onNodeWithTag("colorBlindSwitch").assertIsOn()
        assertTrue(ColorBlindMode.enabled)
    }

    /** Update info saves the setting, and it is still on when Profile is opened again. */
    @Test
    fun settingIsSavedWithProfile() {
        setSaved(false)
        openProfile()
        composeRule.onNodeWithTag("nameField").performTextClearance()
        composeRule.onNodeWithTag("nameField").performTextInput("Colour Test")
        composeRule.onNodeWithTag("emailField").performTextClearance()
        composeRule.onNodeWithTag("emailField").performTextInput("colour.test@ualberta.ca")
        composeRule.onNodeWithTag("phoneField").performTextClearance()
        composeRule.onNodeWithTag("colorBlindSwitch").performClick()
        composeRule.onNodeWithTag("saveButton").performClick()
        waitForText("Profile saved")

        val user = runBlocking { DatabaseController().getUser(deviceId()) }
        assertTrue(user.colorBlindMode)

        composeRule.onNodeWithTag("backButton").performClick()
        openProfile()
        composeRule.onNodeWithTag("colorBlindSwitch").assertIsOn()
    }
}