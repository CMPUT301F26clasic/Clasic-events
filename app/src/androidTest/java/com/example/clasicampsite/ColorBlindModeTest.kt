/**
 * Purpose: Intent tests for color blind mode (WOW factor). Picking a mode from the Profile
 * dropdown changes it right away, and the mode is saved with the profile.
 * Outstanding issues: Changes this emulator's saved profile, then sets it back to regular.
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clasicampsite.data.DatabaseController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Colour blind mode (#76, WOW factor): tests color blind mode by using the app. */
@RunWith(AndroidJUnit4::class)
class ColorBlindModeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** This emulator's device ID, the same one the app saves profiles under. */
    private fun deviceId(): String = AdminAccess.deviceId(composeRule.activity)

    /** Sets this emulator's saved color blind mode, keeping its other fields. */
    private fun setSaved(mode: String) = runBlocking {
        FirebaseFirestore.getInstance().collection("users").document(deviceId())
            .set(mapOf("colorMode" to mode), SetOptions.merge()).await()
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

    /** Picks [mode] from the color blind dropdown. */
    private fun pick(mode: String) {
        composeRule.onNodeWithTag("colorModeButton").performClick()
        composeRule.onNodeWithTag("colorMode_$mode").performClick()
    }

    /** Sets the mode back to regular so other tests use the normal colors. */
    @After
    fun backToRegular() {
        setSaved("regular")
        ColorBlindMode.mode = "regular"
    }

    /** WOW factor: Profile starts on Regular, and every mode can be picked from the dropdown. */
    @Test
    fun eachModeCanBePicked() {
        setSaved("regular")
        openProfile()
        composeRule.onNodeWithText("Color blind mode: Regular").assertIsDisplayed()
        for ((mode, label) in ColorBlindMode.options) {
            pick(mode)
            assertEquals(mode, ColorBlindMode.mode)
            composeRule.onNodeWithText("Color blind mode: $label").assertIsDisplayed()
        }
    }

    /** WOW factor: Update info saves the mode, and it is still picked when Profile is opened again. */
    @Test
    fun modeIsSavedWithProfile() {
        setSaved("regular")
        openProfile()
        composeRule.onNodeWithTag("nameField").performTextClearance()
        composeRule.onNodeWithTag("nameField").performTextInput("Color Test")
        composeRule.onNodeWithTag("emailField").performTextClearance()
        composeRule.onNodeWithTag("emailField").performTextInput("color.test@ualberta.ca")
        composeRule.onNodeWithTag("phoneField").performTextClearance()
        pick("tritanopia")
        composeRule.onNodeWithTag("saveButton").performClick()
        waitForText("Profile saved")

        val user = runBlocking { DatabaseController().getUser(deviceId()) }
        assertEquals("tritanopia", user.colorMode)

        composeRule.onNodeWithTag("backButton").performClick()
        openProfile()
        composeRule.onNodeWithText("Color blind mode: Tritanopia (blue-blind)").assertIsDisplayed()
    }
}