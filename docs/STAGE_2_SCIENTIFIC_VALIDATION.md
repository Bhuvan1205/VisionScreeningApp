# Stage 2 Scientific Validation Report

## Executive Verdict
The Stage 2 architecture successfully derives the LogMAR visual-acuity result through a continuous mathematical pipeline rooted in physical measurement geometry and a standard psychometric thresholding algorithm. 

**The LogMAR score is absolutely NOT a disguised lookup value.** It is directly calculated as the averaged continuous threshold of a 3-down 1-up clinical staircase, which is driven entirely by the geometric scaling of raw on-screen integer pixels determined by the distance and calibration variables.

A concrete mathematical bug was discovered in `Staircase3Down1UpAlgorithm` regarding edge-case threshold reporting (floor-hits returning `null` instead of the floor LogMAR). This bug was fixed, tested, and pushed before final validation.

---

## 1. Exact Mathematical Pipeline Trace
The pipeline deterministically maps user responses to continuous LogMAR without reliance on static dummy arrays:
1. **Calibration:** `pixelsPerMm = calibratedPixels / referenceMm` (e.g., 300px / 85.6mm = 3.5 px/mm).
2. **Optotype Target:** The algorithm dictates a `currentLogMar` target.
3. **Viewing Distance:** Computed dynamically to guarantee the hardware can render the stroke. `minDistanceMm = (5 / pixelsPerMm) / (2 * tan(angle/2))`. A `negotiatedDistanceMm` is locked in.
4. **Physical Size:** `marArcmin = 10^LogMAR`. `sizeMm = 2 * negotiatedDistance * tan((5 * marArcmin/60 * PI/180)/2)`.
5. **Rendered Pixels:** `exactStrokePx = (sizeMm * pixelsPerMm) / 5`. `renderedStroke = round(exactStrokePx)`.
6. **User Response:** Matched against requested orientation.
7. **Staircase State:** Response fed to `Staircase3Down1UpAlgorithm`.
8. **Threshold & Final LogMAR:** Averages the final two continuous LogMAR reversals.

---

## 2. Formula-by-Formula Verification
Equations in `MeasurementEngine.kt` were cross-referenced against standard ophthalmic physics:
* **MAR Conversion:** `10.0.pow(targetAcuityLogMar.toDouble())` âœ… *Correct. LogMAR = log10(MAR), thus MAR = 10^LogMAR.*
* **Angle calculation:** `(5.0 * marArcmin / 60.0) * (PI / 180.0)` âœ… *Correct. 5x MAR total width. Converted to degrees (divide by 60), then to radians (multiply by PI/180).*
* **Physical Dimension:** `2.0 * D * tan(angleRadians / 2.0)` âœ… *Correct isosceles triangle trigonometry.*

---

## 3. Staircase Algorithm Verification
The implemented `Staircase3Down1UpAlgorithm`:
* **Difficulty Scaling:** Steps down (decreases LogMAR by 0.1, making it harder) after **3 consecutive** correct responses. Steps up (increases by 0.1, making it easier) after **1** incorrect response.
* **Target Threshold:** This specific design mathematically converges on the 79.4% probability-of-seeing threshold.
* **Reversals:** A reversal is explicitly tracked when the direction of difficulty change flips (Down -> Up, or Up -> Down).
* **Threshold Calculation:** The final threshold is the raw, continuous mathematical average of the last 2 reversals: `avg = reversals.takeLast(2).average()`. It does NOT round to predefined bins anymore.

### Handling of `NOT_VISIBLE`
* Treated as an immediate incorrect response, resetting the consecutive-correct counter to 0 and forcing the difficulty Up.
* If `NOT_VISIBLE` is pressed at the maximum possible LogMAR (easiest level), it explicitly triggers a fast-fail, terminating the test instantly and yielding a `null` (off-scale/blind) threshold.

---

