package com.example.swasthyatech.calibration

import com.example.swasthyatech.data.DeviceCalibration

/**
 * Handles the logic for manual screen calibration.
 * 
 * The user adjusts a displayed reference line/box until it matches a known 
 * physical standard (e.g., a standard credit card which is 85.60 mm wide, 
 * or a simple ruler measurement of 50 mm).
 */
class CalibrationEngine(
    private val physicalReferenceLengthMm: Float = 50f // Default reference is 50 mm
) {
    init {
        require(physicalReferenceLengthMm > 0) { "Reference length must be positive." }
    }

    /**
     * Given the user-adjusted size of the reference in pixels, calculates the pixels-per-millimeter.
     * 
     * @param adjustedPixels The length in pixels that the user claims matches the physical reference length.
     */
    fun calculatePixelsPerMm(adjustedPixels: Float): Float {
        require(adjustedPixels > 0) { "Adjusted pixels must be positive." }
        return adjustedPixels / physicalReferenceLengthMm
    }

    /**
     * Finalizes the calibration and creates a DeviceCalibration data object for the Phase 1 schema.
     * 
     * @param adjustedPixels The final agreed length in pixels matching the reference.
     * @param deviceManufacturer System provided manufacturer (e.g., android.os.Build.MANUFACTURER)
     * @param deviceModel System provided model (e.g., android.os.Build.MODEL)
     * @param screenResolution String representation of resolution (e.g. "1080x1920")
     * @param screenDensity Raw android display density metric
     */
    fun createDeviceCalibration(
        adjustedPixels: Float,
        deviceManufacturer: String,
        deviceModel: String,
        screenResolution: String,
        screenDensity: Float
    ): DeviceCalibration {
        val pixelsPerMm = calculatePixelsPerMm(adjustedPixels)
        
        return DeviceCalibration(
            deviceManufacturer = deviceManufacturer,
            deviceModel = deviceModel,
            screenResolution = screenResolution,
            screenDensity = screenDensity,
            calibrationMethod = "MANUAL_REFERENCE_${physicalReferenceLengthMm}MM",
            pixelsPerMm = pixelsPerMm,
            calibrationTimestamp = System.currentTimeMillis(),
            isValid = true
        )
    }
}
