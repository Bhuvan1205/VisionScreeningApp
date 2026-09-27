# STAGE 4: PHYSICAL DISPLAY CALIBRATION

## A. Context & Current Handling
To ensure valid visual acuity screenings, the Tumbling E optotype stroke width must subtend an exact visual angle (e.g., 1 arcminute for 0.0 LogMAR) on the user's retina. This requires the device to accurately project a specific physical height and width (in millimeters) on its display, regardless of the device's screen size or pixel density.

**Current Prototype Implementation:**
*   **System/Navigation-bar Insets:** `MainActivity.kt` uses `enableEdgeToEdge()` combined with `Modifier.safeDrawingPadding()` at the root `Surface`. This guarantees the application coordinate system starts exactly at the usable boundary, free from system UI overlap.
*   **Optotype Placement:** Center-aligned in `TestScreen.kt`. It utilizes Jetpack Compose layout bounds that safely remain within the `safeDrawingPadding`.
*   **API Density:** Android provides `LocalDensity.current.density`, which represents the hardware's logical density scale (dp to px).
*   **Pixel-to-Millimeter Conversion:** `CalibrationScreen.kt` prompts the user to place a physical reference object (like a credit card) on the screen. The user scales a visual box to match. This yields a floating-point `pixelsPerMm` factor stored in `CalibrationRepository`.
*   **Mathematical Loop:** `MeasurementEngine` computes the exact pixel bounds needed for a target physical millimeter size. `TumblingERenderer.kt` takes these raw pixels and divides by `LocalDensity.current.density` to create a Compose `Dp` unit. Compose inherently multiplies this `Dp` unit back by the exact same density factor when rendering the Canvas, resulting in a perfectly preserved raw pixel array.

## B. What Needs to be Measured
1.  **Hardware Pixel Pitch (Physical):** The exact metric width of a single pixel on the display panel.
2.  **Reported Density (Software):** The `xdpi` and `ydpi` values provided by `android.util.DisplayMetrics`.
3.  **Active Display Area:** The precise physical height and width of the illuminated screen bounds.

## C. What Android APIs Provide
*   `android.os.Build.MODEL` and `android.os.Build.MANUFACTURER`
*   `Resources.getSystem().displayMetrics.density` (Logical density factor)
*   `Resources.getSystem().displayMetrics.xdpi` and `ydpi` (Claimed exact physical pixels per inch. *Warning: Frequently inaccurate or faked by OEMs*).
*   `WindowMetrics` (Total screen bounds in pixels, including and excluding window insets).

## D. What Must be Physically Verified with a Ruler
Because Android OEMs often report idealized or generic DPI values rather than exact hardware pitch, the `xdpi` / `ydpi` fields cannot be trusted for clinical accuracy. We must physically verify:
1.  **Calibration UI Accuracy:** When the user matches the visual box to a standard ID card (85.60 mm), does the resulting `pixelsPerMm` yield a mathematically correct pixel pitch for that specific device model?
2.  **Optotype Physical Size:** We must render a full-screen or heavily scaled reference box in the UI, grab a highly accurate digital caliper, and measure the illuminated pixels. If the app commands a 50.0 mm box, it must physically measure 50.0 mm on the glass.

## E. Verification Procedure for Optotypes
1.  Force the `MeasurementEngine` to target exactly `1.0 LogMAR` (10 arcminutes stroke, 50 arcminutes total size) at a heavily constrained distance (e.g., 200mm).
2.  Calculate expected physical millimeter size: `S = 200 * tan(50/60 degrees)`.
3.  Render the optotype on screen.
4.  Use digital calipers to measure the rendered `E` from top-edge to bottom-edge and left-edge to right-edge.
5.  Perform this check across 3 vastly different device models (e.g., a 1080p phone, a 1440p phone, and a tablet).

## F. Acceptance Measurements
*   **Dimensional Tolerance:** The physically measured optotype MUST be within `±0.25 mm` of the mathematical target size. 
*   **Squareness:** The width and height of the `E` must match within `±1 pixel` to ensure the display does not have rectangular pixels or asymmetrical DPI scaling.
*   **Resolution Floor:** The stroke width of the smallest tested LogMAR at the maximum tested distance must exceed `1.0 physical pixel`. If the stroke width drops below 1 pixel, contrast degradation destroys clinical validity.
