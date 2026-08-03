package com.mechanicai.pro.presentation.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mechanicai.pro.data.model.Vehicle
import com.mechanicai.pro.data.remote.nhtsa.NhtsaVinResult
import com.mechanicai.pro.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddVehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddVehicleUiState())
    val uiState: StateFlow<AddVehicleUiState> = _uiState.asStateFlow()

    fun updateVin(vin: String) {
        _uiState.value = _uiState.value.copy(vin = vin.uppercase())
    }

    fun updateMake(make: String) {
        _uiState.value = _uiState.value.copy(make = make)
    }

    fun updateModel(model: String) {
        _uiState.value = _uiState.value.copy(model = model)
    }

    fun updateYear(year: String) {
        _uiState.value = _uiState.value.copy(year = year.filter { it.isDigit() })
    }

    fun updateEngine(engine: String) {
        _uiState.value = _uiState.value.copy(engine = engine)
    }

    fun updateTrim(trim: String) {
        _uiState.value = _uiState.value.copy(trim = trim)
    }

    fun updateNickname(nickname: String) {
        _uiState.value = _uiState.value.copy(nickname = nickname)
    }

    fun decodeVin() {
        val vin = _uiState.value.vin
        if (vin.length != 17) {
            _uiState.value = _uiState.value.copy(errorMessage = "VIN must be 17 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = vehicleRepository.decodeVin(vin)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.fold(
                onSuccess = { decoded ->
                    _uiState.value = _uiState.value.copy(
                        make = decoded.Make ?: "",
                        model = decoded.Model ?: "",
                        year = decoded.ModelYear ?: "",
                        trim = decoded.Trim ?: "",
                        engine = listOfNotNull(
                            decoded.EngineModel,
                            decoded.DisplacementL?.let { "${it}L" },
                            decoded.Cylinders?.let { "$it cyl" }
                        ).joinToString(", "),
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to decode VIN"
                    )
                }
            )
        }
    }

    fun saveVehicle(onSuccess: () -> Unit) {
        val state = _uiState.value
        val yearInt = state.year.toIntOrNull()
        if (state.make.isBlank() || state.model.isBlank() || yearInt == null || yearInt < 1900) {
            _uiState.value = state.copy(errorMessage = "Make, model, and a valid year are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val vehicle = Vehicle(
                vin = state.vin,
                make = state.make,
                model = state.model,
                year = yearInt,
                trim = state.trim,
                engine = state.engine,
                nickname = state.nickname
            )
            val result = vehicleRepository.saveVehicle(vehicle)
            _uiState.value = state.copy(isLoading = false)
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    _uiState.value = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to save vehicle"
                    )
                }
            )
        }
    }

    data class AddVehicleUiState(
        val vin: String = "",
        val make: String = "",
        val model: String = "",
        val year: String = "",
        val trim: String = "",
        val engine: String = "",
        val nickname: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) {
        val isValid: Boolean
            get() = make.isNotBlank() && model.isNotBlank() && year.toIntOrNull() != null
    }
}
