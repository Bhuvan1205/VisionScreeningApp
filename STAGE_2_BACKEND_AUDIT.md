# Stage 2 Backend & Measurement Architecture Audit

## 1. Current Architecture Overview
The Phase 1 MVP is a modular, offline-first Android application designed to present physically scaled optotypes based on a manual screen calibration. It follows an MVVM architecture, utilizing `kotlinx.serialization` for local JSON persistence. The application is logically divided into:
*   **Data Layer**: `Models.kt`, `SessionRepository`, `CalibrationRepository`.
*   **Measurement/Engine Layer**: `MeasurementEngine` (math), `CalibrationEngine` (px/mm conversion), `DistanceValidationEngine` (state tracker).
*   **Test Logic Layer**: `VisualAcuityTestEngine` (trial execution), `TestAlgorithm` (progression rules).
*   **Presentation Layer**: `SessionViewModel` (flow orchestration), `TestViewModel` (trial binding), `CalibrationViewModel`.

### Explicit Items That Should Not Be Changed
*   **Density Independence Architecture:** The mathematical cancellation of Android system density scaling (`pixelsPerMm` → `totalPixelSize` → `Dp(totalPixelSize / LocalDensity.current.density)` → Compose Canvas) is incredibly robust. It completely insulates the clinical scaling from OS-level font size/display size user changes. This mechanism is mathematically sound and must remain.
*   **Offline-first JSON Persistence:** The direct-to-disk JSON write approach perfectly matches the resource-constrained offline MVP requirements. Do not introduce Room/SQLite or Cloud Sync yet.
*   **Modular Interface Design:** The separation between `TestAlgorithm` and `VisualAcuityTestEngine` is structurally correct and should be preserved.

## 2. End-to-End Mathematical Pipeline & Data Flow

Tracing a single test iteration (`calibration` → `LogMAR`):

1.  **Calibration Box Sizing:** User adjusts a slider matching an on-screen box to a credit card.
    *   *Value:* `currentPixels` (e.g., 300f). **Classification:** User-entered.
    *   *Value:* `referencePhysicalMm` (85.6f). **Classification:** Assumed (Standard ID-1 card).
2.  **Pixels-per-Millimeter:** `CalibrationEngine` divides pixels by the reference.
    *   *Value:* `pixelsPerMm = currentPixels / 85.6f`. **Classification:** Deterministically calculated.
3.  **Viewing Distance Selection:** `SessionViewModel` injects the test distance.
    *   *Value:* `400f` (40 cm). **Classification:** Placeholder / Assumed (Hardcoded).
4.  **Target Acuity Selection:** `MvpStepDownAlgorithm` pulls the current difficulty.
    *   *Value:* `1.0f` LogMAR. **Classification:** Placeholder / Dummy (Predefined list `[1.0, 0.8, 0.6, 0.4, 0.2, 0.0]`, missing standard 0.1 ETDRS intervals).
5.  **Optotype Physical Dimensions:** `MeasurementEngine` computes the angle and size.
    *   *Value:* `marArcmin = 10 ^ LogMAR`. **Classification:** Deterministically calculated.
    *   *Value:* `sizeMm = 2 * 400 * tan(angleRadians / 2)`. **Classification:** Deterministically calculated.
6.  **Pixels:** Converts mm to screen pixels.
    *   *Value:* `totalPixelSize = sizeMm * pixelsPerMm`. **Classification:** Deterministically calculated.
    *   *Value:* `strokePixelSize = totalPixelSize / 5f`. **Classification:** Deterministically calculated (Warning: fractional float).
7.  **Rendered Optotype:** `TumblingERenderer` converts to Compose boundaries.
    *   *Value:* `sizeDp = Dp(totalPixelSize / density)`. **Classification:** Deterministically calculated.
8.  **User Response:** Operator presses directional button.
    *   *Value:* `Direction.UP`. **Classification:** User-entered.
9.  **Threshold:** 2 out of 3 correct responses pass a level; failure terminates the test.
    *   *Value:* `lastPassedAcuity`. **Classification:** Estimated (via weak statistical threshold).
10. **LogMAR Final Result:** The engine maps the LogMAR to a Snellen fraction.
    *   *Value:* `convertLogMarToFraction(lastPassedAcuity)`. **Classification:** Placeholder / Dummy (Hardcoded `when` block).

## 3. Explicit Hardcoded Intersections

### Where is the fixed 40 cm viewing distance assumed?
*   `SessionViewModel.kt`: Initializes the `ScreeningSession` config with `ViewingDistance(targetDistanceMm = 400f)`.
*   `SessionViewModel.kt`: Instantiates `DistanceValidationEngine(targetDistanceMm = 400f)`.
*   `DistanceValidationEngine.kt`: Hardcodes `toleranceMm = 20f` and forces `measuredDistanceMm = targetDistanceMm` when `completeManualGuidance()` is called.
*   `TestViewModel.kt`: Falls back to `session.distanceValidation?.measuredDistanceMm ?: 400f` when initializing the engine.

### Where is LogMAR calculated?
LogMAR is **not calculated** from performance. Instead, performance selects a value from a predefined, hardcoded array.
*   `SessionViewModel.kt`: Hardcodes the list `acuityLevels = listOf(1.0f, 0.8f, 0.6f, 0.4f, 0.2f, 0.0f)`.
*   `MvpStepDownAlgorithm.kt`: Iterates down this list. The final score is simply `lastPassedAcuity` (the last array element they passed).
*   `VisualAcuityTestEngine.kt`: Translates the float into a string via a hardcoded placeholder `convertLogMarToFraction()` rather than a true logarithmic conversion.

