# Stage 3.1 Distance Calibration Report

## Executive Summary
This document summarizes the scientific correction applied in Stage 3.1 to eliminate generic camera and biological assumptions from the distance estimation subsystem. 

## 1. Assumptions Removed
1. **Generic 60Â° FOV:** `FaceDistanceAnalyzer` no longer relies on a hardcoded 60Â° assumption if accurate camera metadata is available. The analyzer accepts `CameraMetadata`, computing the exact effective focal length in pixels directly from the device's physical sensor width and optical focal length.
2. **Generic 63mm IPD:** The `DistanceEstimationEngine` no longer pretends the standard biological prior of 63mm Interpupillary Distance (IPD) is a perfect constant. It is now treated as an explicit scale uncertainty model.

## 2. Exact Files Changed
- `Models.kt`: Added `CameraMetadata` and `DistanceValidationLog` structures.
- `FaceDistanceAnalyzer.kt`: Accepts `CameraMetadata?` to compute physically accurate $f_{px}$ over the image frame.
- `DistanceEstimationEngine.kt`: Tracks `lastIpdPx` and `lastFocalLengthPx` to support on-the-fly geometric calibration. Distinguishes calibrated scale uncertainty (~1%) from population prior uncertainty (~5.5%).
- `DistanceValidationEngine.kt`: Upon user completion of `manualGuidance()` (physically holding the device at 400mm), it automatically calls `calibrateUsingLastFrame()` to extract the exact personalized `personSpecificIpdMm` of the participant.
- `DistanceEstimationEngineTest.kt`: Unit tests verifying that calibrated users achieve high confidence and uncalibrated users accurately reflect high ($\pm23$mm) uncertainty.

## 3. Final Distance Equation
If `CameraMetadata` is available:
$f_{px} = focalLength_{mm} \cdot \left(\frac{ImageWidth_{px}}{SensorWidth_{mm}}\right)$

$D_{mm} = \frac{IPD_{mm} \cdot f_{px}}{IPD_{px}}$

*Note: If the user explicitly calibrates the distance using manual guidance at a known target ($D_{target}$), $IPD_{mm}$ is dynamically calculated as:*
$IPD_{mm} = \frac{D_{target} \cdot IPD_{px}}{f_{px}}$

## 4. Uncertainty Model
The `DistanceEstimationEngine` explicitly decouples tracking noise from biological scale uncertainty.
**Total Uncertainty ($\Delta D$)** = $\sigma_{temporal} + \sigma_{scale}$
- **$\sigma_{temporal}$**: The standard deviation of the raw distance estimates across the 15-frame circular buffer.
- **$\sigma_{scale}$**: The systemic scaling error.
  - *If Uncalibrated:* Uses the 63mm biological prior. Standard human variance is $\approx 3.5$mm, representing an explicit $5.5\%$ scale uncertainty ($\sigma_{scale} = D_{median} \cdot 0.055$).
  - *If Calibrated:* The user has physically measured the target distance (e.g., 400mm string). Due to human error in the string hold, we allocate an explicit $1\%$ residual uncertainty ($\sigma_{scale} = D_{median} \cdot 0.01$).

Confidence is scaled inversely based on this mathematically rigorous uncertainty limit. A stable but systematically biased (uncalibrated) measurement will *not* receive $1.0$ confidence merely because its frame-to-frame variance is low.

## 5. Tests Performed
- **`test valid smoothing and uncertainty uncalibrated`**: Verifies the estimator restricts confidence and accurately reports $>23$mm uncertainty for a standard 400mm uncalibrated hold.
- **`test calibration significantly reduces uncertainty`**: Verifies that calling `calibrateUsingLastFrame()` slashes the reported uncertainty from $\sim 5.5\%$ to $1\%$ and boosts confidence.
- Remaining bounding, rejection, and temporal-smoothing tests were preserved and passed.

## 6. Build Result
Codebase successfully compiles (`assembleDebug`) and passes all unit tests, verifying type safety and mathematical implementation of the new metadata and uncertainty models.

## 7. Remaining Scientific Limitations
- If a low-end device fails to supply `Camera2` hardware characteristics (or if it reports generic legacy metadata), the system safely falls back to the generic 60Â° FOV approximation and logs the `cameraMetadata` as `null` in the experimental validation logs.
- The system assumes the user's head is directly facing the camera. Significant yaw/pitch will artificially shorten the 2D $IPD_{px}$ distance, causing the system to over-estimate the physical distance.

## 8. Physical Device Measurement
Physical-device measurement is **still strictly required** to experimentally validate the end-to-end geometry on real hardware. The added `DistanceValidationLog` (Step 7) enables collection of absolute and relative error across multiple known distances (300mm to 600mm) using ADB or developer modes, without altering the clinical screening UI.
