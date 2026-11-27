/**
 * Purpose: The admin screen that lists campsite reviews/ratings and lets an admin
 * remove reviews that violate app policy after confirming (#71).
 * Only admins can use it; everyone else sees "Admins only" (US 03.11.01).
 * Outstanding issues: None.
 */
package com.example.clasicampsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.clasicampsite.model.CampsiteReview

/** The admin's list of campsite reviews that can be moderated for policy violations. */
@Composable
fun AdminReviewsScreen(navController: NavController) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        allowed = AdminAccess.isAdmin(context)
    }
    if (allowed != true) {
        AdminOnlyMessage(navController, checking = allowed == null)
        return
    }

    val viewModel: AdminReviewsViewModel = viewModel()
    var reviewToRemove by remember { mutableStateOf<CampsiteReview?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("backButton"),
        ) {
            Text("Back")
        }
        Text("All campsite reviews", fontSize = 24.sp, modifier = Modifier.testTag("adminReviewsTitle"))
        if (viewModel.message.isNotEmpty()) {
            Text(viewModel.message)
        }

        if (!viewModel.loaded) {
            Text("Loading...")
        } else if (viewModel.reviews.isEmpty()) {
            Text("No reviews yet.", modifier = Modifier.testTag("noReviewsText"))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("adminReviewList"),
            ) {
                items(
                    viewModel.reviews,
                    key = { "${it.campsiteId}_${it.userId}" },
                ) { review ->
                    Card(modifier = Modifier.fillMaxWidth().testTag("reviewCard_${review.campsiteId}_${review.userId}")) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = review.campsiteName.ifBlank { review.campsiteId },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("Rating: ${review.rating} / 5")
                            if (review.comment.isNotBlank()) {
                                Text("Comment: ${review.comment}")
                            }
                            Text("Reviewer ID: ${review.userId}")
                            Button(
                                onClick = { reviewToRemove = review },
                                modifier = Modifier.testTag("removeReviewButton_${review.campsiteId}_${review.userId}"),
                            ) {
                                Text("Remove review")
                            }
                        }
                    }
                }
            }
        }
    }

    val target = reviewToRemove
    if (target != null) {
        AlertDialog(
            onDismissRequest = { reviewToRemove = null },
            title = { Text("Remove review?") },
            text = { Text("This deletes the review for violating app policy and updates the campsite's rating.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeReview(target)
                        reviewToRemove = null
                    },
                    modifier = Modifier.testTag("confirmRemoveReviewButton"),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewToRemove = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}