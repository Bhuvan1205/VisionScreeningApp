package com.example.swasthyatech.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.swasthyatech.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    CALIBRATION,
    INTRO,
    DISTANCE_CHECK,
    PRACTICE,
    EYE_TRANSITION,
    TESTING,
    RESULT
}

data class SessionUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val activeSession: ScreeningSession? = null,
    val isCalibrationValid: Boolean = false,
    val activeCalibration: DeviceCalibration? = null,
    val currentTestingEye: Eye? = null,
    val distanceValidation: DistanceValidation? = null
)

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepo = SessionRepository(application)
    private val calibRepo = CalibrationRepository(application)

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    var distanceValidationEngine = com.example.swasthyatech.engine.DistanceValidationEngine(targetDistanceMm = 400f)
    
    
    init {
        checkCalibration()
    }

    fun checkCalibration() {
        val calibration = calibRepo.getActiveCalibration()
        _uiState.value = _uiState.value.copy(
            isCalibrationValid = calibration != null && calibration.isValid,
            activeCalibration = calibration
        )
    }

    fun navigateTo(screen: AppScreen) {
        if ((screen == AppScreen.TESTING || screen == AppScreen.INTRO) && !_uiState.value.isCalibrationValid) {
            _uiState.value = _uiState.value.copy(currentScreen = AppScreen.CALIBRATION)
        } else {
            _uiState.value = _uiState.value.copy(currentScreen = screen)
        }
    }

    fun startNewSession(participantId: String = "Anon-${UUID.randomUUID().toString().take(4)}") {
        val calibration = _uiState.value.activeCalibration ?: return
        
        val acuityLevels = listOf(1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0.0f, -0.1f)
        val minAcuity = acuityLevels.minOrNull() ?: -0.1f
        val engine = com.example.swasthyatech.engine.MeasurementEngine(calibration.pixelsPerMm)
        val minDistance = engine.calculateMinimumViewingDistance(minAcuity)
        val negotiatedDistanceMm = kotlin.math.max(400f, kotlin.math.ceil(minDistance / 10f) * 10f)
        distanceValidationEngine = com.example.swasthyatech.engine.DistanceValidationEngine(targetDistanceMm = negotiatedDistanceMm)
        val config = TestConfiguration(
            optotypeType = OptotypeType.TUMBLING_E,
            supportedOrientations = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT),
            acuityLevels = acuityLevels,
            testAlgorithmVersion = "STAIRCASE_3_DOWN_1_UP_V1",
            viewingDistance = ViewingDistance(targetDistanceMm = negotiatedDistanceMm),
            deviceCalibration = calibration
        )

        val session = ScreeningSession(
            sessionId = UUID.randomUUID().toString(),
            appVersionName = com.example.swasthyatech.BuildConfig.VERSION_NAME,
            appVersionCode = com.example.swasthyatech.BuildConfig.VERSION_CODE,
            participantId = participantId,
            timestamp = System.currentTimeMillis(),
            testConfiguration = config,
            rightEyeTest = null,
            leftEyeTest = null,
            finalResult = null,
            sessionStatus = TestStatus.NOT_STARTED
        )
        
        viewModelScope.launch { sessionRepo.saveSession(session) }
        
        _uiState.value = _uiState.value.copy(
            activeSession = session,
            currentTestingEye = Eye.RIGHT,
            currentScreen = AppScreen.INTRO
        )
    }

    fun saveEyeResult(eyeResult: EyeTestResult) {
        val currentSession = _uiState.value.activeSession ?: return
        
        val updatedSession = if (eyeResult.eye == Eye.RIGHT) {
            currentSession.copy(rightEyeTest = eyeResult, sessionStatus = TestStatus.IN_PROGRESS)
        } else {
            currentSession.copy(leftEyeTest = eyeResult)
        }
        
        viewModelScope.launch { sessionRepo.saveSession(updatedSession) }
        
        if (eyeResult.eye == Eye.RIGHT) {
            // Move to left eye transition screen
            _uiState.value = _uiState.value.copy(
                activeSession = updatedSession,
                currentTestingEye = Eye.LEFT,
                currentScreen = AppScreen.EYE_TRANSITION
            )
        } else {
            // Left eye completed, finish session
            val finalRes = FinalResult(
                rightEyeAcuityLogMar = updatedSession.rightEyeTest?.calculatedAcuity,
                leftEyeAcuityLogMar = updatedSession.leftEyeTest?.calculatedAcuity,
                overallStatus = TestStatus.COMPLETED,
                resultAlgorithmVersion = "V1",
                clinicalInterpretation = null
            )
            val finalizedSession = updatedSession.copy(
                finalResult = finalRes, 
                sessionStatus = TestStatus.COMPLETED
            )
            viewModelScope.launch { sessionRepo.saveSession(finalizedSession) }
            
            _uiState.value = _uiState.value.copy(
                activeSession = finalizedSession,
                currentTestingEye = null,
                currentScreen = AppScreen.RESULT
            )
        }
    }
    
    fun abortSession() {
        val currentSession = _uiState.value.activeSession
        if (currentSession != null) {
            val finalRes = FinalResult(
                rightEyeAcuityLogMar = currentSession.rightEyeTest?.calculatedAcuity,
                leftEyeAcuityLogMar = currentSession.leftEyeTest?.calculatedAcuity,
                overallStatus = TestStatus.INTERRUPTED,
                resultAlgorithmVersion = "V1",
                clinicalInterpretation = null
            )
            val aborted = currentSession.copy(
                sessionStatus = TestStatus.INTERRUPTED,
                finalResult = finalRes
            )
            viewModelScope.launch { sessionRepo.saveSession(aborted) }
            _uiState.value = _uiState.value.copy(
                activeSession = aborted,
                currentTestingEye = null,
                currentScreen = AppScreen.RESULT
            )
        } else {
            _uiState.value = _uiState.value.copy(
                activeSession = null,
                currentTestingEye = null,
                currentScreen = AppScreen.HOME
            )
        }
    }
        fun completeDistanceValidation() {
        if (distanceValidationEngine.validationState.status != com.example.swasthyatech.data.ValidationStatus.VALIDATED) {
            distanceValidationEngine.completeManualGuidance()
        }
        val currentSession = _uiState.value.activeSession
        if (currentSession != null) {
            val updatedSession = currentSession.copy(distanceValidation = distanceValidationEngine.validationState)
        viewModelScope.launch { sessionRepo.saveSession(updatedSession) }
            _uiState.value = _uiState.value.copy(
                distanceValidation = distanceValidationEngine.validationState,
                activeSession = updatedSession
            )
            if (updatedSession.rightEyeTest == null) {
                navigateTo(AppScreen.PRACTICE)
            } else {
                navigateTo(AppScreen.TESTING)
            }
        }
    }
    fun returnHome() {
        _uiState.value = _uiState.value.copy(
            activeSession = null,
            currentTestingEye = null,
            currentScreen = AppScreen.HOME
        )
    }

}










