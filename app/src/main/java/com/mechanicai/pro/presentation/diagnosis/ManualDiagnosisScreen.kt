package com.mechanicai.pro.presentation.diagnosis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mechanicai.pro.data.model.DiagnosisResult
import com.mechanicai.pro.data.model.LiveDataParameter
import com.mechanicai.pro.data.model.Severity
import com.mechanicai.pro.data.model.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualDiagnosisScreen(
    onBack: () -> Unit,
    viewModel: ManualDiagnosisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Diagnosis") },
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
            if (uiState.result != null) {
                DiagnosisResultContent(
                    result = uiState.result!!,
                    vehicle = uiState.selectedVehicle,
                    onReset = viewModel::clearResult
                )
            } else {
                DiagnosisInputContent(
                    vehicles = vehicles,
                    selectedVehicle = uiState.selectedVehicle,
                    onSelectVehicle = viewModel::selectVehicle,
                    obdCodes = uiState.obdCodes,
                    onAddCode = viewModel::addCode,
                    onRemoveCode = viewModel::removeCode,
                    liveData = uiState.liveData,
                    onAddLiveData = viewModel::addLiveDataParameter,
                    onRemoveLiveData = viewModel::removeLiveDataParameter,
                    symptoms = uiState.symptoms,
                    onSymptomsChange = viewModel::updateSymptoms,
                    notes = uiState.notes,
                    onNotesChange = viewModel::updateNotes,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    onDiagnose = { viewModel.diagnose {} },
                    onBack = onBack
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosisInputContent(
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    onSelectVehicle: (Vehicle) -> Unit,
    obdCodes: List<String>,
    onAddCode: (String) -> Unit,
    onRemoveCode: (String) -> Unit,
    liveData: List<LiveDataParameter>,
    onAddLiveData: (String, String, String) -> Unit,
    onRemoveLiveData: (String) -> Unit,
    symptoms: String,
    onSymptomsChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onDiagnose: () -> Unit,
    onBack: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Text(
        text = "Enter the information you have. The AI will use it to help diagnose the problem.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
    )

    if (vehicles.isNotEmpty()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedVehicle?.displayName ?: "Select a vehicle",
                onValueChange = {},
                readOnly = true,
                label = { Text("Vehicle") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                vehicles.forEach { vehicle ->
                    DropdownMenuItem(
                        text = { Text(vehicle.displayName) },
                        onClick = {
                            onSelectVehicle(vehicle)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    // OBD codes section
    Text(
        text = "OBD-II Codes",
        style = MaterialTheme.typography.titleLarge
    )
    var newCode by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = newCode,
            onValueChange = { newCode = it.uppercase() },
            label = { Text("e.g., P0301") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            singleLine = true
        )
        IconButton(
            onClick = {
                onAddCode(newCode)
                newCode = ""
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add code"
            )
        }
    }
    obdCodes.forEach { code ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(
                onClick = { onRemoveCode(code) }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove code",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Live data section
    Text(
        text = "Live Data Parameters",
        style = MaterialTheme.typography.titleLarge
    )
    var newParamName by remember { mutableStateOf("") }
    var newParamValue by remember { mutableStateOf("") }
    var newParamUnit by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = newParamName,
            onValueChange = { newParamName = it },
            label = { Text("Name") },
            modifier = Modifier.weight(1.2f),
            singleLine = true
        )
        OutlinedTextField(
            value = newParamValue,
            onValueChange = { newParamValue = it },
            label = { Text("Value") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = newParamUnit,
            onValueChange = { newParamUnit = it },
            label = { Text("Unit") },
            modifier = Modifier.weight(0.8f),
            singleLine = true
        )
        IconButton(
            onClick = {
                onAddLiveData(newParamName, newParamValue, newParamUnit)
                newParamName = ""
                newParamValue = ""
                newParamUnit = ""
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add parameter"
            )
        }
    }
    liveData.forEach { param ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${param.name}: ${param.value} ${param.unit}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(
                onClick = { onRemoveLiveData(param.name) }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove parameter",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    OutlinedTextField(
        value = symptoms,
        onValueChange = onSymptomsChange,
        label = { Text("Symptoms") },
        placeholder = { Text("What is the car doing? Noises, vibrations, smells, warning lights...") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6
    )

    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Additional Notes") },
        placeholder = { Text("Recent repairs, mileage, weather, anything else relevant") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4
    )

    errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    if (isLoading) {
        CircularProgressIndicator()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }
        Button(
            onClick = onDiagnose,
            modifier = Modifier.weight(1f),
            enabled = !isLoading && selectedVehicle != null
        ) {
            Text("Diagnose")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun DiagnosisResultContent(
    result: DiagnosisResult,
    vehicle: Vehicle?,
    onReset: () -> Unit
) {
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            SeverityChip(severity = result.severity)
        }
    }

    ResultSection(title = "Explanation") {
        Text(result.explanation)
    }

    ResultSection(title = "Likely Causes") {
        result.likelyCauses.forEach { Text("• $it") }
    }

    ResultSection(title = "Recommended Fixes") {
        result.recommendedFixes.forEachIndexed { index, fix ->
            Text("${index + 1}. $fix")
        }
    }

    ResultSection(title = "Parts & Tools Needed") {
        result.partsNeeded.forEach { Text("• $it") }
    }

    ResultSection(title = "Safety Notes") {
        result.safetyNotes.forEach { Text("⚠ $it") }
    }

    ResultSection(title = "When to See a Mechanic") {
        Text(result.whenToSeeMechanic)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onReset,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Start New Diagnosis")
    }
}

@Composable
private fun ResultSection(
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

@Composable
private fun SeverityChip(severity: Severity) {
    val color = when (severity) {
        Severity.LOW -> MaterialTheme.colorScheme.secondary
        Severity.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Severity.HIGH -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        Severity.CRITICAL -> MaterialTheme.colorScheme.error
        Severity.UNKNOWN -> MaterialTheme.colorScheme.outline
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Text(
            text = "Severity: ${severity.name}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
