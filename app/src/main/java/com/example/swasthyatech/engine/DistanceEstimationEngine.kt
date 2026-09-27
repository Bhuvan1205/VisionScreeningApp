package com.example.swasthyatech.engine

import com.example.swasthyatech.data.DistanceEstimate
import com.example.swasthyatech.data.DistanceSource
import com.example.swasthyatech.data.DistanceStatus
import kotlin.math.sqrt

/**
 * Produces geometric distance estimates from camera eye-landmark measurements.
 *
 * MATHEMATICAL MODEL
 * ==================
 * Uses the thin-lens / pinhole camera model:
 *
 *   D_mm = (IPD_mm × f_px) / IPD_px
 *
 * where:
 *   D_mm    = estimated eye-to-screen distance (mm)
 *   IPD_mm  = inter-pupillary distance scale parameter (mm)
 *   f_px    = effective focal length in pixels (derived from physical camera
 *             intrinsics when available, or a generic 60° FOV fallback)
 *   IPD_px  = detected inter-pupillary distance in the image plane (pixels)
 *
 * SCALE PARAMETER (IPD_mm)
 * ========================
 * Two modes:
 *   1. Population prior (UNCALIBRATED): IPD_mm = 63.0 mm.
 *      Based on adult human population mean. Population ±2 SD spans ~56–70 mm,
 *      giving a systematic scale uncertainty of ±5.5% of the estimated distance.
 *      This is explicitly encoded as an engineering uncertainty floor.
 *
 *   2. Person-specific scale (SCALE_CALIBRATED): The user physically holds the
 *      device at a known distance D_known. The estimator back-calculates:
 *          IPD_mm = (D_known_mm × IPD_px) / f_px
 *      This is a scale parameter fit — it removes the population-prior bias
 *      for this individual but is NOT independent camera validation.
 *      Residual uncertainty: ~2% (calibration distance hold error + landmark noise).
 *
 * UNCERTAINTY MODEL (Engineering Budget)
 * =======================================
 * totalUncertainty = sqrt(σ_temporal² + σ_scale²)  [root-sum-square]
 *
 * Direct addition was rejected because σ_temporal and σ_scale are independent
 * error sources; RSS is more appropriate than worst-case addition.
 *
 *   σ_temporal: standard deviation of raw estimates over the temporal buffer.
 *              Captures frame-to-frame measurement jitter.
 *   σ_scale:    systematic bias from IPD scale uncertainty.
 *              = median × 0.055 (uncalibrated, population prior)
 *              = median × 0.020 (scale-calibrated, calibration-distance error)
 *
 * IMPORTANT: This is an engineering uncertainty budget, NOT a statistical
 * confidence interval derived from empirical validation. Physical device
 * validation experiments are required before making stronger claims.
 *
 * FRAME QUALITY GATING
 * ====================
 * Frames are rejected (not added to temporal buffer) when:
 *   - Head pose Euler Y (yaw) exceeds ±MAX_HEAD_YAW_DEG (prototype threshold)
 *   - Head pose Euler X (pitch) exceeds ±MAX_HEAD_PITCH_DEG (prototype threshold)
 *   - IPD_px < MIN_IPD_PX (face too small / too far, landmarks unreliable)
 *   - IPD jump > MAX_IPD_JUMP_FRACTION from previous valid frame (sudden motion)
 *
 * These thresholds are engineering choices for this prototype, not clinically
 * validated gating criteria.
 */
