package com.mechanicai.pro.presentation.obd

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mechanicai.pro.data.model.DiagnosisResult
import com.mechanicai.pro.data.model.DiagnosticInputs
import com.mechanicai.pro.data.model.LiveDataParameter
import com.mechanicai.pro.data.model.Vehicle
import com.mechanicai.pro.data.remote.obd.BluetoothObdManager
import com.mechanicai.pro.data.repository.DiagnosisRepository
import com.mechanicai.pro.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothScanViewModel @Inject constructor(
    private val obdManager: BluetoothObdManager,
    private val vehicleRepository: VehicleRepository,
    private val diagnosisRepository: DiagnosisRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothScanUiState())
    val uiState: StateFlow<BluetoothScanUiState> = _uiState.asStateFlow()

    val connectionState = obdManager.connectionState

    val vehicles = vehicleRepository.observeVehicles()

    init {
        viewModelScope.launch {
            vehicleRepository.observeVehicles().collect { list ->
                if (_uiState.value.selectedVehicle == null && list.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(selectedVehicle = list.first())
                }
            }
        }
        loadPairedDevices()
    }

    fun loadPairedDevices() {
        if (!hasBluetoothConnectPermission()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Bluetooth connect permission is required"
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            pairedDevices = obdManager.getPairedDevices(),
            errorMessage = null
        )
    }

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = obdManager.connect(device)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message ?: "Failed to connect"
                )
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            obdManager.disconnect()
            _uiState.value = _uiState.value.copy(
                codes = emptyList(),
                liveData = emptyMap(),
                result = null
            )
        }
    }

    fun readCodes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = obdManager.readTroubleCodes()
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.fold(
                onSuccess = { codes ->
                    _uiState.value = _uiState.value.copy(codes = codes)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to read codes"
                    )
                }
            )
        }
    }

    fun clearCodes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = obdManager.clearTroubleCodes()
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        codes = emptyList(),
                        errorMessage = "Codes cleared successfully"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to clear codes"
                    )
                }
            )
        }
    }

    fun readLiveData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = obdManager.readLiveData()
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(liveData = data)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to read live data"
                    )
                }
            )
        }
    }

    fun diagnose(vehicle: Vehicle, onComplete: (DiagnosisResult) -> Unit) {
        val state = _uiState.value
        val inputs = DiagnosticInputs(
            obdCodes = state.codes,
            liveData = state.liveData.map { LiveDataParameter(it.key, it.value, "") },
            symptoms = "Read via Bluetooth OBD-II scan.",
            notes = ""
        )
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null, result = null)
            val result = diagnosisRepository.diagnose(vehicle, inputs)
            _uiState.value = state.copy(isLoading = false)
            result.fold(
                onSuccess = { diagnosis ->
                    val session = com.mechanicai.pro.data.model.DiagnosticSession(
                        vehicleId = vehicle.id,
                        vehicleName = vehicle.displayName,
                        inputs = inputs,
                        result = diagnosis
                    )
                    diagnosisRepository.saveSession(session)
                    _uiState.value = state.copy(result = diagnosis)
                    onComplete(diagnosis)
                },
                onFailure = { error ->
                    _uiState.value = state.copy(
                        errorMessage = error.message ?: "Diagnosis failed"
                    )
                }
            )
        }
    }

    fun selectVehicle(vehicle: Vehicle) {
        _uiState.value = _uiState.value.copy(selectedVehicle = vehicle)
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null)
    }

    fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

    data class BluetoothScanUiState(
        val pairedDevices: List<BluetoothDevice> = emptyList(),
        val selectedVehicle: Vehicle? = null,
        val codes: List<String> = emptyList(),
        val liveData: Map<String, String> = emptyMap(),
        val result: DiagnosisResult? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )
}
