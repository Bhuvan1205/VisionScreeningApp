package com.example.swasthyatech.ui.optotype

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.swasthyatech.data.Direction

/**
 * A perfectly deterministic Canvas-based renderer for the Tumbling E optotype.
 * Uses exact integer pixel dimensions provided by the MeasurementEngine and disables
 * anti-aliasing to prevent contrast degradation on low-DPI displays (Stage 2 P0-A).
 */
@Composable
fun TumblingERenderer(
    direction: Direction,
    totalPixelSize: Int,
    strokePixelSize: Int,
    modifier: Modifier = Modifier
) {
    // Convert raw pixels to Dp for Compose sizing constraints, preserving exact dimension mathematically.
    val density = LocalDensity.current.density
    val sizeDp = Dp(totalPixelSize.toFloat() / density)

    // A Paint object with anti-aliasing explicitly disabled
    val aliasFreePaint = androidx.compose.ui.graphics.Paint().apply {
        isAntiAlias = false
        color = Color.Black
    }

    Canvas(
        modifier = modifier.size(sizeDp)
    ) {
        val rotationDegrees = when (direction) {
            Direction.RIGHT -> 0f
            Direction.DOWN -> 90f
            Direction.LEFT -> 180f
            Direction.UP -> 270f
            Direction.UNKNOWN -> 0f 
            Direction.NOT_VISIBLE -> 0f 
        }

        rotate(degrees = rotationDegrees) {
            // Draw into the raw Canvas to use the alias-free Paint
            drawIntoCanvas { canvas ->
                val strokeF = strokePixelSize.toFloat()
                val totalF = totalPixelSize.toFloat()

                // 1. Vertical Spine (Left side)
                canvas.drawRect(
                    0f, 0f, strokeF, totalF,
                    aliasFreePaint
                )

                // 2. Top Horizontal Bar
                canvas.drawRect(
                    strokeF, 0f, totalF, strokeF,
                    aliasFreePaint
                )

                // 3. Middle Horizontal Bar
                canvas.drawRect(
                    strokeF, strokeF * 2, totalF, strokeF * 3,
                    aliasFreePaint
                )

                // 4. Bottom Horizontal Bar
                canvas.drawRect(
                    strokeF, strokeF * 4, totalF, strokeF * 5,
                    aliasFreePaint
                )
            }
        }
    }
}


