package com.mechanicai.pro.presentation.subscription

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as? Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "${state.remaining} of ${state.monthlyLimit} AI diagnoses remaining this month",
                style = MaterialTheme.typography.titleLarge,
            )

            PlanCard(
                name = "Free",
                detail = "3 AI diagnoses per calendar month",
                selected = !state.isPro,
            )
            PlanCard(
                name = "Pro",
                detail = "100 AI diagnoses per calendar month" +
                    (state.price?.let { " • $it/month" } ?: ""),
                selected = state.isPro,
            )

            if (state.requiresAccountUpgrade && !state.isPro) {
                Text(
                    text = "Before subscribing, open Settings and link an email account. " +
                        "This protects your subscription and lets you restore it on another device.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (state.isLoading || state.purchaseInProgress) {
                CircularProgressIndicator()
            }

            if (!state.isPro) {
                Button(
                    onClick = { activity?.let(viewModel::subscribe) },
                    enabled = activity != null &&
                        !state.requiresAccountUpgrade &&
                        !state.purchaseInProgress &&
                        state.price != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(state.price?.let { "Upgrade to Pro — $it/month" } ?: "Loading Play offer…")
                }
            } else {
                Text(
                    text = "Your Pro subscription is active.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            OutlinedButton(
                onClick = viewModel::restorePurchases,
                enabled = !state.purchaseInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore purchases")
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Text(
                text = "Subscriptions renew automatically through Google Play until canceled. " +
                    "Manage or cancel your plan in Google Play. Usage resets each calendar month (UTC).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanCard(name: String, detail: String, selected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
            if (selected) Text("Current", color = MaterialTheme.colorScheme.primary)
        }
    }
}
