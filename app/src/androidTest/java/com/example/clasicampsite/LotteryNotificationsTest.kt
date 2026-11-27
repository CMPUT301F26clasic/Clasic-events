package com.example.clasicampsite

/*
 * Purpose: Unit and UI tests for sending and displaying campsite lottery
 * win and loss notifications (#5).
 * Outstanding issues: None right now.
 */

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import com.example.clasicampsite.data.DatabaseController
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class LotteryNotificationsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sendLotteryOutcomeNotifications_sendsWinAndLossMessages() = runBlocking {
        val db = DatabaseController()
        val testCampsiteName = "Test Pine Ridge Site"
        val winnerId = "test_user_winner_001"
        val loserId = "test_user_loser_002"

        db.sendLotteryOutcomeNotifications(
            campsiteName = testCampsiteName,
            entrantUserIds = listOf(winnerId, loserId),
            winnerUserIds = listOf(winnerId),
        )

        val winnerNotifs = db.getNotificationsForUser(winnerId)
        val loserNotifs = db.getNotificationsForUser(loserId)

        assert(winnerNotifs.any { it.message.contains("won", ignoreCase = true) && it.message.contains(testCampsiteName) })
        assert(loserNotifs.any { it.message.contains("not selected", ignoreCase = true) && it.message.contains(testCampsiteName) })
    }

    @Test
    fun notificationScreen_displaysBackButtonAndTitle() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            NotificationScreen(navController = navController, deviceId = "test_user_winner_001")
        }

        composeTestRule.onNodeWithTag("backButton").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notificationsTitle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }
}