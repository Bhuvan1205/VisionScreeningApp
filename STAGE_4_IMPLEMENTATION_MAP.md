# Stage 4 Implementation Map: Production Integration

## A. Current Production Flow
1. `AppScreen.HOME` -> Start Session
2. `AppScreen.INTRO` -> User Instructions
3. `AppScreen.DISTANCE_CHECK` -> Currently hijacked by the developer validation UI.
4. `AppScreen.PRACTICE` -> Tumbling E Practice.
5. `AppScreen.TESTING` -> Right Eye Test.
6. `AppScreen.EYE_TRANSITION` -> Switch eyes.
7. `AppScreen.DISTANCE_CHECK` -> Re-validate distance for left eye.
8. `AppScreen.TESTING` -> Left Eye Test.
9. `AppScreen.RESULT` -> Final LogMAR result display.

## B. Exact Integration Points
1. **`SessionViewModel.kt`:** Manages `distanceValidationEngine`. Holds the state across the entire session.
2. **`DistanceValidationScreen.kt`:** The UI layer. Must be converted from a developer logging screen to a user-facing guidance screen that drives `DistanceValidationEngine`.
3. **`TestViewModel.kt` & `VisualAcuityTestEngine.kt`:** Already set up to read `session.distanceValidation?.measuredDistanceMm` (or target if null). This connection is correct.
4. **`MeasurementEngine.kt`:** Already uses the injected viewing distance to calculate `OptotypeDimensions`. This is correct.
5. **`Staircase3Down1UpAlgorithm.kt`:** Already determines the threshold based on responses. This is correct.

## C. Missing Connections & Flaws to Fix
1. **Bypassing Camera:** `SessionViewModel.completeDistanceValidation()` currently hardcodes a call to `distanceValidationEngine.completeManualGuidance()`, which overwrites the camera estimate with a manual fallback. This must be fixed to respect the camera's validation.
2. **User Interface:** `DistanceValidationScreen.kt` needs a total UI overhaul to be patient-facing (TOO_CLOSE/TOO_FAR guidance) rather than logging developer metrics to LogCat.
3. **Calibration Trigger:** The user needs a clear button to trigger `distanceValidationEngine.performScaleCalibration(400f)` once they are holding the string/phone at the target distance, which sets the personalized IPD scale instead of the population prior.
4. **State Linking:** `DistanceValidationScreen` must use the `DistanceValidationEngine` instance owned by `SessionViewModel`, rather than instantiating its own isolated `DistanceEstimationEngine`.

## D. Minimal Files That Must Change
1. `SessionViewModel.kt` (Fix `completeDistanceValidation` logic).
2. `AppScreenHost.kt` (Pass `SessionViewModel` to `DistanceValidationScreen`).
3. `DistanceValidationScreen.kt` (Complete UI rewrite for patient flow, binding to `DistanceValidationEngine`).

## E. Blockers
None. The backend engines (`DistanceEstimationEngine`, `DistanceValidationEngine`, `MeasurementEngine`) are fully functional and mathematically sound. The only requirement is to wire the UI layer correctly to these engines without bypassing them.
