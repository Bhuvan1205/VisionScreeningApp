# Phase 2: Calibration and Physical Measurement

## Overview
This document describes the measurement and calibration mathematics powering the SwasthyaTech Vision Screening Prototype. 

To overcome arbitrary Android font scaling and differing screen resolutions, the application employs a deterministic mapping from visual angle to physical millimeter dimensions, which are then converted to pure pixels based on a manually calibrated pixels-per-millimeter ratio.

## Core Mathematical Assumptions & Conventions
Because visual acuity can be interpreted using multiple standards (Snellen, decimal, LogMAR), this implementation specifically formalizes **LogMAR** and standard clinical minutes of arc for reproducible scaling.

### Target Acuity
* Acuity is passed to the engine as a `LogMAR` float.
* **0.0 LogMAR** equals standard 6/6 (20/20) vision.
* **1.0 LogMAR** equals standard 6/60 (20/200) vision.

### Visual Angle (MAR)
* `MAR` (Minimum Angle of Resolution) is the width of a single stroke or gap of the Tumbling E optotype in **minutes of arc** (arcmin).
* `MAR = 10 ^ LogMAR`
* 1 minute of arc = `1/60` of a degree.

### Optotype Geometry
* We assume a standard **Tumbling E** where the total height and width of the letter are exactly `5 * MAR`.
* The stroke width and the gap width are exactly `1 * MAR`.
* The total visual angle subtended by the full letter is `5 * MAR`.

### Physical Dimension Calculation
To calculate the physical size $S$ (in millimeters) of the optotype on a screen viewed from a distance $D$ (in millimeters):
1. Compute the total visual angle in radians: 
   `θ_radians = (5 * MAR / 60) * (π / 180)`
2. The exact physical size on the screen is:
   `S = 2 * D * tan(θ_radians / 2)`

*Example:* 
For LogMAR 0.0 (Snellen 6/6), `MAR = 1`. 
Total angle = 5 arcmin.
At viewing distance `D = 400 mm` (40 cm):
`S = 2 * 400 * tan( (5/60/2) * (π/180) ) = 0.58177 mm`

For LogMAR 1.0 (Snellen 6/60), `MAR = 10`.
Total angle = 50 arcmin.
At viewing distance `D = 400 mm`:
`S = 2 * 400 * tan( (50/60/2) * (π/180) ) = 5.8180 mm`

## Calibration Procedure
The prototype avoids relying on Android device metrics like `DisplayMetrics.xdpi` or `1 dp = 1/160 inch` because OEM implementations are frequently inaccurate on low-cost hardware.

1. **Manual Reference:** The `CalibrationEngine` assumes the user is presented a box/line on the screen alongside a known physical object (e.g., a standard credit card at 85.6 mm or a ruler at 50 mm).
2. **Adjustment:** The user adjusts the box size on screen until it perfectly matches the physical object.
3. **Calculation:** The engine records the final pixel size $P$ of the box.
   `pixelsPerMm = P / Reference_Length_mm`

This `pixelsPerMm` scalar acts as the absolute source of truth for the rest of the application.

## Pixel Conversion Pipeline
Once `pixelsPerMm` and the physical size $S$ are known:
1. `Optotype Size (pixels) = S * pixelsPerMm`
2. `Stroke Size (pixels) = Optotype Size / 5`

This OptotypeDimensions data structure enforces **integer pixel snapping** (added in Stage 2 P0-A). Stroke Size (pixels) is strictly rounded to the nearest integer, and the Optotype Size is forced to Stroke Size * 5. This prevents sub-pixel anti-aliasing in Compose which would otherwise artificially blur the optotype edges and corrupt the threshold measurement on low-DPI displays.

This OptotypeDimensions data structure is then cleanly handed to the UI Phase 3 renderer, completely decoupled from Android font-rendering algorithms.

## Distance Scale Variability
Phase 7 introduces the domain representation of DistanceValidation. The underlying physical scale math S = 2 * D * tan(theta / 2) natively supports variable dynamic distances where D corresponds directly to measuredDistanceMm recorded by the validation engine, ensuring accurate geometric subtension irrespective of explicit 400mm assumptions.

