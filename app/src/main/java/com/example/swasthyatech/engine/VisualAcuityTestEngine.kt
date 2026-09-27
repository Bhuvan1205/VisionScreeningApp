package com.example.swasthyatech.engine

import com.example.swasthyatech.data.Direction
import com.example.swasthyatech.data.Eye
import com.example.swasthyatech.data.EyeTestResult
import com.example.swasthyatech.data.OptotypeType
import com.example.swasthyatech.data.TestStatus
import com.example.swasthyatech.data.Trial
import kotlin.random.Random

/**
 * State of the visual acuity test engine.
 */
enum class EngineState {
    NOT_STARTED,
    PRESENTING,
    WAITING_FOR_RESPONSE,
    COMPLETED,
    ABORTED
}

/**
 * The core Visual-Acuity Test Engine.
 * Manages the trial sequence, correctness evaluation, and integration with the MeasurementEngine.
 */
class VisualAcuityTestEngine(
    private val eye: Eye,
    private val algorithm: TestAlgorithm,
    private val measurementEngine: MeasurementEngine,
    private val viewingDistanceMm: Float,
    private val random: Random = Random.Default
) {
    var state: EngineState = EngineState.NOT_STARTED
        private set

    private val trialHistory = mutableListOf<Trial>()
    
    // State for the current trial
    var currentAcuityLogMar: Float? = null
        private set
    var currentOrientation: Direction? = null
        private set
    var currentOptotypeDimensions: OptotypeDimensions? = null
        private set
    
    private var trialStartTimeMs: Long = 0

    fun start() {
        require(state == EngineState.NOT_STARTED) { "Test already started or completed." }
        state = EngineState.PRESENTING // Intermediate
        prepareNextTrial()
    }

    private fun prepareNextTrial() {
        currentAcuityLogMar = algorithm.getNextAcuity()
        
        if (currentAcuityLogMar == null) {
            // No more levels
            state = EngineState.COMPLETED
            return
        }

        currentOrientation = algorithm.generateNextOrientation(random)
        currentOptotypeDimensions = measurementEngine.calculateOptotypePixelDimensions(
            targetAcuityLogMar = currentAcuityLogMar!!,
            viewingDistanceMm = viewingDistanceMm
        )
        
        state = EngineState.WAITING_FOR_RESPONSE
        trialStartTimeMs = System.currentTimeMillis()
    }

    /**
     * Receives a normalized semantic response (UP, DOWN, LEFT, RIGHT, NOT_VISIBLE).
     */
    fun submitResponse(response: Direction) {
        require(state == EngineState.WAITING_FOR_RESPONSE) { "Engine is not waiting for a response." }
        
        val responseTime = System.currentTimeMillis() - trialStartTimeMs
        val targetOrientation = currentOrientation!!
        val isCorrect = (response == targetOrientation)
        
        // Record trial
        val trial = Trial(
            trialId = trialHistory.size + 1,
            eye = eye,
            targetAcuity = currentAcuityLogMar!!,
            optotypeType = OptotypeType.TUMBLING_E,
            presentedOrientation = targetOrientation,
            userResponse = response,
            isCorrect = isCorrect,
            responseTimeMs = responseTime,
            timestamp = System.currentTimeMillis()
        )
        trialHistory.add(trial)

        // Evaluate progression
        val isTestComplete = algorithm.submitResponse(targetOrientation, response)
        
        if (isTestComplete) {
            state = EngineState.COMPLETED
        } else {
            prepareNextTrial()
        }
    }

    fun abort() {
        state = EngineState.ABORTED
    }

    /**
     * Retrieves the final result model for the tested eye.
     */
    fun getEyeTestResult(): EyeTestResult {
        val testStatus = when (state) {
            EngineState.COMPLETED -> TestStatus.COMPLETED
            EngineState.ABORTED -> TestStatus.INTERRUPTED
            EngineState.NOT_STARTED -> TestStatus.NOT_STARTED
            else -> TestStatus.IN_PROGRESS
        }
        
        val threshold = algorithm.getCurrentThreshold()
        
        return EyeTestResult(
            eye = eye,
            trials = trialHistory.toList(),
            status = testStatus,
            calculatedAcuity = threshold,
            calculatedAcuityFraction = convertLogMarToFraction(threshold)
        )
    }

    private fun convertLogMarToFraction(logMar: Float?): String? {
        if (logMar == null) return null
        return when {
            logMar <= -0.1f -> "6/4.8"
            logMar <= 0.0f -> "6/6"
            logMar <= 0.1f -> "6/7.5"
            logMar <= 0.2f -> "6/9"
            logMar <= 0.3f -> "6/12"
            logMar <= 0.4f -> "6/15"
            logMar <= 0.5f -> "6/18"
            logMar <= 0.6f -> "6/24"
            logMar <= 0.7f -> "6/30"
            logMar <= 0.8f -> "6/38"
            logMar <= 0.9f -> "6/48"
            else -> "6/60"
        }
    }
}