class DistanceEstimationEngine(
    private val targetDistanceMm: Float,
    private val bufferSize: Int = 20  // Increased: ~670ms at 30fps for more stable median
) {
    // Temporal buffer of valid raw distance estimates
    private val history = mutableListOf<Float>()

    // Scale model state
    private var scaleIpdMm: Float? = null  // null = using population prior
    private var lastValidIpdPx: Float? = null
    private var lastValidFocalLengthPx: Float? = null
    private var lastEulerY: Float = 0f
    private var lastEulerX: Float = 0f

    // Constants: biological population prior
    // Source: Dodgson (2004) "Variation and extremes of human interpupillary distance"
    private val POPULATION_PRIOR_IPD_MM = 63.0f
    private val UNCALIBRATED_SCALE_UNCERTAINTY_FRACTION = 0.055f  // ±3.5mm / 63mm ≈ 5.5%
    private val CALIBRATED_SCALE_UNCERTAINTY_FRACTION = 0.020f    // hold error + landmark noise

    // Frame quality gating thresholds (engineering prototype values, not clinically validated)
    private val MAX_HEAD_YAW_DEG = 15f    // beyond this, 2D IPD_px is significantly foreshortened
    private val MAX_HEAD_PITCH_DEG = 20f  // beyond this, vertical IPD component is artificially large
    private val MIN_IPD_PX = 20f          // below this, landmark precision is insufficient
    private val MAX_IPD_JUMP_FRACTION = 0.25f  // reject frames where IPD jumps >25% suddenly

    // Maximum uncertainty before declaring LOW_CONFIDENCE (engineering threshold)
    private val MAX_UNCERTAINTY_FOR_VALID_MM = 40f

    /**
     * Performs a person-specific scale calibration.
     *
     * The user physically positions the device at [knownDistanceMm] (measured
     * with a ruler/string). The estimator uses the current IPD_px and f_px
     * to derive the person-specific IPD_mm scale parameter.
     *
     * NOTE: This is a scale parameter fit. It is NOT independent validation.
     * Subsequent distance estimates will be more accurate for this person,
     * but the accuracy has NOT been independently verified.
     *
     * @return true if calibration succeeded (a valid recent frame was available), false otherwise.
     */
    fun calibrateScaleAtKnownDistance(knownDistanceMm: Float): Boolean {
        val ipd = lastValidIpdPx
        val foc = lastValidFocalLengthPx
        if (ipd != null && foc != null && ipd >= MIN_IPD_PX) {
            scaleIpdMm = (knownDistanceMm * ipd) / foc
            history.clear()  // Reset: old estimates used different scale
            return true
        }
        return false
    }

    /**
     * Explicitly sets the scale parameter to a known measured subject IPD.
     * This avoids the population prior confounder for experimental validation.
     */
    fun setExplicitIpdMm(ipdMm: Float?) {
        scaleIpdMm = ipdMm
        history.clear() // Reset buffer on scale change
    }

    fun getExplicitIpdMm(): Float? = scaleIpdMm

    fun isScaleCalibrated(): Boolean = scaleIpdMm != null

    /**
     * Processes a single camera frame's measurements.
     *
     * @param ipdPx Euclidean distance between eye landmarks in image pixels.
     * @param focalLengthPx Effective focal length in pixels (from camera intrinsics or fallback).
     * @param headEulerY Yaw angle in degrees. 0 = frontal. Positive = face turned right.
     * @param headEulerX Pitch angle in degrees. 0 = level. Positive = looking up.
     * @param headEulerZ Roll angle in degrees.
     * @param metadataAvailable Whether physical CameraIntrinsics was used for focalLengthPx.
     */
    fun processFrame(
        ipdPx: Float,
        focalLengthPx: Float,
        headEulerY: Float = 0f,
        headEulerX: Float = 0f,
        headEulerZ: Float = 0f,
        metadataAvailable: Boolean
    ): DistanceEstimate {
        // Head pose gating
        if (kotlin.math.abs(headEulerY) > MAX_HEAD_YAW_DEG ||
            kotlin.math.abs(headEulerX) > MAX_HEAD_PITCH_DEG) {
            return errorEstimate(DistanceStatus.INVALID_HEAD_POSE, metadataAvailable)
        }

        // Minimum IPD gate
        if (ipdPx < MIN_IPD_PX) {
            return errorEstimate(DistanceStatus.INVALID_FACE_GEOMETRY, metadataAvailable)
        }

        // IPD jump gate — reject sudden large changes (motion artifact / track loss)
        val prev = lastValidIpdPx
        if (prev != null && kotlin.math.abs(ipdPx - prev) / prev > MAX_IPD_JUMP_FRACTION) {
            // Don't add to buffer; return LOW_CONFIDENCE with current buffer content
            return calculateSmoothedEstimate(metadataAvailable)
        }

        // Frame is valid — record and update buffer
        lastValidIpdPx = ipdPx
        lastValidFocalLengthPx = focalLengthPx
        lastEulerY = headEulerY
        lastEulerX = headEulerX

        val currentScaleIpdMm = scaleIpdMm ?: POPULATION_PRIOR_IPD_MM
        val rawDistanceMm = (currentScaleIpdMm * focalLengthPx) / ipdPx

        if (history.size >= bufferSize) {
            history.removeAt(0)
        }
        history.add(rawDistanceMm)

        return calculateSmoothedEstimate(metadataAvailable)
    }

    fun reportFatalError(status: DistanceStatus): DistanceEstimate {
        if (status == DistanceStatus.FACE_NOT_DETECTED || status == DistanceStatus.MULTIPLE_FACES) {
            history.clear()
            lastValidIpdPx = null
        }
        return errorEstimate(status, cameraIntrinsicsAvailable = false)
    }

    private fun errorEstimate(status: DistanceStatus, cameraIntrinsicsAvailable: Boolean): DistanceEstimate {
        return DistanceEstimate(
            distanceMm = if (history.isNotEmpty()) history.last() else 0f,
            confidence = 0f,
            uncertaintyMm = Float.MAX_VALUE,
            timestamp = System.currentTimeMillis(),
            source = currentSource(),
            status = status,
            cameraIntrinsicsAvailable = cameraIntrinsicsAvailable,
            headEulerY = lastEulerY,
            headEulerX = lastEulerX,
            ipdPx = lastValidIpdPx,
            focalLengthPx = lastValidFocalLengthPx,
            ipdMmScale = scaleIpdMm ?: POPULATION_PRIOR_IPD_MM
        )
    }

    private fun calculateSmoothedEstimate(metadataAvailable: Boolean): DistanceEstimate {
        val minValidFrames = 8  // Require at least 8 valid frames for a stable estimate
        if (history.size < minValidFrames) {
            return DistanceEstimate(
                distanceMm = history.lastOrNull() ?: 0f,
                confidence = 0f,
                uncertaintyMm = Float.MAX_VALUE,
                timestamp = System.currentTimeMillis(),
                source = currentSource(),
                status = DistanceStatus.LOW_CONFIDENCE,
                cameraIntrinsicsAvailable = metadataAvailable,
                headEulerY = lastEulerY,
                headEulerX = lastEulerX,
                ipdPx = lastValidIpdPx,
                focalLengthPx = lastValidFocalLengthPx,
                ipdMmScale = scaleIpdMm ?: POPULATION_PRIOR_IPD_MM
            )
        }

        // Median for robust central tendency (resistant to outliers)
        val sorted = history.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
        } else {
            sorted[sorted.size / 2]
        }

        // σ_temporal: standard deviation over the buffer
        val mean = history.average().toFloat()
        var variance = 0f
        for (v in history) { variance += (v - mean) * (v - mean) }
        variance /= history.size
        val sigmaTemporal = sqrt(variance)

        // σ_scale: systematic bias from scale parameter uncertainty
        val scaleFraction = if (scaleIpdMm != null) {
            CALIBRATED_SCALE_UNCERTAINTY_FRACTION
        } else {
            UNCALIBRATED_SCALE_UNCERTAINTY_FRACTION
        }
        val sigmaScale = median * scaleFraction

        // Root-sum-square combination (independent error sources)
        val totalUncertainty = sqrt(sigmaTemporal * sigmaTemporal + sigmaScale * sigmaScale)

        // Confidence: normalized inverse of uncertainty. NOT a probability.
        val confidence = kotlin.math.max(0f, 1f - (totalUncertainty / MAX_UNCERTAINTY_FOR_VALID_MM))

        // Camera-metadata degradation: if metadata unavailable, mark status accordingly
        val baseStatus = when {
            totalUncertainty > MAX_UNCERTAINTY_FOR_VALID_MM -> DistanceStatus.LOW_CONFIDENCE
            median < (targetDistanceMm - 50f) -> DistanceStatus.TOO_CLOSE
            median > (targetDistanceMm + 150f) -> DistanceStatus.TOO_FAR
            else -> DistanceStatus.VALID
        }
        val status = if (baseStatus == DistanceStatus.VALID && !metadataAvailable) {
            // Valid geometrically but explicitly degraded due to generic FOV fallback
            DistanceStatus.CAMERA_METADATA_UNAVAILABLE
        } else {
            baseStatus
        }

        return DistanceEstimate(
            distanceMm = median,
            confidence = confidence,
            uncertaintyMm = totalUncertainty,
            timestamp = System.currentTimeMillis(),
            source = currentSource(),
            status = status,
            cameraIntrinsicsAvailable = metadataAvailable,
            headEulerY = lastEulerY,
            headEulerX = lastEulerX,
            ipdPx = lastValidIpdPx,
            focalLengthPx = lastValidFocalLengthPx,
            ipdMmScale = scaleIpdMm ?: POPULATION_PRIOR_IPD_MM
        )
    }

    private fun currentSource(): DistanceSource = if (scaleIpdMm != null) {
        DistanceSource.CAMERA_FACE_GEOMETRY_SCALE_CALIBRATED
    } else {
        DistanceSource.CAMERA_FACE_GEOMETRY_UNCALIBRATED
    }

    /**
     * Produces a manual-fallback estimate.
     * Clears the camera estimation buffer — the two pathways are mutually exclusive.
     *
     * @param statedDistanceMm Distance stated by the user / operator (mm).
     */
    fun generateManualFallback(statedDistanceMm: Float): DistanceEstimate {
        history.clear()
        lastValidIpdPx = null
        lastValidFocalLengthPx = null
        return DistanceEstimate(
            distanceMm = statedDistanceMm,
            confidence = 0.4f,    // Deliberately low: compliance uncertainty is large
            uncertaintyMm = 80f,  // Engineering estimate: user positioning error ≈ ±80mm
            timestamp = System.currentTimeMillis(),
            source = DistanceSource.MANUAL_FALLBACK,
            status = DistanceStatus.VALID,
            cameraIntrinsicsAvailable = false
        )
    }
}

