package com.example.swasthyatech.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MeasurementEngineTest {

    private val pixelsPerMm = 10f // Simple scale: 10 pixels = 1 mm
    private val engine = MeasurementEngine(pixelsPerMm)

    @Test
    fun `test mm to pixels conversion`() {
        assertEquals(50f, engine.mmToPixels(5f), 0.001f)
        assertEquals(0f, engine.mmToPixels(0f), 0.001f)
    }

    @Test
    fun `test pixels to mm conversion`() {
        assertEquals(5f, engine.pixelsToMm(50f), 0.001f)
        assertEquals(0f, engine.pixelsToMm(0f), 0.001f)
    }

    @Test
    fun `test round trip conversion`() {
        val originalMm = 15.5f
        val pixels = engine.mmToPixels(originalMm)
        val resultMm = engine.pixelsToMm(pixels)
        assertEquals(originalMm, resultMm, 0.001f)
    }

    @Test
    fun `test optotype physical size calculation - 6_6 vision (LogMAR 0,0)`() {
        // Target: LogMAR 0.0 (Snellen 6/6 or 20/20)
        // MAR = 10^0 = 1 arcmin. Total size = 5 arcmin.
        // Distance = 400 mm
        // 5 arcmin = 5/60 degrees.
        // Size = 2 * 400 * tan( (5/60/2) * pi / 180 ) = 0.58177... mm
        val sizeMm = engine.calculateOptotypePhysicalSizeMm(targetAcuityLogMar = 0.0f, viewingDistanceMm = 400f)
        assertEquals(0.58177f, sizeMm, 0.0001f)
    }

    @Test
    fun `test optotype physical size calculation - LogMAR 1,0`() {
        // Target: LogMAR 1.0 (Snellen 6/60 or 20/200)
        // MAR = 10^1 = 10 arcmin. Total size = 50 arcmin.
        // Distance = 400 mm
        // 50 arcmin = 50/60 degrees = 0.8333... degrees
        // Size = 2 * 400 * tan( (0.8333/2) * pi / 180 ) = 5.8180... mm
        val sizeMm = engine.calculateOptotypePhysicalSizeMm(targetAcuityLogMar = 1.0f, viewingDistanceMm = 400f)
        assertEquals(5.8180f, sizeMm, 0.001f)
    }

    @Test
    fun `test optotype pixel dimension wrapping`() {
        // 400mm, LogMAR 1.0 -> 5.8180 mm physical size
        // pixelsPerMm = 10
        // Expected pixel size = 58.180
        val dims = engine.calculateOptotypePixelDimensions(targetAcuityLogMar = 1.0f, viewingDistanceMm = 400f)
        assertEquals(5.818f, dims.requested.exactPhysicalSizeMm, 0.01f)
        assertEquals(58.18f, dims.requested.exactTotalPixelSize, 0.1f)
        assertEquals(58.18f / 5f, dims.requested.exactStrokePixelSize, 0.1f)
    }

    @Test
    fun `test optotype dimensions across variable validated distances`() {
        val distances = listOf(300f, 350f, 400f, 450f, 500f)
        val expectedSizesMm = listOf(
            0.4363f, // 300mm -> 0.4363 mm
            0.5090f, // 350mm -> 0.5090 mm
            0.5818f, // 400mm -> 0.5818 mm
            0.6545f, // 450mm -> 0.6545 mm
            0.7272f  // 500mm -> 0.7272 mm
        )
        for (i in distances.indices) {
            val sizeMm = engine.calculateOptotypePhysicalSizeMm(targetAcuityLogMar = 0.0f, viewingDistanceMm = distances[i])
            assertEquals(expectedSizesMm[i], sizeMm, 0.0001f)
        }
    }

    @Test
    fun `test invalid inputs`() {
        assertThrows(IllegalArgumentException::class.java) {
            MeasurementEngine(0f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.mmToPixels(-5f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.calculateOptotypePhysicalSizeMm(0f, -400f)
        }
    }
}


