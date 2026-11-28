/**
 * Purpose: Displays details for a single campsite and lets users join/leave its lottery,
 * submit a 1-5 star rating, write a review (US 01.13.02) and message the owner (US 01.14.01).
 * Opening a campsite whose lottery deadline has passed draws its winners (US 02.05.01).
 * Outstanding issues: The automatic draw only happens when someone opens the campsite.
 */
package com.example.clasicampsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.provider.Settings
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.Review
import androidx.compose.material3.OutlinedTextField
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * Shows details, lottery join/leave controls, a star rating picker, written reviews and a
 * button to message the owner for [campsiteId].
 *
 * @param navController used to go back or open Manage Lottery
 * @param campsiteId the Firestore ID of the campsite, or "" for a built-in sample
 * @param dbController where the campsite, lottery, ratings and reviews are loaded from and saved
 */
@Composable
fun CampsiteDetailScreen(
    navController: NavController,
    campsiteId: String = "",
    dbController: DatabaseController = remember { DatabaseController() }
) {
    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    /** keya adding variables for already in lottery list */
    var alreadyinLottery by remember { mutableStateOf(false) }
    var updateLottery by remember { mutableStateOf(false) }
    /** shown under the join button, so a failed join doesn't hide the campsite */
    var lotteryError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    var userrating by remember { mutableStateOf<Int?>(null) }
    var showRating by remember { mutableStateOf<Double?>(null) }
    var rating by remember { mutableStateOf(false) }
    var ratingError by remember { mutableStateOf<String?>(null) }
    /** written reviews (US 01.13.02) */
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var reviewText by remember { mutableStateOf("") }
    var postingReview by remember { mutableStateOf(false) }
    var reviewError by remember { mutableStateOf<String?>(null) }
    /** message to the owner (US 01.14.01) */
    var showMessageDialog by remember { mutableStateOf(false) }
    var sendingMessage by remember { mutableStateOf(false) }
    var messageError by remember { mutableStateOf<String?>(null) }
    var messageSent by remember { mutableStateOf(false) }
    /** winners of this campsite's lottery once it has been drawn, by the owner or automatically */
    var drawnWinners by remember { mutableStateOf<List<String>?>(null) }

    /** Fetch details from Firestore when campsiteId changes */
    LaunchedEffect(campsiteId) {
        if (campsiteId.isNotBlank()) {
            try {
                isLoading = true
                /** draws the winners first if the deadline has passed; a failure here shouldn't hide the campsite */
                runCatching { dbController.resolveLotteryIfDue(campsiteId) }
                val fetched = dbController.getCampsite(campsiteId)
                if (fetched != null) {
                    campsite = fetched
                    showRating = fetched.rating
                    alreadyinLottery =
                        userId != null && dbController.isUserInLottery(campsiteId, userId)
                    userrating = dbController.getUserRating(campsiteId, userId)
                    val drawn = dbController.getLottery(campsiteId)
                    drawnWinners = when {
                        drawn?.hasBeenDrawn() == true -> drawn.winners
                        fetched.lotteryResolved -> fetched.winners
                        else -> null
                    }
                    reviews = runCatching { dbController.getReviews(campsiteId) }.getOrDefault(emptyList())
                    reviewText = reviews.firstOrNull { it.userId == userId }?.text ?: ""
                } else {
                    errorMessage = "Campsite not found."
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load campsite: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        } else {
            /** Sample fallback data if no ID provided for preview/testing */
            campsite = Campsite(
                id = "sample_1",
                name = "Pine Ridge Campsite",
                location = "Banff National Park, AB",
                maxOccupancy = 6,
                rating = 4.8,
                availableFrom = LocalDate.of(2027, 5, 1),
                availableTo = LocalDate.of(2027, 9, 30)
            )

            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton")
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading campsite details...")
            }
        } else if (errorMessage != null) {
            Text(
                text = errorMessage ?: "An error occurred",
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp
            )
        } else campsite?.let { site ->
            Text(
                text = "Campsite Details",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("campsiteDetailsTitle")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            val onToggle: () -> Unit = {
                if (userId != null) {
                    scope.launch {
                        updateLottery = true
                        lotteryError = null
                        try {
                            if (alreadyinLottery) {
                                dbController.leaveLottery(site.id, userId)
                                alreadyinLottery = false
                            } else {
                                dbController.joinLottery(site.id, userId)
                                alreadyinLottery = true
                            }
                        } catch (e: Exception) {
                            lotteryError = "Lottery update failed: ${e.localizedMessage}"
                        } finally {
                            updateLottery = false
                        }
                    }
                }
            }

            val winners = drawnWinners
            val deadlinePassed = site.lotteryDeadline?.let { LocalDate.now().isAfter(it) } ?: false
            val isOwner = site.ownerId.isNotBlank() && site.ownerId == userId

            if (winners != null || deadlinePassed) {
                Text(
                    text = when {
                        winners != null && userId in winners -> "You won this lottery! Open Reservations to pick your dates."
                        winners != null && alreadyinLottery -> "You weren't selected this time."
                        else -> "Registration for this lottery is closed."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("lotteryResultText")
                )
            } else if (isOwner) {
                Text(
                    text = "This is your campsite, so you can't join its lottery.",
                    modifier = Modifier.testTag("ownCampsiteText")
                )
            } else if (alreadyinLottery) {
                OutlinedButton(
                    onClick = onToggle,
                    enabled = !updateLottery && userId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("leaveLotteryButton")
                ) {
                    Text(if (updateLottery) "Leaving..." else "Leave Lottery")
                }
            } else {
                Button(
                    onClick = onToggle,
                    enabled = !updateLottery && userId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("joinLotteryButton")
                ) {
                    Text(if (updateLottery) "Joining..." else "Join Lottery")
                }
            }

            lotteryError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("lotteryErrorText")
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campsiteCard"),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    /** 1. Name */
                    Column {
                        Text(
                            text = "Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = site.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("campsiteName")
                        )
                    }

                    /** 2. Location */
                    Column {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = site.location,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag("campsiteLocation")
                        )
                    }

                    /**
                     * 3. Max Occupancy & 4. Rating Row
                     * 3. Max Occupancy & 4. Rating Row
                     */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Max Occupancy",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${site.maxOccupancy} people",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.testTag("campsiteMaxOccupancy")
                            )
                        }

                        Column {
                            Text(
                                text = "Rating",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "★ ${showRating ?: site.rating} / 5.0",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("campsiteRating")
                            )
                        }
                    }

                    /** Manage Lottery button, only for the campsite's owner */
                    if (site.ownerId.isNotBlank() && site.ownerId == userId) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                navController.navigate("manage_lottery/${site.id}")
                            },
                            modifier = Modifier.testTag("manageLotteryButton")
                        ) {
                            Text("Manage Lottery")
                        }
                    }
                }
            }

            /** Star picker, below the card */
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (userrating == null) "Rate this campsite" else "Your rating",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (star in 1..5) {
                    Text(
                        text = if (star <= (userrating ?: 0)) "★" else "☆",
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .testTag("star$star")
                            .clickable(enabled = !rating) {
                                scope.launch {
                                    rating = true
                                    ratingError = null
                                    try {
                                        showRating = dbController.rateCampsite(site.id, userId, star)
                                        userrating = star
                                    } catch (e: Exception) {
                                        ratingError = "Rating failed: ${e.localizedMessage}"
                                    } finally {
                                        rating = false
                                    }
                                }
                            }
                    )
                }
            }

            ratingError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            if (campsiteId.isNotBlank()) {
                /** Message the owner (US 01.14.01), not shown to the owner themselves */
                if (site.ownerId.isNotBlank() && !isOwner) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showMessageDialog = true; messageError = null; messageSent = false },
                        modifier = Modifier.fillMaxWidth().testTag("messageOwnerButton")
                    ) {
                        Text("Message the owner")
                    }
                    if (messageSent) {
                        Text(
                            "Message sent!",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("messageSentText")
                        )
                    }
                }

                /** Written reviews (US 01.13.02) */
                Spacer(modifier = Modifier.height(24.dp))
                Text("Reviews", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { if (it.length <= 1000) reviewText = it },
                    label = { Text("Write a review") },
                    supportingText = { Text("${reviewText.length}/1000") },
                    modifier = Modifier.fillMaxWidth().testTag("reviewField")
                )
                Button(
                    enabled = !postingReview && reviewText.isNotBlank() && userId != null,
                    onClick = {
                        scope.launch {
                            postingReview = true
                            reviewError = null
                            try {
                                val name = dbController.getUser(userId).name.ifBlank { "Anonymous" }
                                dbController.postReview(site.id, userId, name, reviewText)
                                reviews = dbController.getReviews(site.id)
                            } catch (e: Exception) {
                                reviewError = e.localizedMessage
                            } finally {
                                postingReview = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("postReviewButton")
                ) {
                    Text(if (postingReview) "Posting..." else "Post review")
                }

                reviewError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (reviews.isEmpty()) {
                    Text("No reviews yet.", modifier = Modifier.testTag("noReviewsText"))
                } else {
                    reviews.forEachIndexed { index, review ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("review_$index")
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    review.userName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(review.text)
                            }
                        }
                    }
                }
            }

            if (showMessageDialog) {
                MessageReserverDialog(
                    recipientName = "the owner",
                    isSending = sendingMessage,
                    error = messageError,
                    onDismiss = { showMessageDialog = false },
                    onSend = { text ->
                        scope.launch {
                            sendingMessage = true
                            messageError = null
                            try {
                                dbController.sendMessageToOwner(userId, site.id, text)
                                showMessageDialog = false
                                messageSent = true
                            } catch (e: Exception) {
                                messageError = e.localizedMessage
                            } finally {
                                sendingMessage = false
                            }
                        }
                    }
                )
            }
        }
    }
}

