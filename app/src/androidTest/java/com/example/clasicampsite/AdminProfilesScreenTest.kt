//Purpose: Intent tests for the admin profiles screen (US 03.05.01, US 03.02.01).
//The tests make this emulator an admin first, since only admins see the admin buttons (US 03.11.01).

package com.example.clasicampsite

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.example.clasicampsite.model.User
import org.junit.Assert.assertFalse

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

    /** Marks this emulator's user as an admin in Firestore, keeping its other fields. */
    private fun makeThisDeviceAdmin() = runBlocking {
        FirebaseFirestore.getInstance().collection("users").document(deviceId())
            .set(mapOf("admin" to true), SetOptions.merge()).await()
        Unit
    }

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

    /** Waits for Home to show the admin button, then opens the admin profiles screen. */
    private fun openAdminProfiles() {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("adminProfilesButton").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("adminProfilesButton").performClick()
    }

    /** US 03.05.01: the admin list shows saved profiles. */
    @Test
    fun browseShowsSavedProfile() {
        makeThisDeviceAdmin()
        saveProfile("Admin Test User")
        openAdminProfiles()
        waitForText("Admin Test User")
        // The list holds every profile, so scroll to this device's one first
        composeRule.onNodeWithTag("profileList").performScrollToNode(hasText("Device ID: ${deviceId()}"))
        composeRule.onNodeWithText("Device ID: ${deviceId()}").assertIsDisplayed()
    }

    /** US 03.02.01: removing asks first, then takes the profile off the list. */
    @Test
    fun removeAsksFirstThenRemovesProfile() {
        makeThisDeviceAdmin()
        saveProfile("Remove Me")
        openAdminProfiles()
        waitForText("Remove Me")
        composeRule.onNodeWithTag("profileList").performScrollToNode(hasTestTag("removeButton_${deviceId()}"))
        composeRule.onNodeWithTag("removeButton_${deviceId()}").performClick()
        composeRule.onNodeWithText("Remove profile?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmRemoveButton").performClick()
        waitForText("Profile removed")
        composeRule.onNodeWithText("Remove Me").assertDoesNotExist()
    }

    /** US 03.07.01: removing a campsite owner also removes their campsites. */
    @Test
    fun removeOwnerAlsoRemovesTheirCampsites() {
        makeThisDeviceAdmin()
        val database = FirebaseFirestore.getInstance()
        runBlocking {
            database.collection("users").document("admin_test_owner").set(
                User(id = "admin_test_owner", name = "Owner To Remove", email = "owner.test@ualberta.ca")
            ).await()
            database.collection("campsites").document("admin_test_owner_campsite").set(
                mapOf(
                    "name" to "Owner Test Campsite",
                    "ownerId" to "admin_test_owner",
                    "availableFrom" to "2027-05-01",
                    "availableTo" to "2027-09-30",
                )
            ).await()
        }
        // Leave Home and come back so it checks admin again
        composeRule.onNodeWithTag("profileButton").performClick()
        composeRule.onNodeWithTag("backButton").performClick()
        openAdminProfiles()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("profileList").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("profileList").performScrollToNode(hasTestTag("removeButton_admin_test_owner"))
        composeRule.onNodeWithTag("removeButton_admin_test_owner").performClick()
        composeRule.onNodeWithText(
            "This deletes this user's profile and the 1 campsite(s) they own. It can't be undone."
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("confirmRemoveButton").performClick()
        waitForText("Profile removed")

        val campsite = runBlocking {
            database.collection("campsites").document("admin_test_owner_campsite").get().await()
        }
        assertFalse(campsite.exists())
    }
}