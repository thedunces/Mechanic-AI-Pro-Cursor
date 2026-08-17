package com.mechanicai.pro.presentation.obd

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mechanicai.pro.data.model.DiagnosisResult
import com.mechanicai.pro.data.model.Severity
import com.mechanicai.pro.data.model.Vehicle
import com.mechanicai.pro.data.remote.obd.BluetoothObdManager
import com.mechanicai.pro.presentation.components.SafetyDisclaimer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScanScreen(
    onBack: () -> Unit,
    viewModel: BluetoothScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) {
            viewModel.loadPairedDevices()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasBluetoothConnectPermission()) {
            viewModel.loadPairedDevices()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth OBD Scan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            if (!viewModel.hasBluetoothConnectPermission()) {
                Text(
                    text = "Bluetooth permissions are required to connect to an OBD-II adapter.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = { permissionLauncher.launch(permissions) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permissions")
                }
                return@Column
            }

            if (uiState.result != null) {
                BluetoothDiagnosisResultContent(
                    result = uiState.result!!,
                    vehicle = uiState.selectedVehicle,
                    onReset = viewModel::clearResult
                )
            } else {
                SafetyDisclaimer()
                BluetoothScanContent(
                    uiState = uiState,
                    vehicles = vehicles,
                    connectionState = connectionState,
                    onConnect = viewModel::connect,
                    onDisconnect = viewModel::disconnect,
                    onReadCodes = viewModel::readCodes,
                    onClearCodes = viewModel::clearCodes,
                    onReadLiveData = viewModel::readLiveData,
                    onDiagnose = { vehicle ->
                        viewModel.diagnose(
                            vehicle,
                            LocalContext.current as? android.app.Activity
                        )
                    },
                    onSelectVehicle = viewModel::selectVehicle
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothScanContent(
    uiState: BluetoothScanViewModel.BluetoothScanUiState,
    vehicles: List<Vehicle>,
    connectionState: BluetoothObdManager.ConnectionState,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onReadCodes: () -> Unit,
    onClearCodes: () -> Unit,
    onReadLiveData: () -> Unit,
    onDiagnose: (Vehicle) -> Unit,
    onSelectVehicle: (Vehicle) -> Unit
) {
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var vehicleExpanded by remember { mutableStateOf(false) }

    Text(
        text = "Connect an ELM327 Bluetooth adapter that is already paired with this phone.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = when (connectionState) {
                    is BluetoothObdManager.ConnectionState.Connected -> "Connected to ${connectionState.deviceName}"
                    BluetoothObdManager.ConnectionState.Connecting -> "Connecting..."
                    BluetoothObdManager.ConnectionState.Disconnected -> "Not connected"
                    is BluetoothObdManager.ConnectionState.Error -> "Error: ${connectionState.message}"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    if (uiState.pairedDevices.isNotEmpty()) {
        Text(
            text = "Paired Devices",
            style = MaterialTheme.typography.titleLarge
        )
        uiState.pairedDevices.forEach { device ->
            Card(
                onClick = { selectedDevice = device },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedDevice == device) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Text(
                    text = device.name ?: device.address,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    } else {
        Text(
            text = "No paired Bluetooth devices found. Pair your ELM327 adapter in Android settings first.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { selectedDevice?.let { onConnect(it) } },
            modifier = Modifier.weight(1f),
            enabled = selectedDevice != null && connectionState !is BluetoothObdManager.ConnectionState.Connected
        ) {
            Text("Connect")
        }
        OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.weight(1f),
            enabled = connectionState is BluetoothObdManager.ConnectionState.Connected
        ) {
            Text("Disconnect")
        }
    }

    if (vehicles.isNotEmpty()) {
        Text(
            text = "Select Vehicle for Diagnosis",
            style = MaterialTheme.typography.titleLarge
        )
        ExposedDropdownMenuBox(
            expanded = vehicleExpanded,
            onExpandedChange = { vehicleExpanded = !vehicleExpanded }
        ) {
            OutlinedTextField(
                value = uiState.selectedVehicle?.displayName ?: "Select vehicle",
                onValueChange = {},
                readOnly = true,
                label = { Text("Vehicle") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = vehicleExpanded,
                onDismissRequest = { vehicleExpanded = false }
            ) {
                vehicles.forEach { vehicle ->
                    DropdownMenuItem(
                        text = { Text(vehicle.displayName) },
                        onClick = {
                            onSelectVehicle(vehicle)
                            vehicleExpanded = false
                        }
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onReadCodes,
            modifier = Modifier.weight(1f),
            enabled = connectionState is BluetoothObdManager.ConnectionState.Connected && !uiState.isLoading
        ) {
            Text("Read Codes")
        }
        OutlinedButton(
            onClick = onClearCodes,
            modifier = Modifier.weight(1f),
            enabled = connectionState is BluetoothObdManager.ConnectionState.Connected && !uiState.isLoading
        ) {
            Text("Clear Codes")
        }
    }

    Button(
        onClick = onReadLiveData,
        modifier = Modifier.fillMaxWidth(),
        enabled = connectionState is BluetoothObdManager.ConnectionState.Connected && !uiState.isLoading
    ) {
        Text("Read Live Data")
    }

    Button(
        onClick = { uiState.selectedVehicle?.let { onDiagnose(it) } },
        modifier = Modifier.fillMaxWidth(),
        enabled = connectionState is BluetoothObdManager.ConnectionState.Connected &&
            uiState.selectedVehicle != null && !uiState.isLoading
    ) {
        Text("Diagnose with AI")
    }

    if (uiState.isLoading) {
        CircularProgressIndicator()
    }

    uiState.errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    if (uiState.codes.isNotEmpty()) {
        ResultCard(title = "OBD Codes") {
            uiState.codes.forEach { Text("• $it") }
        }
    }

    if (uiState.liveData.isNotEmpty()) {
        ResultCard(title = "Live Data") {
            uiState.liveData.forEach { (name, value) ->
                Text("• $name: $value")
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun BluetoothDiagnosisResultContent(
    result: DiagnosisResult,
    vehicle: Vehicle?,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Diagnosis for ${vehicle?.displayName ?: "your vehicle"}",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                val color = when (result.severity) {
                    Severity.LOW -> MaterialTheme.colorScheme.secondary
                    Severity.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    Severity.HIGH -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    Severity.CRITICAL -> MaterialTheme.colorScheme.error
                    Severity.UNKNOWN -> MaterialTheme.colorScheme.outline
                }
                Card(colors = CardDefaults.cardColors(containerColor = color)) {
                    Text(
                        text = "Severity: ${result.severity.name}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        ResultCard(title = "Explanation") { Text(result.explanation) }
        ResultCard(title = "Likely Causes") { result.likelyCauses.forEach { Text("• $it") } }
        ResultCard(title = "Recommended Fixes") { result.recommendedFixes.forEachIndexed { i, fix -> Text("${i + 1}. $fix") } }
        ResultCard(title = "Parts & Tools") { result.partsNeeded.forEach { Text("• $it") } }
        ResultCard(title = "Safety Notes") { result.safetyNotes.forEach { Text("⚠ $it") } }
        ResultCard(title = "When to See a Mechanic") { Text(result.whenToSeeMechanic) }

        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Another Vehicle")
        }
    }
}

@Composable
private fun ResultCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
