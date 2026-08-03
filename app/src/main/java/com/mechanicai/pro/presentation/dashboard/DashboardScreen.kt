package com.mechanicai.pro.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mechanicai.pro.presentation.components.ActionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToVehicles: () -> Unit,
    onNavigateToManualDiagnosis: () -> Unit,
    onNavigateToBluetoothScan: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val subscription by viewModel.subscription.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mechanic AI Pro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign out",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WelcomeCard(userDisplayName = user?.displayName ?: "Mechanic")

            Card(
                onClick = onNavigateToSubscription,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${subscription.tier.replaceFirstChar { it.uppercase() }} plan",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "${subscription.remaining} of ${subscription.monthlyLimit} AI diagnoses remaining",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            ActionCard(
                title = "New Manual Diagnosis",
                subtitle = "Enter OBD codes, live data, and symptoms",
                icon = Icons.Default.Build,
                onClick = onNavigateToManualDiagnosis
            )

            ActionCard(
                title = "Bluetooth OBD Scan",
                subtitle = "Connect an ELM327 adapter and scan your car",
                icon = Icons.Default.Bluetooth,
                onClick = onNavigateToBluetoothScan
            )

            ActionCard(
                title = "My Vehicles",
                subtitle = "Add or manage vehicles",
                icon = Icons.Default.Add,
                onClick = onNavigateToVehicles
            )

            ActionCard(
                title = "Diagnosis History",
                subtitle = "Review past diagnostic sessions",
                icon = Icons.Default.History,
                onClick = onNavigateToHistory
            )

            ActionCard(
                title = "Live Data Log",
                subtitle = "Browse recorded sensor readings",
                icon = Icons.AutoMirrored.Default.List,
                onClick = onNavigateToHistory
            )

            ActionCard(
                title = "Settings",
                subtitle = "Account linking, privacy, and production info",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WelcomeCard(userDisplayName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Welcome back, $userDisplayName",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select an option below to start diagnosing.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Start
            )
        }
    }
}
