package com.example.swasthyatech.ui.calibration

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.swasthyatech.calibration.CalibrationEngine
import com.example.swasthyatech.data.CalibrationRepository
import com.example.swasthyatech.data.DeviceCalibration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalibrationUiState(
    val currentPixels: Float = 300f, // Initial arbitrary box size
    val referencePhysicalMm: Float = 85.6f, // Credit card width
    val isSaved: Boolean = false,
    val existingCalibration: DeviceCalibration? = null
)

class CalibrationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalibrationRepository(application)
    private val engine = CalibrationEngine(physicalReferenceLengthMm = 85.6f)

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    init {
        loadExistingCalibration()
    }

    private fun loadExistingCalibration() {
        val existing = repository.getActiveCalibration()
        if (existing != null) {
            _uiState.value = _uiState.value.copy(
                existingCalibration = existing,
                // Roughly invert it for the slider to start near the saved value if we wanted to
                currentPixels = existing.pixelsPerMm * _uiState.value.referencePhysicalMm
            )
        }
    }

    fun updatePixels(pixels: Float) {
        _uiState.value = _uiState.value.copy(currentPixels = pixels, isSaved = false)
    }

    fun saveCalibration(screenDensity: Float, screenResolution: String) {
        val pixels = _uiState.value.currentPixels
        val calibration = engine.createDeviceCalibration(
            adjustedPixels = pixels,
            deviceManufacturer = Build.MANUFACTURER ?: "Unknown",
            deviceModel = Build.MODEL ?: "Unknown",
            screenResolution = screenResolution,
            screenDensity = screenDensity
        )

        val success = repository.saveCalibration(calibration)
        if (success) {
            _uiState.value = _uiState.value.copy(
                existingCalibration = calibration,
                isSaved = true
            )
        }
    }
}
