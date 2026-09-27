package com.example.swasthyatech.engine

import com.example.swasthyatech.data.Direction
import com.example.swasthyatech.data.Eye
import com.example.swasthyatech.data.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VisualAcuityTestEngineTest {

    @Test
    fun `simulate complete right eye test`() {
        val measurementEngine = MeasurementEngine(pixelsPerMm = 10f)
        val algorithm = Staircase3Down1UpAlgorithm()
        // Fixed seed for deterministic randomization
        val random = Random(42) 
        
        val engine = VisualAcuityTestEngine(
            eye = Eye.RIGHT,
            algorithm = algorithm,
            measurementEngine = measurementEngine,
            viewingDistanceMm = 400f,
            random = random
        )

        assertEquals(EngineState.NOT_STARTED, engine.state)
        engine.start()
        
        // --- LEVEL 1 (LogMAR 1.0) ---
        assertEquals(EngineState.WAITING_FOR_RESPONSE, engine.state)
        assertEquals(1.0f, engine.currentAcuityLogMar)
        assertNotNull(engine.currentOptotypeDimensions)
        
        // Trial 1: Correct
        var orientation = engine.currentOrientation!!
        engine.submitResponse(orientation) // Correct
        
        // Trial 2: Correct -> Should pass level
        orientation = engine.currentOrientation!!
        engine.submitResponse(orientation) // Correct
        
        // --- LEVEL 2 (LogMAR 0.5) ---
        assertEquals(0.5f, engine.currentAcuityLogMar)
        
        // Trial 3: Correct
        orientation = engine.currentOrientation!!
        engine.submitResponse(orientation) // Correct
        
        // Trial 4: Incorrect
        orientation = engine.currentOrientation!!
        val wrongResponse = if (orientation == Direction.UP) Direction.DOWN else Direction.UP
        engine.submitResponse(wrongResponse) // Incorrect
        
        // Trial 5: Correct -> Should pass level
        orientation = engine.currentOrientation!!
        engine.submitResponse(orientation) // Correct

        // --- LEVEL 3 (LogMAR 0.0) ---
        assertEquals(0.0f, engine.currentAcuityLogMar)
        
        // Trial 6: Incorrect
        orientation = engine.currentOrientation!!
        engine.submitResponse(Direction.UNKNOWN) // Incorrect
        
        // Trial 7: Incorrect -> Should fail level and end test
        orientation = engine.currentOrientation!!
        engine.submitResponse(Direction.UNKNOWN) // Incorrect
        
        assertEquals(EngineState.COMPLETED, engine.state)
        
        val result = engine.getEyeTestResult()
        assertEquals(Eye.RIGHT, result.eye)
        assertEquals(TestStatus.COMPLETED, result.status)
        assertEquals(7, result.trials.size)
        // Passed 1.0 and 0.5. Failed 0.0. So score is 0.5
        assertEquals(0.5f, result.calculatedAcuity) 
    }
}



