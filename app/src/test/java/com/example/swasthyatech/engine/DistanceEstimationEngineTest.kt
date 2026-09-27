package com.example.swasthyatech.engine

import com.example.swasthyatech.data.DistanceSource
import com.example.swasthyatech.data.DistanceStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

/**
 * Unit tests for DistanceEstimationEngine.
 *
 * All inputs are synthetic/deterministic. These tests verify the mathematical
 * behavior of the engine, NOT empirically validated accuracy on real hardware.
 * Physical device validation is a separate experiment (see STAGE_3_1_DISTANCE_VALIDATION_PROTOCOL.md).
 */
class DistanceEstimationEngineTest {

    private lateinit var engine: DistanceEstimationEngine

    // Synthetic camera parameters (consistent, deterministic)
    // focalLength = 500px, assumedIpd = 63mm -> at 400mm, IPD_px = 63*500/400 = 78.75px
    private val FOCAL_LENGTH_PX = 500f
    private val KNOWN_DISTANCE_MM = 400f
    private val EXPECTED_IPD_PX = 63f * FOCAL_LENGTH_PX / KNOWN_DISTANCE_MM  // = 78.75px

    @Before
    fun setUp() {
        engine = DistanceEstimationEngine(targetDistanceMm = KNOWN_DISTANCE_MM)
    }

    // -----------------------------------------------------------------------
    // Fundamental geometry correctness
    // -----------------------------------------------------------------------