## 4. Distance Analysis
* **A. Calculated Distance:** The algorithm computes `minDistanceMm` based on `minAcuity = -0.1 LogMAR`.
* **B. Instructed Distance:** `negotiatedDistanceMm` takes `max(400, ceil(minDistance / 10)*10)`. If the device has low DPI, the app forces a larger instructed distance (e.g., 650mm).
* **C. Physically Measured Distance:** Currently bounded by `DistanceValidationEngine`, which acts as a manual fallback. The operator is instructed to hold it at `negotiatedDistanceMm`, and they tap a button to confirm this physical distance is maintained. (Future Phase 7 adds camera-estimation).

---

## 5. Five Simulated Response Traces

*All simulations start at 1.0 LogMar, step = 0.1.*

**Trace 1: Consistently Correct (Perfect Observer)**
* Target: 1.0... 0.9... down to -0.1 (3 passes each).
* At -0.1, 3 passes -> difficulty would go to -0.2, but hits `minLogMar`. Test terminates.
* **Result:** Threshold = -0.1 LogMAR (Floor hit).

**Trace 2: Consistently Incorrect (Blind Observer)**
* Target 1.0 -> 1 Incorrect.
* Ceiling hit.
* **Result:** Reversals = 0. Threshold = `null`.

**Trace 3: Standard Reversals (Threshold around 0.3)**
* 1.0 down to 0.2 (All correct).
* 0.2 (Incorrect). Reversal 1 at 0.2. Moves up to 0.3.
* 0.3 (Correct x3). Reversal 2 at 0.3. Moves down to 0.2.
* 0.2 (Incorrect). Reversal 3 at 0.2. Moves up to 0.3.
* 0.3 (Correct x3). Reversal 4 at 0.3. Test Stops.
* **Result:** Average of last 2 reversals (0.2, 0.3) = **0.25 LogMAR**.

**Trace 4: Alternating Guesses (High Variance)**
* 0.5 (Incorrect) -> Reversal 1 @ 0.5 -> Up to 0.6.
* 0.6 (Correct x1, Incorrect x1) -> No reversal since it was already moving Up. -> Up to 0.7.
* 0.7 (Correct x3) -> Reversal 2 @ 0.7 -> Down to 0.6.
* 0.6 (Incorrect) -> Reversal 3 @ 0.6 -> Up to 0.7.
* 0.7 (Correct x3) -> Reversal 4 @ 0.7. Test Stops.
* **Result:** Average of (0.6, 0.7) = **0.65 LogMAR**.

**Trace 5: The `NOT_VISIBLE` Fast-Fail**
* 0.3 (Correct x3).
* 0.2 (`NOT_VISIBLE` response). Evaluates as Incorrect.
* Reversal at 0.2. Target moves Up to 0.3.

---

## 6. Pixel Quantization Error
Because fractional pixel strokes cause aliasing, Stage 2 introduced Integer Snapping (`round(exactStrokePx)`). 
**Analysis at limit:**
Target: -0.1 LogMAR at 400mm on a ~300 DPI screen (`pixelsPerMm` = 11.8).
- Mathematical size: 0.463 mm = 5.46 raw pixels total (stroke = 1.09px).
- Snapped Stroke = 1px.
- Snapped Total = 5px.
- **Quantization Error:** (5 - 5.46) / 5.46 = **-8.4%**.
This sub-10% error is clinically acceptable and far superior to the ~30% contrast degradation caused by anti-aliasing.

---

## 7. Remaining Measurement Uncertainties
1. **Calibration Accuracy:** Still depends on the user manually matching a credit card. Parallax or aspect-ratio distortion during calibration permanently skews `pixelsPerMm`.
2. **Physical Distance Drift:** Without active camera tracking, if a user leans forward during the test, the visual angle increases, falsely improving their LogMAR score.
3. **Glare/Luminance:** MVP does not standardize screen brightness.

## Summary Conclusion
The current implementation measures visual acuity geometrically and determines thresholds psychometrically. The math is verifiable and correct. We are ready for Stage 3 (Phase 7): ML distance tracking.