## 4. Current Assumptions & Weaknesses

### Algorithmic Weaknesses (Clinically Questionable)
*   **High Guessing Variance:** A 4-choice Tumbling E has a 25% guess probability. The MVP's 2-out-of-3 passing criteria yields a **15.6% chance** of passing a level purely by random guessing. 
*   **No Reversals:** The algorithm immediately terminates upon a failed level without backing up to verify (no psychometric staircase). A single mistaken tap permanently ends the test.
*   **Destructive NOT_VISIBLE Handling:** `NOT_VISIBLE` is evaluated merely as `isCorrect = false`. The engine pointlessly forces the user to complete the remaining trials of the level even after they explicitly stated they cannot see the screen.

### Measurement Uncertainties & Hardware Constraints
*   **Sub-Pixel Aliasing:** `strokePixelSize = totalPixelSize / 5f` results in a Float. On low-DPI screens at 40 cm, 1 MAR may map to a fractional pixel (e.g., 1.83 px). The Compose Canvas applies anti-aliasing to draw this, which visually blurs the optotype. This inadvertently turns the visual-acuity test into a contrast-sensitivity test, invalidating the result.
*   **Parallax & Aspect Ratio Calibration Error:** The UI calibration assumes a perfect 1:1 pixel aspect ratio (square pixels) and relies on the user perfectly aligning a physical card without parallax error.
*   **Rigid Geometry Cap:** Hardcoding 400mm means low-DPI devices cannot test for 6/6 (0.0 LogMAR) vision, because the required pixel size is physically smaller than the device's hardware pixels.

### Persistence Weaknesses
*   **Main-Thread Disk I/O:** `SessionRepository.saveSession` uses `File.writeText()` synchronously on the calling thread. `SessionViewModel` calls this directly from UI events, risking UI stutters and ANRs.
*   **Lack of Atomic Writes:** `File.writeText()` is not atomic. If the Android OS kills the process mid-write, the JSON file is permanently corrupted.
*   **Process Death Vulnerability:** `SessionViewModel` holds the active session in memory. If the app is backgrounded and killed by Android, the ViewModel is destroyed. The session remains permanently orphaned as `IN_PROGRESS` on disk with no way to resume.
*   **Backup/Restore Calibration Invalidation:** If app data is transferred to a new phone via Google Drive or ADB, `CalibrationRepository` blindly loads the old physical scaling JSON, permanently corrupting all measurements on the new device without warning.

## 5. Recommended Architecture & Roadmap

### Recommended Architecture Adjustments
1.  **Dynamic Pre-flight Distance:** `MeasurementEngine` must calculate if a target distance causes `strokePixelSize` to drop below a strict integer threshold (e.g., 1.0 or 2.0 px). If so, it must automatically increase the testing distance to accommodate the hardware limit.
2.  **Integer Pixel Snapping:** `MeasurementEngine` must snap `totalPixelSize` to the nearest multiple of 5 *after* DP conversion to guarantee clean, un-aliased rendering.
3.  **Clinical Staircase Algorithm:** Introduce an ETDRS-style or adaptive 3-down 1-up staircase algorithm supporting full 0.1 LogMAR increments.
4.  **Resilient State/I-O:** Shift repositories to use Coroutines (`Dispatchers.IO`) and `AtomicFile`. Implement `SavedStateHandle` in `SessionViewModel`.

### Prioritized Implementation Roadmap

#### P0 — Scientifically Necessary (Fixing critical measurement invalidations)
1.  **Pixel Snapping & Anti-Alias Prevention:** Modify `MeasurementEngine` to force exact integer multiples of 5 for optotypes to prevent contrast-blurring.
2.  **Dynamic Viewing Distance:** Remove the 400mm hardcode. Calculate minimum safe distance dynamically based on device `pixelsPerMm` and minimum target LogMAR.
3.  **Clinical Test Algorithm:** Replace 3-trial step-down with a statistically sound clinical staircase algorithm with reversals and proper 0.1 LogMAR granularity.
4.  **Hardware Verification on Calibration Load:** Verify `android.os.Build.MODEL` matches the JSON calibration file upon load to prevent silent scaling corruption from device backups.

#### P1 — Important for Robustness (Fixing crashes and data loss)
5.  **Asynchronous & Atomic Persistence:** Refactor `SessionRepository` and `CalibrationRepository` to use `AtomicFile` and `Dispatchers.IO` to prevent JSON corruption and Main-Thread blocking.
6.  **Process Death Recovery:** Integrate `SavedStateHandle` into `SessionViewModel` and allow `VisualAcuityTestEngine` to resume an interrupted `trialHistory`.
7.  **Fast-Fail NOT_VISIBLE:** Update the `TestAlgorithm` interface to accept exact `Direction` responses and instantly break the level/trigger reversal upon `NOT_VISIBLE`.

#### P2 — Useful Enhancements (UX and reporting)
8.  **Orientation Lock:** Add `android:screenOrientation="portrait"` to `AndroidManifest.xml` to prevent visual jumps during testing.
9.  **APK Version Tracking:** Append `BuildConfig.VERSION_CODE` into `ScreeningSession` schema for future data traceability.

#### P3 — Optional / AI Features (Out of scope for current backend hardening)
10. **Automated Camera Distance Estimation:** Integrating front-facing camera ML depth estimation (Phase 7/8).
11. **Voice Input Integration:** Adding speech-to-text semantic matching for directional responses.
