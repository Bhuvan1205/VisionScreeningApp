package com.example.swasthyatech.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swasthyatech.data.Direction
import com.example.swasthyatech.data.Eye
import com.example.swasthyatech.data.EyeTestResult
import com.example.swasthyatech.data.ScreeningSession
import com.example.swasthyatech.engine.EngineState
import com.example.swasthyatech.engine.MeasurementEngine
import com.example.swasthyatech.engine.Staircase3Down1UpAlgorithm
import com.example.swasthyatech.engine.OptotypeDimensions
import com.example.swasthyatech.engine.VisualAcuityTestEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TestScreenState(
    val eye: Eye = Eye.RIGHT,
    val engineState: EngineState = EngineState.NOT_STARTED,
    val currentOrientation: Direction? = null,
    val dimensions: OptotypeDimensions? = null,
    val currentAcuityLogMar: Float? = null,
    val inputEnabled: Boolean = false,
    val resultLogMar: Float? = null,
    val resultFraction: String? = null
)

class TestViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TestScreenState())
    val uiState: StateFlow<TestScreenState> = _uiState.asStateFlow()

    private var engine: VisualAcuityTestEngine? = null
    
    
    fun startTest(session: ScreeningSession, eye: Eye) {
        val config = session.testConfiguration
        
        val measurementEngine = MeasurementEngine(config.deviceCalibration.pixelsPerMm)
        val algorithm = Staircase3Down1UpAlgorithm()
        
        

        engine = VisualAcuityTestEngine(
            eye = eye,
            algorithm = algorithm,
            measurementEngine = measurementEngine,
            viewingDistanceMm = session.distanceValidation?.measuredDistanceMm ?: session.distanceValidation?.targetDistanceMm ?: config.viewingDistance.targetDistanceMm
        )
        
        engine?.start()
        syncState()
    }

    fun submitTouchResponse(direction: Direction, onTestComplete: (EyeTestResult) -> Unit) {
        val currentEngine = engine ?: return
        
        if (!_uiState.value.inputEnabled || currentEngine.state != EngineState.WAITING_FOR_RESPONSE) {
            return 
        }

        _uiState.value = _uiState.value.copy(inputEnabled = false)
        currentEngine.submitResponse(direction)
        
        viewModelScope.launch {
            delay(200)
            syncState()
            if (currentEngine.state == EngineState.COMPLETED || currentEngine.state == EngineState.ABORTED) {
                val res = currentEngine.getEyeTestResult()
                onTestComplete(res)
            }
        }
    }

    fun abortTest(onAbort: () -> Unit) {
        engine?.abort()
        onAbort()
    }

    private fun syncState() {
        val currentEngine = engine ?: return
        
        if (currentEngine.state == EngineState.COMPLETED || currentEngine.state == EngineState.ABORTED) {
            val result = currentEngine.getEyeTestResult()
            _uiState.value = _uiState.value.copy(
                engineState = currentEngine.state,
                inputEnabled = false,
                resultLogMar = result.calculatedAcuity,
                resultFraction = result.calculatedAcuityFraction
            )
        } else {
            _uiState.value = _uiState.value.copy(
                eye = currentEngine.getEyeTestResult().eye,
                engineState = currentEngine.state,
                currentOrientation = currentEngine.currentOrientation,
                dimensions = currentEngine.currentOptotypeDimensions,
                currentAcuityLogMar = currentEngine.currentAcuityLogMar,
                inputEnabled = currentEngine.state == EngineState.WAITING_FOR_RESPONSE
            )
        }
    }
}

