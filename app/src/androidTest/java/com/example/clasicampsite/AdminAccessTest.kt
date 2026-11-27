/**
 * Purpose: Intent tests for admin access (US 03.11.01): only admins see the admin buttons on Home.
 * Outstanding issues: Changes this emulator's admin setting in Firestore, then sets it back to admin.
 */
package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests that Home only shows admin buttons to admins. */
@RunWith(AndroidJUnit4::class)
class AdminAccessTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Sets this emulator's user to admin or not in Firestore, keeping its other fields. */
    private fun setAdmin(value: Boolean) = runBlocking {
        val deviceId = Settings.Secure.getString(
            composeRule.activity.contentResolver, Settings.Secure.ANDROID_ID
        )
        FirebaseFirestore.getInstance().collection("users").document(deviceId)
            .set(mapOf("admin" to value), SetOptions.merge()).await()
        Unit
    }

    /** Leaves Home and comes back, so Home checks admin again. */
    private fun reopenHome() {
        composeRule.onNodeWithTag("profileButton").performClick()
        composeRule.onNodeWithTag("backButton").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("profileButton").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** US 03.11.01: an admin sees the admin buttons, along with all the normal ones. */
    @Test
    fun adminSeesAdminButtons() {
        setAdmin(true)
        reopenHome()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("adminProfilesButton").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("adminProfilesButton").assertIsDisplayed()
        composeRule.onNodeWithTag("adminCampsitesButton").assertIsDisplayed()
        composeRule.onNodeWithTag("adminImagesButton").assertIsDisplayed()
        composeRule.onNodeWithTag("myCampsitesButton").assertIsDisplayed()
        composeRule.onNodeWithTag("campsiteButton").assertIsDisplayed()
    }

    /** US 03.11.01: a regular user doesn't see the admin buttons. */
    @Test
    fun regularUserDoesNotSeeAdminButtons() {
        setAdmin(false)
        reopenHome()
        /** give Home time to finish checking with Firestore */
        Thread.sleep(3_000)
        composeRule.onNodeWithTag("adminProfilesButton").assertDoesNotExist()
        composeRule.onNodeWithTag("adminCampsitesButton").assertDoesNotExist()
        composeRule.onNodeWithTag("adminImagesButton").assertDoesNotExist()
        /** put this emulator back to admin for the other tests */
        setAdmin(true)
    }

    /** US 03.11.01: an admin who picks "View as: User" on Profile doesn't see the admin buttons. */
    @Test
    fun adminViewingAsUserDoesNotSeeAdminButtons() {
        setAdmin(true)
        composeRule.onNodeWithTag("profileButton").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("viewAsButton").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("viewAsButton").performClick()
        composeRule.onNodeWithTag("viewAsUserOption").performClick()
        composeRule.onNodeWithTag("backButton").performClick()
        /** give Home time to finish checking */
        Thread.sleep(3_000)
        composeRule.onNodeWithTag("adminProfilesButton").assertDoesNotExist()
        composeRule.onNodeWithTag("myCampsitesButton").assertIsDisplayed()
        /** back to admin for the other tests */
        AdminAccess.viewAsUser = false
    }
}