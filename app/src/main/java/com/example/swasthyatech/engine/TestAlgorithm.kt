package com.example.swasthyatech.engine

import com.example.swasthyatech.data.Direction
import kotlin.random.Random

/**
 * An abstraction for the progression algorithm of a visual-acuity test.
 */
interface TestAlgorithm {
    val version: String
    
    fun getNextAcuity(): Float?
    fun generateNextOrientation(random: Random): Direction
    
    /**
     * @return true if the test has reached a termination condition.
     */
    fun submitResponse(targetOrientation: Direction, userResponse: Direction): Boolean
    
    fun getCurrentThreshold(): Float?
}

/**
 * A 3-Down 1-Up Psychometric Staircase algorithm.
 * 
 * - Starts at a high LogMAR (easy).
 * - Decreases LogMAR by step size after `consecutivePassesRequired` correct answers.
 * - Increases LogMAR by step size after 1 incorrect answer.
 * - If NOT_VISIBLE is selected, immediately acts as an incorrect answer (or triggers reversal).
 * - Stops after a set number of reversals.
 */
class Staircase3Down1UpAlgorithm(
    private val startLogMar: Float = 1.0f,
    private val stepLogMar: Float = 0.1f,
    private val minLogMar: Float = -0.1f,
    private val maxLogMar: Float = 1.0f,
    private val maxReversals: Int = 4,
    private val consecutivePassesRequired: Int = 3
) : TestAlgorithm {

    override val version: String = "STAIRCASE_3_DOWN_1_UP_V2"

    private var currentLogMar: Float = startLogMar
    private var consecutiveCorrect = 0
    private var lastDirection: Int = -1 // 0 for down (improving), 1 for up (worsening), -1 init
    private val reversals = mutableListOf<Float>()

    override fun getNextAcuity(): Float? {
        if (reversals.size >= maxReversals) return null
        return currentLogMar
    }

    override fun generateNextOrientation(random: Random): Direction {
        val orientations = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
        return orientations[random.nextInt(orientations.size)]
    }

    override fun submitResponse(targetOrientation: Direction, userResponse: Direction): Boolean {
        if (userResponse == Direction.NOT_VISIBLE || userResponse != targetOrientation) {
            // Incorrect or Not Visible -> Move UP (worsen acuity)
            consecutiveCorrect = 0
            val newDirection = 1
            if (lastDirection == 0) {
                // Reversal!
                reversals.add(currentLogMar)
            }
            lastDirection = newDirection
            
            // Fast fail immediately if they can't even see the easiest
            if (currentLogMar >= maxLogMar && userResponse == Direction.NOT_VISIBLE) {
                return true // End test, cannot see largest optotype
            }

            currentLogMar = kotlin.math.min(maxLogMar, currentLogMar + stepLogMar)
            currentLogMar = kotlin.math.round(currentLogMar * 10f) / 10f // precision cleanup
        } else {
            // Correct
            consecutiveCorrect++
            if (consecutiveCorrect >= consecutivePassesRequired) {
                // Move DOWN (improve acuity)
                consecutiveCorrect = 0
                val newDirection = 0
                if (lastDirection == 1) {
                    // Reversal!
                    reversals.add(currentLogMar)
                }
                lastDirection = newDirection
                
                if (currentLogMar <= minLogMar) {
                    // Reached absolute hardware/floor limit, can't go lower. Force end.
                    return true
                }

                currentLogMar = kotlin.math.max(minLogMar, currentLogMar - stepLogMar)
                currentLogMar = kotlin.math.round(currentLogMar * 10f) / 10f
            }
        }

        return reversals.size >= maxReversals
    }

    override fun getCurrentThreshold(): Float? {
        if (reversals.isEmpty()) {
            // Fast-fail or floor-hit without any reversals.
            if (currentLogMar <= minLogMar) return minLogMar
            return null // Off-scale (worse than maxLogMar)
        }
        // Standard psychometric threshold estimation: average of last reversals
        val avg = reversals.takeLast(2).average().toFloat()
        return avg // Provide continuous mathematical threshold rather than rounding to 0.1 bins
    }
}


