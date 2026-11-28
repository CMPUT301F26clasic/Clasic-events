package com.example.clasicampsite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clasicampsite.data.DatabaseController
import com.example.clasicampsite.model.Campsite
import com.example.clasicampsite.model.Review
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CampsiteDetailScreen(
    navController: NavController,
    campsiteId: String = "",
    dbController: DatabaseController = remember { DatabaseController() }
) {
    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    //keya adding variables for already in lottery list //
    var alreadyinLottery by remember { mutableStateOf(false) }
    var updateLottery by remember { mutableStateOf(false) }//true when joining/leaving lottery
    val scope = rememberCoroutineScope()// runs database calls from a click
    val context = LocalContext.current
    //making sure the phone id is with the unique id
    val userId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    //the variables for ating
    var userrating by remember { mutableStateOf<Int?>(null) } //this ones for rating by users
    var showRating by remember { mutableStateOf<Double?>(null) } //avergare rating shown
    var rating by remember { mutableStateOf(false) } //the rating with the stars
    var ratingError by remember { mutableStateOf<String?>(null) } //error message for rating
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var reviewText by remember { mutableStateOf("") }
    var postingReview by remember { mutableStateOf(false) }
    var reviewError by remember { mutableStateOf<String?>(null) }

    var showMessageDialog by remember { mutableStateOf(false) }
    var sendingMessage by remember { mutableStateOf(false) }
    var messageError by remember { mutableStateOf<String?>(null) }
    var messageSent by remember { mutableStateOf(false) }


    // Fetch details from Firestore when campsiteId changes
    // Fetch details from Firestore when campsiteId changes
    LaunchedEffect(campsiteId) {
        val effectiveId = campsiteId.ifBlank { "sample_1" }

        try {
            isLoading = true

            if (campsiteId.isNotBlank()) {
                dbController.resolveLotteryIfDue(campsiteId)   // draws if due
                val fetched = dbController.getCampsite(campsiteId)
                if (fetched != null) {
                    campsite = fetched
                    showRating = fetched.rating
                } else {
                    errorMessage = "Campsite not found."
                }
            } else {
                // Sample fallback data
                campsite = Campsite(
                    id = "sample_1",
                    name = "Pine Ridge Campsite",
                    location = "Banff National Park, AB",
                    maxOccupancy = 6,
                    rating = 4.8,
                    availableFrom = LocalDate.of(2027, 5, 1),
                    availableTo = LocalDate.of(2027, 9, 30)
                )
            }

            // Always load the user's saved state, whichever branch ran
            if (errorMessage == null && userId != null) {
                if (campsiteId.isNotBlank()) {
                    reviews = dbController.getReviews(campsiteId)
                    reviewText = reviews.firstOrNull { it.userId == userId }?.text ?: ""
                }
                alreadyinLottery = dbController.isUserInLottery(effectiveId, userId)
                userrating = dbController.getUserRating(effectiveId, userId)
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load campsite: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Campsite Details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("campsiteDetailsTitle")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(16.dp))

                //a function to join/leave waitlist
                val onToggle: () -> Unit = {
                    if (userId != null) {
                        //goe sto the launch cause the database takes time
                        scope.launch {
                            updateLottery = true
                            try {
                                if (alreadyinLottery) {
                                    //so if your already in the lottery then u can just do leave
                                    dbController.leaveLottery(site.id, userId)
                                    alreadyinLottery = false
                                } else {
                                    //if your not in then u can join
                                    dbController.joinLottery(site.id, userId)
                                    alreadyinLottery = true
                                }
                            } catch (e: Exception) {
                                errorMessage = "Lottery update failed: ${e.localizedMessage}"
                            } finally {
                                updateLottery = false //turn sthe button back on
                            }
                        }
                    }
                }
                //just building the buttons
                val isOpen = !site.lotteryResolved &&
                        (site.lotteryDeadline?.let { !LocalDate.now().isAfter(it) } ?: true)

                if (isOpen) {
                    if (alreadyinLottery) {
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
                } else {
                    val msg = when {
                        userId in site.winners -> "🎉 You won this lottery!"
                        alreadyinLottery -> "You weren't selected this time."
                        else -> "Registration is closed."
                    }
                    Text(
                        msg, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("lotteryResultText")
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
                        // 1. Name
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

                        // 2. Location
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

                        // 3. Max Occupancy & 4. Rating Row
                        // 3. Max Occupancy & 4. Rating Row
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
                                    //inputs the stars for teh rating
                                    text = "★ ${showRating ?: site.rating} / 5.0",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.testTag("campsiteRating")
                                )

                                // Manage Lottery Button
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
                }

                // Star picker, below the card
                Spacer(modifier = Modifier.height(16.dp))

                //the lael changing based on if they rated it or not
                Text(
                    text = if (userrating == null) "Rate this campsite" else "Your rating",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                //so this part turns the star into coloured stars when they are rating teh stars get filled in
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
                                        try {//saves rating makes a new average
                                            showRating =
                                                dbController.rateCampsite(site.id, userId, star)
                                            userrating = star //updates star
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
                    Spacer(Modifier.height(16.dp))

                    // Message the owner
                    OutlinedButton(
                        onClick = { showMessageDialog = true; messageError = null; messageSent = false },
                        modifier = Modifier.fillMaxWidth().testTag("messageOwnerButton")
                    ) { Text("Message the owner") }
                    if (messageSent) {
                        Text(
                            "Message sent!",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("messageSentText")
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Reviews
                    Text("Reviews", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { if (it.length <= 1000) reviewText = it },
                        label = { Text("Write a review") },
                        supportingText = { Text("${reviewText.length}/1000") },
                        modifier = Modifier.fillMaxWidth().testTag("reviewField")
                    )
                    Button(
                        enabled = !postingReview && reviewText.isNotBlank(),
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
                    ) { Text(if (postingReview) "Posting..." else "Post review") }

                    reviewError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    if (reviews.isEmpty()) {
                        Text("No reviews yet.", modifier = Modifier.testTag("noReviewsText"))
                    } else {
                        reviews.forEachIndexed { index, review ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
}