    @Test
    fun `uncalibrated engine recovers exact distance when IPD matches population prior`() {
        // D = (63 * f_px) / ipd_px = (63 * 500) / 78.75 = 400mm
        pushValidFrames(ipdPx = EXPECTED_IPD_PX, count = 20)
        val result = engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)
        assertEquals(DistanceStatus.VALID, result.status)
        assertEquals(400f, result.distanceMm, 0.5f)
        assertEquals(DistanceSource.CAMERA_FACE_GEOMETRY_UNCALIBRATED, result.source)
    }

    @Test
    fun `scale calibration removes population prior error for individual with different IPD`() {
        // Suppose this user has IPD = 70mm (larger than 63mm mean)
        // At 400mm with f=500: actual IPD_px = 70*500/400 = 87.5px
        // Without calibration the engine would estimate: D = 63*500/87.5 = 360mm (wrong)
        val actualIpdPx = 70f * FOCAL_LENGTH_PX / KNOWN_DISTANCE_MM  // 87.5px

        // Push a frame to populate lastValidIpd
        engine.processFrame(actualIpdPx, FOCAL_LENGTH_PX, metadataAvailable = true)

        // Calibrate at the true known distance
        val calibrated = engine.calibrateScaleAtKnownDistance(KNOWN_DISTANCE_MM)
        assertTrue("Calibration should succeed when a valid frame is available", calibrated)
        assertTrue(engine.isScaleCalibrated())

        // Now push stable frames with the same IPD
        pushValidFrames(ipdPx = actualIpdPx, count = 20)
        val result = engine.processFrame(actualIpdPx, FOCAL_LENGTH_PX, metadataAvailable = true)

        assertEquals(DistanceStatus.VALID, result.status)
        assertEquals(KNOWN_DISTANCE_MM, result.distanceMm, 1f)
        assertEquals(DistanceSource.CAMERA_FACE_GEOMETRY_SCALE_CALIBRATED, result.source)
    }

    @Test
    fun `explicit measured subject IPD overrides population prior`() {
        // User explicitly enters 70mm IPD
        engine.setExplicitIpdMm(70f)
        
        val actualIpdPx = 70f * FOCAL_LENGTH_PX / KNOWN_DISTANCE_MM  // 87.5px
        pushValidFrames(ipdPx = actualIpdPx, count = 20)
        val result = engine.processFrame(actualIpdPx, FOCAL_LENGTH_PX, metadataAvailable = true)

        assertEquals(DistanceStatus.VALID, result.status)
        assertEquals(KNOWN_DISTANCE_MM, result.distanceMm, 1f)
        assertEquals(70f, result.ipdMmScale)
    }

    @Test
    fun `calibrated engine has lower uncertainty than uncalibrated engine`() {
        // Uncalibrated
        val uncalibrated = DistanceEstimationEngine(targetDistanceMm = 400f)
        pushFramesTo(uncalibrated, EXPECTED_IPD_PX, 20)
        val uncalResult = uncalibrated.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)

        // Calibrated
        val calibrated = DistanceEstimationEngine(targetDistanceMm = 400f)
        calibrated.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)
        calibrated.calibrateScaleAtKnownDistance(400f)
        pushFramesTo(calibrated, EXPECTED_IPD_PX, 20)
        val calResult = calibrated.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)

        assertTrue(
            "Calibrated uncertainty (${calResult.uncertaintyMm}) should be less than uncalibrated (${uncalResult.uncertaintyMm})",
            calResult.uncertaintyMm < uncalResult.uncertaintyMm
        )
        assertTrue(
            "Calibrated confidence (${calResult.confidence}) should be higher than uncalibrated (${uncalResult.confidence})",
            calResult.confidence > uncalResult.confidence
        )
    }

    // -----------------------------------------------------------------------
    // Uncertainty model
    // -----------------------------------------------------------------------

    @Test
    fun `stable signal produces non-zero scale uncertainty even at zero temporal variance`() {
        // Perfectly stable frames → σ_temporal ≈ 0
        // But uncalibrated scale uncertainty should still be present (~5.5% of distance)
        pushValidFrames(ipdPx = EXPECTED_IPD_PX, count = 20)
        val result = engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)

        val expectedScaleUncertainty = 400f * 0.055f  // ≈ 22mm
        assertTrue(
            "Scale uncertainty floor must be present even with stable signal. Got ${result.uncertaintyMm}",
            result.uncertaintyMm >= expectedScaleUncertainty * 0.9f  // within 10% of expected floor
        )
    }

    @Test
    fun `confidence does not reach 1 dot 0 on uncalibrated engine with stable signal`() {
        // This verifies that systematic scale bias prevents false high-confidence
        pushValidFrames(ipdPx = EXPECTED_IPD_PX, count = 20)
        val result = engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)
        assertTrue(
            "Uncalibrated engine must not report full confidence due to scale uncertainty. Got ${result.confidence}",
            result.confidence < 0.65f
        )
    }

    @Test
    fun `uncertainty combines temporal and scale components via RSS`() {
        // Use noisy frames to produce measurable σ_temporal
        val ipdValues = floatArrayOf(78f, 80f, 77f, 81f, 79f, 78.5f, 80.5f, 77.5f, 81.5f, 79.5f,
            78f, 80f, 77f, 81f, 79f, 78.5f, 80.5f, 77.5f, 81.5f, 79.5f)
        for (ipd in ipdValues) {
            engine.processFrame(ipd, FOCAL_LENGTH_PX, metadataAvailable = true)
        }
        val result = engine.processFrame(79f, FOCAL_LENGTH_PX, metadataAvailable = true)

        // Verify uncertainty is nonzero and reflects both contributions
        assertTrue(result.uncertaintyMm > 0f)
        // RSS(σ_temporal, σ_scale) ≥ max(σ_temporal, σ_scale), both are nonzero
        val expectedScaleUncertainty = result.distanceMm * 0.055f
        assertTrue(result.uncertaintyMm >= expectedScaleUncertainty * 0.9f)
    }

    // -----------------------------------------------------------------------
    // Frame quality gating
    // -----------------------------------------------------------------------

    @Test
    fun `head yaw beyond threshold returns INVALID_HEAD_POSE`() {
        val result = engine.processFrame(
            ipdPx = EXPECTED_IPD_PX,
            focalLengthPx = FOCAL_LENGTH_PX,
            headEulerY = 20f,  // beyond MAX_HEAD_YAW_DEG = 15°
            metadataAvailable = true
        )
        assertEquals(DistanceStatus.INVALID_HEAD_POSE, result.status)
    }

    @Test
    fun `head pitch beyond threshold returns INVALID_HEAD_POSE`() {
        val result = engine.processFrame(
            ipdPx = EXPECTED_IPD_PX,
            focalLengthPx = FOCAL_LENGTH_PX,
            headEulerX = 25f,  // beyond MAX_HEAD_PITCH_DEG = 20°
            metadataAvailable = true
        )
        assertEquals(DistanceStatus.INVALID_HEAD_POSE, result.status)
    }

    @Test
    fun `head pose within threshold does not gate frame`() {
        pushValidFrames(ipdPx = EXPECTED_IPD_PX, count = 20)
        val result = engine.processFrame(
            ipdPx = EXPECTED_IPD_PX,
            focalLengthPx = FOCAL_LENGTH_PX,
            headEulerY = 10f,   // within 15°
            headEulerX = 15f,   // within 20°
            metadataAvailable = true
        )
        assertNotEquals(DistanceStatus.INVALID_HEAD_POSE, result.status)
    }

    @Test
    fun `tiny IPD returns INVALID_FACE_GEOMETRY`() {
        val result = engine.processFrame(
            ipdPx = 10f,  // below MIN_IPD_PX = 20px
            focalLengthPx = FOCAL_LENGTH_PX,
            metadataAvailable = true
        )
        assertEquals(DistanceStatus.INVALID_FACE_GEOMETRY, result.status)
    }

    @Test
    fun `sudden large IPD jump does not corrupt buffer`() {
        // Prime with stable frames
        pushValidFrames(ipdPx = EXPECTED_IPD_PX, count = 15)
        val stableResult = engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)
        val stableDistance = stableResult.distanceMm

        // Inject a large IPD jump (simulating track loss / another face)
        val jumperIpdPx = EXPECTED_IPD_PX * 1.5f  // 50% jump — exceeds 25% threshold
        val jumpResult = engine.processFrame(jumperIpdPx, FOCAL_LENGTH_PX, metadataAvailable = true)

        // Distance estimate from existing buffer should be close to previous stable value
        assertEquals(stableDistance, jumpResult.distanceMm, 10f)
    }

    // -----------------------------------------------------------------------
    // Range gating
    // -----------------------------------------------------------------------

    @Test
    fun `estimate classified as TOO_CLOSE when face is very near`() {
        // targetDistance = 400mm, too close < 350mm
        // To get 300mm: ipdPx = 63 * 500 / 300 = 105px
        val nearIpdPx = 63f * FOCAL_LENGTH_PX / 300f
        pushValidFrames(ipdPx = nearIpdPx, count = 20)
        val result = engine.processFrame(nearIpdPx, FOCAL_LENGTH_PX, metadataAvailable = true)
        assertEquals(DistanceStatus.TOO_CLOSE, result.status)
    }

    @Test
    fun `estimate classified as TOO_FAR when face is very far`() {
        // targetDistance = 400mm, too far > 550mm
        // To get 600mm: ipdPx = 63 * 500 / 600 = 52.5px
        val farIpdPx = 63f * FOCAL_LENGTH_PX / 600f
        pushValidFrames(ipdPx = farIpdPx, count = 20)
        val result = engine.processFrame(farIpdPx, FOCAL_LENGTH_PX, metadataAvailable = true)
        assertEquals(DistanceStatus.TOO_FAR, result.status)
    }

    // -----------------------------------------------------------------------
    // Camera metadata states
    // -----------------------------------------------------------------------

    @Test
    fun `estimate is CAMERA_METADATA_UNAVAILABLE when no intrinsics provided in valid range`() {
        // Push valid-range frames with metadataAvailable = false
        pushValidFrames(ipdPx = EXPECTED_IPD_PX, count = 20, metadataAvailable = false)
        val result = engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = false)
        assertEquals(DistanceStatus.CAMERA_METADATA_UNAVAILABLE, result.status)
    }

    @Test
    fun `estimate is VALID when intrinsics provided and in valid range`() {
        pushValidFrames(ipdPx = EXPECTED_IPD_PX, count = 20)
        val result = engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)
        assertEquals(DistanceStatus.VALID, result.status)
    }

    // -----------------------------------------------------------------------
    // Fatal error states
    // -----------------------------------------------------------------------

    @Test
    fun `FACE_NOT_DETECTED clears history`() {
        pushValidFrames(ipdPx = EXPECTED_IPD_PX, count = 20)
        engine.reportFatalError(DistanceStatus.FACE_NOT_DETECTED)

        // Next frame should be LOW_CONFIDENCE (insufficient history)
        val nextResult = engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)
        assertEquals(DistanceStatus.LOW_CONFIDENCE, nextResult.status)
    }

    @Test
    fun `MULTIPLE_FACES clears history`() {
        pushValidFrames(ipdPx = EXPECTED_IPD_PX, count = 20)
        engine.reportFatalError(DistanceStatus.MULTIPLE_FACES)
        val nextResult = engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)
        assertEquals(DistanceStatus.LOW_CONFIDENCE, nextResult.status)
    }

    // -----------------------------------------------------------------------
    // Manual fallback
    // -----------------------------------------------------------------------

    @Test
    fun `manual fallback produces MANUAL_FALLBACK source and high uncertainty`() {
        val result = engine.generateManualFallback(400f)
        assertEquals(DistanceSource.MANUAL_FALLBACK, result.source)
        assertEquals(400f, result.distanceMm, 0.01f)
        assertTrue("Manual fallback uncertainty must be ≥ 50mm, got ${result.uncertaintyMm}",
            result.uncertaintyMm >= 50f)
        assertTrue("Manual fallback confidence must be < 0.6, got ${result.confidence}",
            result.confidence < 0.6f)
    }

    // -----------------------------------------------------------------------
    // Calibration vs validation separation
    // -----------------------------------------------------------------------

    @Test
    fun `calibration at same distance as target does not auto-validate the engine`() {
        // Calibration performs scale fitting only — it does NOT certify the measurement
        engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)
        engine.calibrateScaleAtKnownDistance(KNOWN_DISTANCE_MM)

        // After calibration buffer is cleared — first new frame should be LOW_CONFIDENCE
        val result = engine.processFrame(EXPECTED_IPD_PX, FOCAL_LENGTH_PX, metadataAvailable = true)
        assertEquals(DistanceStatus.LOW_CONFIDENCE, result.status)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun pushValidFrames(ipdPx: Float, count: Int, metadataAvailable: Boolean = true) {
        repeat(count) {
            engine.processFrame(ipdPx, FOCAL_LENGTH_PX, metadataAvailable = metadataAvailable)
        }
    }

    private fun pushFramesTo(eng: DistanceEstimationEngine, ipdPx: Float, count: Int) {
        repeat(count) {
            eng.processFrame(ipdPx, FOCAL_LENGTH_PX, metadataAvailable = true)
        }
    }
}
