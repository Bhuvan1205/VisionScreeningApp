package com.example.swasthyatech.ui.calibration.experiment

import com.example.swasthyatech.data.DistanceEstimate
import com.example.swasthyatech.data.DistanceStatus
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

enum class ExperimentMode {
    ACCURACY,
    ROBUSTNESS
}

enum class TrialState {
    IDLE,
    STABILIZING,
    SUCCESS,
    FAILED,
    WAITING_FOR_MOVEMENT
}

data class TrialResult(
    val groundTruthMm: Int,
    val frames: List<DistanceEstimate>,
    val timestamp: Long = System.currentTimeMillis(),
    val rejectedFrames: Int = 0
) {
    val mean: Float
    val median: Float
    val withinTrialStdDev: Float
    val absoluteError: Float
    val percentageError: Float
    val bias: Float
    val avgYaw: Float
    val avgPitch: Float
    val avgIpdPx: Float?
    val focalLengthPx: Float?
    val ipdMmScale: Float?
    val avgConfidence: Float
    val avgUncertaintyMm: Float
    val numberOfFrames: Int

    init {
        numberOfFrames = frames.size
        val dists = frames.map { it.distanceMm }.sorted()
        if (dists.isEmpty()) {
            mean = 0f; median = 0f; withinTrialStdDev = 0f; absoluteError = 0f; percentageError = 0f; bias = 0f; avgYaw = 0f; avgPitch = 0f; avgIpdPx = null; focalLengthPx = null; ipdMmScale = null; avgConfidence = 0f; avgUncertaintyMm = 0f
        } else {
            mean = dists.average().toFloat()
            median = if (dists.size % 2 == 0) {
                (dists[dists.size / 2 - 1] + dists[dists.size / 2]) / 2f
            } else {
                dists[dists.size / 2]
            }
            withinTrialStdDev = sqrt(dists.map { (it - mean).pow(2) }.average()).toFloat()
            bias = mean - groundTruthMm
            absoluteError = abs(bias)
            percentageError = (absoluteError / groundTruthMm) * 100f
            avgYaw = frames.map { it.headEulerY }.average().toFloat()
            avgPitch = frames.map { it.headEulerX }.average().toFloat()
            val ipds = frames.mapNotNull { it.ipdPx }
            avgIpdPx = if (ipds.isNotEmpty()) ipds.average().toFloat() else null
            focalLengthPx = frames.lastOrNull()?.focalLengthPx
            ipdMmScale = frames.lastOrNull()?.ipdMmScale
            avgConfidence = frames.map { it.confidence }.average().toFloat()
            avgUncertaintyMm = frames.map { it.uncertaintyMm }.average().toFloat()
        }
    }
}

data class RobustnessTrial(
    val groundTruthMm: Int,
    val yaw: Float,
    val pitch: Float,
    val status: DistanceStatus,
    val timestamp: Long = System.currentTimeMillis()
)

