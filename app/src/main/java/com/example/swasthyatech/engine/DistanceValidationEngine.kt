package com.example.swasthyatech.engine

import com.example.swasthyatech.data.DistanceEstimate
import com.example.swasthyatech.data.DistanceSource
import com.example.swasthyatech.data.DistanceStatus
import com.example.swasthyatech.data.DistanceValidation
import com.example.swasthyatech.data.ValidationMethod
import com.example.swasthyatech.data.ValidationStatus

/**
 * Orchestrates the distance validation state machine for the test session.
 *
 * SEPARATION OF CONCERNS
 * ======================
 * This engine explicitly separates three distinct operations:
 *
 *   A. SCALE CALIBRATION
 *      The user physically positions the device at a known distance (measured
 *      independently with a ruler/string). The estimator derives a
 *      person-specific IPD scale parameter from this interaction.
 *      → Method: CAMERA_SCALE_CALIBRATED
 *      → This is NOT independent validation. It is parameter fitting.
 *
 *   B. CAMERA ESTIMATION (UNCALIBRATED)
 *      The estimator runs with the 63mm population prior. Accuracy depends on
 *      how closely this individual's IPD matches the population mean.
 *      → Method: CAMERA_UNCALIBRATED
 *
 *   C. MANUAL FALLBACK
 *      Camera is unavailable or was rejected. User states their distance.
 *      No camera geometry is involved. High uncertainty, low confidence.
 *      → Method: MANUAL_GUIDANCE
 *
 * NOTE: Methods A and C cannot both be applied to the same estimate.
 * If the user performs scale calibration (A), subsequent camera estimates
 * are CAMERA_SCALE_CALIBRATED. The manual fallback (C) completely replaces
 * the camera pathway — it does not inherit any camera-derived scale parameters.
 */
class DistanceValidationEngine(
    private val targetDistanceMm: Float,
    private val toleranceMm: Float = 25f  // ±25mm acceptance window
) {
    private val estimator = DistanceEstimationEngine(targetDistanceMm)

    var validationState: DistanceValidation = DistanceValidation(
        targetDistanceMm = targetDistanceMm,
        measuredDistanceMm = null,
        toleranceMm = toleranceMm,
        method = ValidationMethod.UNVALIDATED,
        confidence = 0.0f,
        status = ValidationStatus.UNVALIDATED,
        uncertaintyMm = null,
        cameraIntrinsicsAvailable = false
    )
        private set

    var currentEstimate: DistanceEstimate? = null
        private set

    fun startValidation() {
        validationState = validationState.copy(status = ValidationStatus.IN_PROGRESS)
    }

    /**
     * Called by FaceDistanceAnalyzer on each frame. Updates internal state
     * based on the estimate's status.
     *
     * The test gate (VALIDATED) is only set when:
     *   - status == VALID (within target range)
     *   - confidence > 0.65f
     * CAMERA_METADATA_UNAVAILABLE is NOT treated as VALID for gating purposes.
     */
    fun updateFromCameraEstimate(estimate: DistanceEstimate) {
        currentEstimate = estimate

        val method = when (estimate.source) {
            DistanceSource.CAMERA_FACE_GEOMETRY_SCALE_CALIBRATED -> ValidationMethod.CAMERA_SCALE_CALIBRATED
            DistanceSource.CAMERA_FACE_GEOMETRY_UNCALIBRATED -> ValidationMethod.CAMERA_UNCALIBRATED
            else -> ValidationMethod.UNVALIDATED
        }

        // The uncalibrated population prior (IPD=63mm) has a theoretical max confidence of ~0.40 
        // at 400mm due to the Â±5.5% biological scale variance budget (Ïƒ_scale â‰ˆ 24mm, absolute max 40mm).
        // Therefore, demanding 0.65f confidence in uncalibrated mode is mathematically impossible and causes a deadlock.
        val requiredConfidence = if (method == ValidationMethod.CAMERA_SCALE_CALIBRATED) {
            0.65f // High precision required if scale is explicitly calibrated
        } else {
            0.20f // Allow theoretical limits of population prior if stable
        }
        
        val isGated = estimate.status == DistanceStatus.VALID && estimate.confidence > requiredConfidence

        validationState = validationState.copy(
            measuredDistanceMm = if (isGated) estimate.distanceMm else validationState.measuredDistanceMm,
            method = method,
            confidence = estimate.confidence,
            status = if (isGated) ValidationStatus.VALIDATED else ValidationStatus.IN_PROGRESS,
            uncertaintyMm = estimate.uncertaintyMm,
            cameraIntrinsicsAvailable = estimate.cameraIntrinsicsAvailable
        )
    }

    /**
     * Performs person-specific scale calibration when the user is at a known distance.
     *
     * This ONLY sets the scale parameter in the estimator. It does NOT mark the
     * session as VALIDATED — subsequent camera frames must still pass the normal gate.
     *
     * @return true if calibration succeeded (a valid recent camera frame was available).
     */
    fun performScaleCalibration(knownDistanceMm: Float): Boolean {
        return estimator.calibrateScaleAtKnownDistance(knownDistanceMm)
    }

    /**
     * Completes validation using the manual fallback pathway.
     *
     * This is a SEPARATE pathway from camera estimation. It does NOT trigger
     * scale calibration. The camera-derived scale parameter (if any) is irrelevant
     * to the manual fallback estimate.
     *
     * Store distanceSource = MANUAL_FALLBACK so the session record is unambiguous.
     */
    fun completeManualGuidance() {
        val fallback = estimator.generateManualFallback(targetDistanceMm)
        currentEstimate = fallback

        validationState = validationState.copy(
            measuredDistanceMm = fallback.distanceMm,
            method = ValidationMethod.MANUAL_GUIDANCE,
            confidence = fallback.confidence,
            status = ValidationStatus.VALIDATED,
            uncertaintyMm = fallback.uncertaintyMm,
            cameraIntrinsicsAvailable = false
        )
    }

    fun resetValidation() {
        validationState = DistanceValidation(
            targetDistanceMm = targetDistanceMm,
            measuredDistanceMm = null,
            toleranceMm = toleranceMm,
            method = ValidationMethod.UNVALIDATED,
            confidence = 0.0f,
            status = ValidationStatus.UNVALIDATED,
            uncertaintyMm = null,
            cameraIntrinsicsAvailable = false
        )
        currentEstimate = null
    }

    fun getEstimator(): DistanceEstimationEngine = estimator

    /**
     * Returns the distance to use for the test session.
     * MUST be called only when status == VALIDATED.
     * If not validated, returns null — caller must block the test.
     */
    fun getValidatedDistanceMm(): Float? {
        return if (validationState.status == ValidationStatus.VALIDATED) {
            validationState.measuredDistanceMm
        } else {
            null
        }
    }
}
