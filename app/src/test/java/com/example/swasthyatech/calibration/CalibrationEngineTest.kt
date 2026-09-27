package com.example.swasthyatech.calibration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationEngineTest {

    @Test
    fun `test pixels per mm calculation`() {
        val engine = CalibrationEngine(physicalReferenceLengthMm = 50f)
        
        // User adjusts box to 500 pixels. Reference is 50 mm.
        // pixelsPerMm = 500 / 50 = 10
        val pixelsPerMm = engine.calculatePixelsPerMm(adjustedPixels = 500f)
        assertEquals(10f, pixelsPerMm, 0.001f)
    }

    @Test
    fun `test creating device calibration data model`() {
        val engine = CalibrationEngine(physicalReferenceLengthMm = 85.6f) // Standard credit card width
        
        val calibration = engine.createDeviceCalibration(
            adjustedPixels = 856f,
            deviceManufacturer = "TestMfg",
            deviceModel = "ModelX",
            screenResolution = "1080x1920",
            screenDensity = 2.5f
        )
        
        assertNotNull(calibration)
        assertEquals(10f, calibration.pixelsPerMm, 0.001f)
        assertEquals("MANUAL_REFERENCE_85.6MM", calibration.calibrationMethod)
        assertTrue(calibration.isValid)
        assertEquals("TestMfg", calibration.deviceManufacturer)
    }

    @Test
    fun `test boundary and invalid conditions`() {
        assertThrows(IllegalArgumentException::class.java) {
            CalibrationEngine(-10f)
        }
        
        val engine = CalibrationEngine(50f)
        assertThrows(IllegalArgumentException::class.java) {
            engine.calculatePixelsPerMm(-500f)
        }
    }
}
