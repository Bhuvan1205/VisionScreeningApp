package com.example.swasthyatech.engine

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.tan

/**
 * Core scientific measurement utility for the SwasthyaTech Vision Screening Prototype.
 * 
 * Engineering Assumptions / Clinical Conventions:
 * - Target Acuity is specified in LogMAR.
 * - LogMAR = log10(MAR), where MAR is the Minimum Angle of Resolution (stroke width) in minutes of arc.
 * - A standard Tumbling E optotype has a total height and width of 5 * MAR.
 * - 1 minute of arc (arcmin) = 1/60 degree.
 * - For a viewing distance D, the physical size S of an object subtending visual angle theta is:
 *   S = 2 * D * tan(theta / 2). For small angles, S = D * tan(theta).
 */
class MeasurementEngine(
    val pixelsPerMm: Float
) {
    init {
        require(pixelsPerMm > 0) { "Pixels per millimeter must be strictly positive." }
    }

    /**
     * Converts a physical length in millimeters to pixels.
     */
    fun mmToPixels(mm: Float): Float {
        require(mm >= 0) { "Physical dimension cannot be negative." }
        return mm * pixelsPerMm
    }

    /**
     * Converts pixels to a physical length in millimeters.
     */
    fun pixelsToMm(pixels: Float): Float {
        require(pixels >= 0) { "Pixel dimension cannot be negative." }
        return pixels / pixelsPerMm
    }

    /**
     * Calculates the minimum viewing distance required to ensure that the optotype stroke width
     * is at least 1 pixel (preventing subpixel rendering/loss of contrast).
     *
     * @param minTargetAcuityLogMar The smallest acuity target that needs to be tested.
     * @return The minimum distance in millimeters.
     */
    fun calculateMinimumViewingDistance(minTargetAcuityLogMar: Float): Float {
        // We want strokePixelSize >= 1.0f
        // strokePixelSize = (sizeMm * pixelsPerMm) / 5f
        // So sizeMm >= 5f / pixelsPerMm
        val minSizeMm = 5f / pixelsPerMm
        
        val marArcmin = 10.0.pow(minTargetAcuityLogMar.toDouble())
        val totalAngleArcmin = 5.0 * marArcmin
        val angleRadians = (totalAngleArcmin / 60.0) * (PI / 180.0)
        
        // sizeMm = 2 * D * tan(angleRadians / 2)
        // D = sizeMm / (2 * tan(angleRadians / 2))
        val minDistanceMm = minSizeMm / (2.0 * tan(angleRadians / 2.0))
        return minDistanceMm.toFloat()
    }

    /**
     * Calculates the required physical size (height/width) of a standard Tumbling E optotype
     * in millimeters for a given LogMAR target acuity and viewing distance.
     * 
     * @param targetAcuityLogMar The target visual acuity in LogMAR.
     * @param viewingDistanceMm The distance from the eye to the screen in millimeters.
     * @return Physical size in millimeters of the full optotype (5x MAR).
     */
    fun calculateOptotypePhysicalSizeMm(targetAcuityLogMar: Float, viewingDistanceMm: Float): Float {
        require(viewingDistanceMm > 0) { "Viewing distance must be strictly positive." }
        
        // MAR = 10 ^ LogMAR
        val marArcmin = 10.0.pow(targetAcuityLogMar.toDouble())
        
        // Total optotype size is 5 * MAR
        val totalAngleArcmin = 5.0 * marArcmin
        
        // Convert arcmin to radians
        // 1 arcmin = 1/60 degree. 1 degree = PI / 180 radians.
        val angleRadians = (totalAngleArcmin / 60.0) * (PI / 180.0)
        
        // S = 2 * D * tan(theta / 2)
        val sizeMm = 2.0 * viewingDistanceMm * tan(angleRadians / 2.0)
        
        return sizeMm.toFloat()
    }

    /**
     * Calculates the required size in pixels for the Tumbling E optotype.
     * Enforces integer pixel snapping to prevent sub-pixel anti-aliasing which
     * corrupts visual acuity measurements.
     * 
     * @param targetAcuityLogMar The target visual acuity in LogMAR.
     * @param viewingDistanceMm The distance from the eye to the screen in millimeters.
     * @return Optotype dimension structure for rendering.
     */
    fun calculateOptotypePixelDimensions(targetAcuityLogMar: Float, viewingDistanceMm: Float): OptotypeDimensions {
        val physicalSizeMm = calculateOptotypePhysicalSizeMm(targetAcuityLogMar, viewingDistanceMm)
        val exactTotalPixelSize = mmToPixels(physicalSizeMm)
        val exactStrokePixelSize = exactTotalPixelSize / 5f
        
        val requested = RequestedOptotypeDimensions(
            exactPhysicalSizeMm = physicalSizeMm,
            exactTotalPixelSize = exactTotalPixelSize,
            exactStrokePixelSize = exactStrokePixelSize
        )

        // Snap to nearest integer pixel for the stroke to avoid anti-aliasing
        val snappedStroke = kotlin.math.round(exactStrokePixelSize).toInt()
        val finalStroke = if (snappedStroke < 1) 1 else snappedStroke
        val finalTotal = finalStroke * 5

        val errorPixels = (finalTotal - exactTotalPixelSize)
        val errorPercentage = if (exactTotalPixelSize > 0) (errorPixels / exactTotalPixelSize) * 100f else 0f

        val rendered = RenderedOptotypeDimensions(
            totalPixelSize = finalTotal,
            strokePixelSize = finalStroke,
            errorPixels = errorPixels,
            errorPercentage = errorPercentage,
            isRepresentable = finalStroke >= 1 && kotlin.math.abs(errorPercentage) <= 10.0f // 10% tolerance rule of thumb
        )

        return OptotypeDimensions(requested, rendered)
    }
}

/**
 * The mathematically exact requested dimensions.
 */
data class RequestedOptotypeDimensions(
    val exactPhysicalSizeMm: Float,
    val exactTotalPixelSize: Float,
    val exactStrokePixelSize: Float
)

/**
 * The integer-snapped renderable dimensions.
 */
data class RenderedOptotypeDimensions(
    val totalPixelSize: Int, // Must be an exact multiple of 5
    val strokePixelSize: Int,
    val errorPixels: Float,
    val errorPercentage: Float,
    val isRepresentable: Boolean
)

/**
 * Data structure exposing required dimensions to the rendering layer.
 */
data class OptotypeDimensions(
    val requested: RequestedOptotypeDimensions,
    val rendered: RenderedOptotypeDimensions
)
