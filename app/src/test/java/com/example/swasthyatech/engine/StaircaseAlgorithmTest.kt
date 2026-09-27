package com.example.swasthyatech.engine

import com.example.swasthyatech.data.Direction
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class StaircaseAlgorithmTest {

    @Test
    fun testPerfectObserverReachesFloor() {
        val algo = Staircase3Down1UpAlgorithm(startLogMar = 1.0f, minLogMar = -0.1f)
        
        // Emulate answering correctly until the test terminates
        var isDone = false
        var currentLogMar: Float? = algo.getNextAcuity()
        
        while (!isDone && currentLogMar != null) {
            val target = algo.generateNextOrientation(Random)
            isDone = algo.submitResponse(targetOrientation = target, userResponse = target)
            currentLogMar = algo.getNextAcuity()
        }
        
        assertTrue(isDone)
        assertEquals(-0.1f, algo.getCurrentThreshold() ?: 100f, 0.001f)
    }

    @Test
    fun testCompletelyBlindObserver() {
        val algo = Staircase3Down1UpAlgorithm(startLogMar = 1.0f, maxLogMar = 1.0f)
        
        val target = algo.generateNextOrientation(Random)
        // Submit NOT_VISIBLE at the very first (easiest) level
        val isDone = algo.submitResponse(targetOrientation = target, userResponse = Direction.NOT_VISIBLE)
        
        assertTrue(isDone)
        assertNull(algo.getCurrentThreshold())
    }
}
