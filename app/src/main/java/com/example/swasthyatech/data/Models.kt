package com.example.swasthyatech.data

import kotlinx.serialization.Serializable

@Serializable
enum class Eye {
    RIGHT,
    LEFT
}

@Serializable
enum class OptotypeType {
    TUMBLING_E
}

@Serializable
enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UNKNOWN,
    NOT_VISIBLE
}

@Serializable
enum class TestStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    INTERRUPTED
}

@Serializable
data class ViewingDistance(
    val targetDistanceMm: Float,
    val actualDistanceMm: Float? = null // Populated when camera-based estimation is available
)

@Serializable
data class DeviceCalibration(
    val deviceManufacturer: String,
    val deviceModel: String,
    val screenResolution: String,
    val screenDensity: Float,
    val calibrationMethod: String,
    val pixelsPerMm: Float,
    val calibrationTimestamp: Long,
    val isValid: Boolean
)

@Serializable
data class TestConfiguration(
    val optotypeType: OptotypeType,
    val supportedOrientations: List<Direction>,
    val acuityLevels: List<Float>,
    val testAlgorithmVersion: String,
    val viewingDistance: ViewingDistance,
    val deviceCalibration: DeviceCalibration
)

@Serializable
data class Trial(
    val trialId: Int,
    val eye: Eye,
    val targetAcuity: Float,
    val optotypeType: OptotypeType,
    val presentedOrientation: Direction,
    val userResponse: Direction,
    val isCorrect: Boolean,
    val responseTimeMs: Long?,
    val timestamp: Long
)

// -----------------------------------------------------------------------
// DISTANCE ESTIMATION TYPES
// -----------------------------------------------------------------------

/**
 * Source of a distance estimate. Each enum value maps to a clearly distinct
 * measurement pathway with different uncertainty characteristics.
 */
@Serializable
enum class DistanceSource {
    /**
     * Geometric estimate from camera face landmarks using biological population
     * IPD prior (63 mm ± 3.5 mm). Uncertainty is dominated by scale ambiguity.
     */
    CAMERA_FACE_GEOMETRY_UNCALIBRATED,

    /**
     * Geometric estimate from camera face landmarks after person-specific scale
     * calibration at a known physical distance. Uncertainty is dominated by
     * temporal jitter and the calibration-distance measurement error.
     * NOTE: This is NOT independent validation — it is a scale parameter fit.
     */
    CAMERA_FACE_GEOMETRY_SCALE_CALIBRATED,

    /**
     * User physically positioned phone at a stated distance using a string/ruler.
     * No camera geometry is used. Uncertainty is entirely positional compliance.
     */
    MANUAL_FALLBACK
}

/**
 * Status codes for a distance estimate. Each code represents a distinct failure
 * mode; they must NOT be collapsed into LOW_CONFIDENCE indiscriminately.
 */
@Serializable
enum class DistanceStatus {
    /** Estimate is within acceptable range with sufficient confidence. */
    VALID,
    /** Estimate is geometrically computable but confidence is below threshold. */
    LOW_CONFIDENCE,
    /** Estimated distance is below minimum safe test range. */
    TOO_CLOSE,
    /** Estimated distance is above maximum practical test range. */
    TOO_FAR,
    /** No face was detected in the camera frame. */
    FACE_NOT_DETECTED,
    /** More than one face detected; cannot determine which to track. */
    MULTIPLE_FACES,
    /**
     * Face detected but head pose (yaw/pitch/roll) exceeded engineering thresholds.
     * The 2D IPD measurement is unreliable when the face is not roughly frontal.
     */
    INVALID_HEAD_POSE,
    /**
     * Face detected but eye landmarks are missing or the bounding box is too small
     * to produce a reliable IPD measurement.
     */
    INVALID_FACE_GEOMETRY,
    /**
     * Camera physical metadata (focal length, sensor size) is unavailable.
     * Estimate was produced using a generic FOV fallback — explicitly degraded.
     */
    CAMERA_METADATA_UNAVAILABLE,
    /** The camera or ML Kit detector itself is not supported on this device. */
    UNSUPPORTED
}

/**
 * Camera physical metadata obtained from Camera2 API characteristics.
 * When null fields are present, the distance estimator must degrade
 * to generic approximation and mark the estimate accordingly.
 *
 * @param focalLengthMm Optical focal length in millimeters (Camera2: LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
 * @param sensorWidthMm Physical sensor width in millimeters (Camera2: SENSOR_INFO_PHYSICAL_SIZE)
 * @param sensorHeightMm Physical sensor height in millimeters (Camera2: SENSOR_INFO_PHYSICAL_SIZE)
 * @param activeArrayWidthPx Active sensor array width in pixels (Camera2: SENSOR_INFO_ACTIVE_ARRAY_SIZE)
 * @param activeArrayHeightPx Active sensor array height in pixels (Camera2: SENSOR_INFO_ACTIVE_ARRAY_SIZE)
 * @param cameraId Identifies which physical camera was used
 */
