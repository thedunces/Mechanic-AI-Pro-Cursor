package com.mechanicai.pro.presentation.diagnosis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mechanicai.pro.data.model.DiagnosisResult
import com.mechanicai.pro.data.model.DiagnosticInputs
import com.mechanicai.pro.data.model.DiagnosticSession
import com.mechanicai.pro.data.model.LiveDataParameter
import com.mechanicai.pro.data.model.Vehicle
import com.mechanicai.pro.data.repository.DiagnosisRepository
import com.mechanicai.pro.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualDiagnosisViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val diagnosisRepository: DiagnosisRepository
) : ViewModel() {

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _uiState = MutableStateFlow(ManualDiagnosisUiState())
    val uiState: StateFlow<ManualDiagnosisUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            vehicleRepository.observeVehicles().collect { list ->
                _vehicles.value = list
                if (_uiState.value.selectedVehicle == null && list.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(selectedVehicle = list.first())
                }
            }
        }
    }

    fun selectVehicle(vehicle: Vehicle) {
        _uiState.value = _uiState.value.copy(selectedVehicle = vehicle)
    }

    fun updateSymptoms(value: String) {
        _uiState.value = _uiState.value.copy(symptoms = value)
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun addCode(code: String) {
        val trimmed = code.trim().uppercase()
        if (trimmed.isNotBlank() && trimmed !in _uiState.value.obdCodes) {
            _uiState.value = _uiState.value.copy(
                obdCodes = _uiState.value.obdCodes + trimmed
            )
        }
    }

    fun removeCode(code: String) {
        _uiState.value = _uiState.value.copy(
            obdCodes = _uiState.value.obdCodes - code
        )
    }

    fun addLiveDataParameter(name: String, value: String, unit: String) {
        val trimmedName = name.trim()
        if (trimmedName.isNotBlank()) {
            val updated = _uiState.value.liveData.filter { it.name != trimmedName } +
                LiveDataParameter(trimmedName, value.trim(), unit.trim())
            _uiState.value = _uiState.value.copy(liveData = updated)
        }
    }

    fun removeLiveDataParameter(name: String) {
        _uiState.value = _uiState.value.copy(
            liveData = _uiState.value.liveData.filter { it.name != name }
        )
    }

    fun diagnose(onComplete: (DiagnosisResult) -> Unit) {
        val vehicle = _uiState.value.selectedVehicle
        if (vehicle == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a vehicle first")
            return
        }

        val inputs = DiagnosticInputs(
            obdCodes = _uiState.value.obdCodes,
            liveData = _uiState.value.liveData,
            symptoms = _uiState.value.symptoms,
            notes = _uiState.value.notes
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, result = null)
            val result = diagnosisRepository.diagnose(vehicle, inputs)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.fold(
                onSuccess = { diagnosis ->
                    val session = DiagnosticSession(
                        vehicleId = vehicle.id,
                        vehicleName = vehicle.displayName,
                        inputs = inputs,
                        result = diagnosis
                    )
                    diagnosisRepository.saveSession(session)
                    _uiState.value = _uiState.value.copy(result = diagnosis)
                    onComplete(diagnosis)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Diagnosis failed"
                    )
                }
            )
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    data class ManualDiagnosisUiState(
        val selectedVehicle: Vehicle? = null,
        val obdCodes: List<String> = emptyList(),
        val liveData: List<LiveDataParameter> = emptyList(),
        val symptoms: String = "",
        val notes: String = "",
        val isLoading: Boolean = false,
        val result: DiagnosisResult? = null,
        val errorMessage: String? = null
    )
}
