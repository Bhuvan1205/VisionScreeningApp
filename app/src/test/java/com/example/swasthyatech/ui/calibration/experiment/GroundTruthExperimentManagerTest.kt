package com.example.swasthyatech.ui.calibration.experiment

import com.example.swasthyatech.data.DistanceEstimate
import com.example.swasthyatech.data.DistanceSource
import com.example.swasthyatech.data.DistanceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroundTruthExperimentManagerTest {

    private fun mockEstimate(distanceMm: Float, status: DistanceStatus): DistanceEstimate {
        return DistanceEstimate(
            distanceMm = distanceMm,
            confidence = 0.8f,
            uncertaintyMm = 5f,
            timestamp = System.currentTimeMillis(),
            source = DistanceSource.CAMERA_FACE_GEOMETRY_UNCALIBRATED,
            status = status
        )
    }

    @Test
    fun testStabilityWindowAcceptance() {
        val manager = GroundTruthExperimentManager(requiredTrialsPerDistance = 5, requiredStabilityWindowMs = 100L)
        manager.setTargetDistance(400)
        manager.startTrial()

        assertEquals(TrialState.STABILIZING, manager.currentState)

        // Feed 10 valid frames quickly
        for (i in 1..10) {
            manager.processFrame(mockEstimate(400f, DistanceStatus.VALID))
        }
        
        // Advance time manually or sleep slightly since manager uses System.currentTimeMillis()
        Thread.sleep(110)
        manager.processFrame(mockEstimate(400f, DistanceStatus.VALID))

        assertEquals(TrialState.WAITING_FOR_MOVEMENT, manager.currentState)
        assertEquals(1, manager.getTrialsForCurrentTarget())
    }

    @Test
    fun testStabilityWindowRejection() {
        val manager = GroundTruthExperimentManager(requiredTrialsPerDistance = 5, requiredStabilityWindowMs = 100L)
        manager.setTargetDistance(400)
        manager.startTrial()

        manager.processFrame(mockEstimate(400f, DistanceStatus.VALID))
        // Introduce an invalid frame
        manager.processFrame(mockEstimate(400f, DistanceStatus.INVALID_HEAD_POSE))

        assertEquals(TrialState.FAILED, manager.currentState)
        assertEquals(0, manager.getTrialsForCurrentTarget())
    }

    @Test
    fun testDuplicateTrialPrevention() {
        val manager = GroundTruthExperimentManager(requiredTrialsPerDistance = 5, requiredStabilityWindowMs = 50L)
        manager.setTargetDistance(400)
        
        // Trial 1
        manager.startTrial()
        Thread.sleep(60)
        for (i in 1..10) manager.processFrame(mockEstimate(400f, DistanceStatus.VALID))
        
        assertEquals(TrialState.WAITING_FOR_MOVEMENT, manager.currentState)
        
        // Attempting to start again should be blocked
        manager.startTrial()
        assertEquals(TrialState.WAITING_FOR_MOVEMENT, manager.currentState)
        
        // Move device away
        manager.processFrame(mockEstimate(0f, DistanceStatus.FACE_NOT_DETECTED))
        assertEquals(TrialState.IDLE, manager.currentState)
        
        // Now can start Trial 2
        manager.startTrial()
        assertEquals(TrialState.STABILIZING, manager.currentState)
    }

    @Test
    fun testStatsCalculation() {
        val frames = listOf(
            mockEstimate(390f, DistanceStatus.VALID),
            mockEstimate(400f, DistanceStatus.VALID),
            mockEstimate(410f, DistanceStatus.VALID)
        )
        val result = TrialResult(400, frames)
        
        assertEquals(400f, result.mean, 0.01f)
        assertEquals(400f, result.median, 0.01f)
        assertEquals(0f, result.bias, 0.01f)
        assertEquals(0f, result.absoluteError, 0.01f)
        assertEquals(0f, result.percentageError, 0.01f)
        assertTrue(result.withinTrialStdDev > 0)
    }
}