class GroundTruthExperimentManager(
    private val requiredTrialsPerDistance: Int = 10,
    private val requiredStabilityWindowMs: Long = 1500L,
    private val onTrialCompleted: ((TrialResult) -> Unit)? = null
) {
    var mode: ExperimentMode = ExperimentMode.ACCURACY
        private set
    var currentTargetDistance: Int? = null
        private set
    var currentState: TrialState = TrialState.IDLE
        private set
    var currentFailureReason: String? = null
        private set

    val accuracyTrials = mutableMapOf<Int, MutableList<TrialResult>>()
    val robustnessTrials = mutableListOf<RobustnessTrial>()

    private val currentFrames = mutableListOf<DistanceEstimate>()
    private var currentRejectedFrames = 0
    private var stabilizationStartTime = 0L
    private var lastDistanceForMovementCheck = -1f

    fun setMode(newMode: ExperimentMode) {
        mode = newMode
        resetState()
    }

    fun setTargetDistance(distanceMm: Int) {
        currentTargetDistance = distanceMm
        resetState()
    }

    fun startTrial() {
        if (currentTargetDistance == null) return
        if (currentState == TrialState.WAITING_FOR_MOVEMENT) return
        
        currentFrames.clear()
        currentRejectedFrames = 0
        stabilizationStartTime = System.currentTimeMillis()
        currentState = TrialState.STABILIZING
        currentFailureReason = null
    }

    fun resetState() {
        currentState = TrialState.IDLE
        currentFrames.clear()
        currentRejectedFrames = 0
        currentFailureReason = null
        lastDistanceForMovementCheck = -1f
    }

    fun processFrame(estimate: DistanceEstimate) {
        if (currentTargetDistance == null) return

        if (mode == ExperimentMode.ROBUSTNESS) {
            // Robustness mode doesn't need stabilization windows. 
            // It just records the immediate frame and its rejection status.
            return
        }

        when (currentState) {
            TrialState.WAITING_FOR_MOVEMENT -> {
                // Require device to move by at least 15% OR lose valid status
                val isValid = isFrameValidForAccuracy(estimate)
                if (!isValid || (lastDistanceForMovementCheck > 0 && abs(estimate.distanceMm - lastDistanceForMovementCheck) / lastDistanceForMovementCheck > 0.15f)) {
                    currentState = TrialState.IDLE
                }
            }
            TrialState.STABILIZING -> {
                if (!isFrameValidForAccuracy(estimate)) {
                    currentRejectedFrames++
                    val elapsed = System.currentTimeMillis() - stabilizationStartTime
                    if (elapsed >= requiredStabilityWindowMs) {
                        currentState = TrialState.FAILED
                        currentFailureReason = "Too many invalid frames during stability window: ${estimate.status}"
                    }
                    return
                }

                currentFrames.add(estimate)

                val elapsed = System.currentTimeMillis() - stabilizationStartTime
                if (elapsed >= requiredStabilityWindowMs) {
                    if (currentFrames.size < 10) { // Require minimum 10 frames in that 1.5s
                        currentState = TrialState.FAILED
                        currentFailureReason = "Insufficient frames collected (${currentFrames.size} valid, $currentRejectedFrames rejected)"
                    } else {
                        // Success!
                        saveTrial()
                    }
                }
            }
            else -> {}
        }
    }

    fun recordRobustnessTrial(estimate: DistanceEstimate) {
        if (mode == ExperimentMode.ROBUSTNESS && currentTargetDistance != null) {
            robustnessTrials.add(
                RobustnessTrial(
                    groundTruthMm = currentTargetDistance!!,
                    yaw = estimate.headEulerY,
                    pitch = estimate.headEulerX,
                    status = estimate.status
                )
            )
        }
    }

    private fun saveTrial() {
        val target = currentTargetDistance ?: return
        val list = accuracyTrials.getOrPut(target) { mutableListOf() }
        
        val trialResult = TrialResult(target, currentFrames.toList(), System.currentTimeMillis(), currentRejectedFrames)
        list.add(trialResult)
        
        lastDistanceForMovementCheck = trialResult.mean
        currentState = TrialState.WAITING_FOR_MOVEMENT
        currentFrames.clear()
        
        onTrialCompleted?.invoke(trialResult)
    }

    private fun isFrameValidForAccuracy(estimate: DistanceEstimate): Boolean {
        return estimate.status == DistanceStatus.VALID || 
               estimate.status == DistanceStatus.TOO_CLOSE || 
               estimate.status == DistanceStatus.TOO_FAR
    }
    
    fun getTrialsForCurrentTarget(): Int {
        val target = currentTargetDistance ?: return 0
        return accuracyTrials[target]?.size ?: 0
    }
    
    fun isTargetComplete(distanceMm: Int): Boolean {
        return (accuracyTrials[distanceMm]?.size ?: 0) >= requiredTrialsPerDistance
    }
}