@Serializable
data class CameraIntrinsics(
    val focalLengthMm: Float,
    val sensorWidthMm: Float,
    val sensorHeightMm: Float,
    val activeArrayWidthPx: Int,
    val activeArrayHeightPx: Int,
    val cameraId: String
)

/**
 * A single distance estimate produced by the estimator pipeline.
 *
 * @param distanceMm Geometric estimate of the eye-to-screen distance in mm.
 * @param uncertaintyMm Engineering uncertainty budget (see DistanceEstimationEngine for breakdown).
 *                      This is NOT a statistically calibrated confidence interval.
 * @param confidence Normalized [0,1] inverse-uncertainty score. NOT a probability.
 * @param source Which measurement pathway produced this estimate.
 * @param status Explicit validity / failure classification.
 * @param cameraIntrinsicsAvailable Whether physical camera metadata was used for this estimate.
 */
@Serializable
data class DistanceEstimate(
    val distanceMm: Float,
    val confidence: Float,
    val uncertaintyMm: Float,
    val timestamp: Long,
    val source: DistanceSource,
    val status: DistanceStatus,
    val cameraIntrinsicsAvailable: Boolean = false,
    val headEulerY: Float = 0f,
    val headEulerX: Float = 0f,
    val ipdPx: Float? = null,
    val focalLengthPx: Float? = null,
    val ipdMmScale: Float? = null
)

@Serializable
data class EyeTestResult(
    val eye: Eye,
    val trials: List<Trial>,
    val status: TestStatus,
    val calculatedAcuity: Float?,
    val calculatedAcuityFraction: String?
)

@Serializable
data class FinalResult(
    val rightEyeAcuityLogMar: Float?,
    val leftEyeAcuityLogMar: Float?,
    val overallStatus: TestStatus,
    val resultAlgorithmVersion: String,
    val clinicalInterpretation: String?
)

@Serializable
data class ScreeningSession(
    val sessionId: String,
    val participantId: String,
    val timestamp: Long,
    val schemaVersion: Int = 1,
    val appVersionName: String = "1.0",
    val appVersionCode: Int = 1,
    val testConfiguration: TestConfiguration,
    val rightEyeTest: EyeTestResult?,
    val leftEyeTest: EyeTestResult?,
    val finalResult: FinalResult?,
    val distanceValidation: DistanceValidation? = null,
    val sessionStatus: TestStatus
)

@Serializable
enum class ValidationMethod {
    UNVALIDATED,
    MANUAL_GUIDANCE,
    CAMERA_UNCALIBRATED,
    CAMERA_SCALE_CALIBRATED,
    DEPTH_SENSOR
}

@Serializable
enum class ValidationStatus {
    PENDING,
    VALIDATED,
    UNVALIDATED,
    IN_PROGRESS
}

@Serializable
data class DistanceValidation(
    val targetDistanceMm: Float,
    val measuredDistanceMm: Float?,
    val toleranceMm: Float,
    val method: ValidationMethod,
    val confidence: Float,
    val status: ValidationStatus,
    val uncertaintyMm: Float? = null,
    val cameraIntrinsicsAvailable: Boolean = false
)

// -----------------------------------------------------------------------
// DEVELOPER / RESEARCH INSTRUMENTATION
// NOT used in the normal participant screening flow.
// -----------------------------------------------------------------------

/**
 * A single entry in the physical-device validation experiment log.
 * Ground truth is measured independently by the experimenter.
 * This data structure must NEVER appear in the screening session result.
 *
 * @param groundTruthDistanceMm Independently measured true eye-to-screen distance (mm).
 * @param estimatedDistanceMm Output from the DistanceEstimationEngine.
 * @param absoluteErrorMm |groundTruth - estimated|
 * @param relativeErrorFraction absoluteError / groundTruth
 * @param uncertaintyMm Engineering uncertainty budget from estimator.
 * @param scalingSource Which IPD scale model was active (population prior vs calibrated).
 * @param focalLengthPx Effective focal length in pixels used for this frame.
 * @param headEulerY Yaw angle at time of frame (degrees; 0 = frontal).
 * @param headEulerX Pitch angle (degrees).
 * @param headEulerZ Roll angle (degrees).
 * @param imageAnalysisWidthPx Width of the image-analysis frame in pixels.
 * @param imageAnalysisHeightPx Height of the image-analysis frame in pixels.
 * @param cameraIntrinsics Physical camera metadata if available.
 */
@Serializable
data class DistanceValidationLog(
    val timestamp: Long,
    val groundTruthDistanceMm: Float,
    val estimatedDistanceMm: Float,
    val absoluteErrorMm: Float,
    val relativeErrorFraction: Float,
    val uncertaintyMm: Float,
    val confidence: Float,
    val facesDetected: Int,
    val ipdPx: Float,
    val focalLengthPx: Float,
    val headEulerY: Float?,
    val headEulerX: Float?,
    val headEulerZ: Float?,
    val scalingSource: DistanceSource,
    val imageAnalysisWidthPx: Int,
    val imageAnalysisHeightPx: Int,
    val cameraIntrinsics: CameraIntrinsics?
)
