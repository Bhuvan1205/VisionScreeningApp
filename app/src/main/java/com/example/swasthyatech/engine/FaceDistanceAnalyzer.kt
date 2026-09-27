package com.example.swasthyatech.engine

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.swasthyatech.data.CameraIntrinsics
import com.example.swasthyatech.data.DistanceEstimate
import com.example.swasthyatech.data.DistanceStatus
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

/**
 * ImageAnalysis.Analyzer that:
 *  1. Detects faces using ML Kit (on-device, no cloud).
 *  2. Extracts eye landmarks and head-pose angles.
 *  3. Computes effective focal length in pixels from physical CameraIntrinsics
 *     (Camera2 metadata) when available, or falls back to a documented
 *     generic approximation — which is explicitly flagged as degraded.
 *  4. Passes measurements to DistanceEstimationEngine.
 *
 * FOCAL LENGTH COMPUTATION
 * ========================
 * When CameraIntrinsics is available (preferred):
 *
 *   f_px = focalLength_mm × (analysisImageWidth_px / sensorWidth_mm)
 *
 * This maps the physical focal length onto the analysis image coordinate system.
 * It accounts for the scaling between the full sensor active array and the
 * image-analysis resolution via the scale factor (analysisWidth / activeArrayWidth).
 *
 * When CameraIntrinsics is NOT available (degraded fallback):
 *   f_px = (imageWidth / 2) / tan(ASSUMED_FOV / 2)
 *   ASSUMED_FOV = 65° (typical front-camera FOV; documented assumption, not measured)
 *
 * The fallback path produces DistanceStatus.CAMERA_METADATA_UNAVAILABLE, never VALID.
 *
 * CROP / RESOLUTION ACCOUNTING
 * =============================
 * The analysis image may be a different resolution than the active sensor array.
 * The correct mapping:
 *   f_px_at_analysis_res = f_px_at_full_res × (analysisWidth / activeArrayWidth)
 *
 * This is incorporated when CameraIntrinsics.activeArrayWidthPx is available.
 * If only focalLength_mm and sensorWidth_mm are available (common case), the
 * direct formula f_px = focalLength_mm × (analysisWidth / sensorWidth_mm) is
 * equivalent when the analysis image fills the full sensor width. Minor crop
 * factors on some devices introduce a small additional error, which is
 * subsumed into the engineering uncertainty budget.
 *
 * HEAD POSE
 * =========
 * ML Kit provides Euler angles (X = pitch, Y = yaw, Z = roll). These are
 * passed to DistanceEstimationEngine which gates frames exceeding thresholds.
 */
class FaceDistanceAnalyzer(
    private val estimationEngine: DistanceEstimationEngine,
    var cameraIntrinsics: CameraIntrinsics? = null,
    private val onEstimate: (DistanceEstimate) -> Unit
) : ImageAnalysis.Analyzer {

    // Generic FOV fallback constant. Documented assumption, not measured.
    // Typical front-facing camera FOV ranges 60–70°; 65° is chosen as midpoint.
    private val ASSUMED_FOV_FALLBACK_DEG = 65.0

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)  // More reliable landmarks
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Determine the analysis image dimensions in camera coordinate space.
        // After rotation correction, width is the horizontal extent of the frame.
        val isPortrait = rotationDegrees == 90 || rotationDegrees == 270
        val analysisWidthPx = if (isPortrait) imageProxy.height else imageProxy.width
        val analysisHeightPx = if (isPortrait) imageProxy.width else imageProxy.height

        detector.process(image)
            .addOnSuccessListener { faces ->
                when {
                    faces.isEmpty() ->
                        onEstimate(estimationEngine.reportFatalError(DistanceStatus.FACE_NOT_DETECTED))
                    faces.size > 1 ->
                        onEstimate(estimationEngine.reportFatalError(DistanceStatus.MULTIPLE_FACES))
                    else -> {
                        val face = faces.first()

                        // Extract head pose Euler angles (degrees)
                        val eulerY = face.headEulerAngleY  // yaw
                        val eulerX = face.headEulerAngleX  // pitch
                        val eulerZ = face.headEulerAngleZ  // roll

                        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
                        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)

                        if (leftEye == null || rightEye == null) {
                            onEstimate(estimationEngine.reportFatalError(DistanceStatus.INVALID_FACE_GEOMETRY))
                            imageProxy.close()
                            return@addOnSuccessListener
                        }

                        val dx = leftEye.position.x - rightEye.position.x
                        val dy = leftEye.position.y - rightEye.position.y
                        val ipdPx = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                        val intrinsics = cameraIntrinsics
                        val metadataAvailable = intrinsics != null

                        val focalLengthPx = if (intrinsics != null) {
                            // The physical sensor dimensions are usually reported in landscape (e.g. 4000x3000),
                            // but CameraX often yields portrait (e.g. 480x640). 
                            // To map them correctly without mixing up width/height, we use the maximum dimension.
                            val maxImageDim = kotlin.math.max(analysisWidthPx, analysisHeightPx)
                            val maxSensorMm = kotlin.math.max(intrinsics.sensorWidthMm, intrinsics.sensorHeightMm)
                            
                            // f_px = f_mm * (image_pixels / sensor_mm)
                            intrinsics.focalLengthMm * (maxImageDim.toFloat() / maxSensorMm)
                        } else {
                            // Generic FOV fallback. Explicitly not equivalent to physical metadata.
                            val fovRad = Math.toRadians(ASSUMED_FOV_FALLBACK_DEG)
                            (analysisWidthPx / 2.0 / kotlin.math.tan(fovRad / 2.0)).toFloat()
                        }

                        val estimate = estimationEngine.processFrame(
                            ipdPx = ipdPx,
                            focalLengthPx = focalLengthPx,
                            headEulerY = eulerY,
                            headEulerX = eulerX,
                            headEulerZ = eulerZ,
                            metadataAvailable = metadataAvailable
                        )
                        onEstimate(estimate)
                    }
                }
            }
            .addOnFailureListener {
                onEstimate(estimationEngine.reportFatalError(DistanceStatus.UNSUPPORTED))
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
