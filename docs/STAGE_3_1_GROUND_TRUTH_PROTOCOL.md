# Stage 3.1: Ground Truth Physical Validation Protocol

## 1. Objective
To physically validate the repeatability and baseline bias of the geometric distance estimator under real-world conditions. Specifically, this experiment uses a "Measured IPD" mode to isolate platform intrinsic errors (Camera2 + ML Kit) from population biology variance.

## 2. Experimental Setup
- **Device:** Android Smartphone.
- **Physical Reference (Ground Truth):** Stiff measuring ruler.
- **Environment:** Uniform indoor lighting.
- **Reference Points:**
  - **Start:** Physical glass of the front camera lens.
  - **End:** Outer canthus (outer corner) of the participant's eye (this is approximately coplanar with the pupils, avoiding the 10-15mm offset error caused by measuring to the nose bridge).

## 3. Data Collection Modes

The UI supports explicitly overriding the 63.0 mm population prior.

- **MODE A (POPULATION PRIOR):** Leaves the Subject IPD field blank. Uses 63.0 mm.
- **MODE B (MEASURED IPD):** The tester physically measures their IPD (e.g., in a mirror with a ruler, or a pupillometer) and enters it (e.g., `64.5`) before starting.

## 4. Minimum Experimental Redesign

To maximize scientific value while minimizing manual labor, execute exactly this protocol:

### Phase A: Isolation of Intrinsic Platform Error
**Setup:** Enter your physically MEASURED subject IPD into the UI.
**Execution:** 
Record 10 trials at exactly **400 mm**.
*Purpose:* This establishes the "best case" accuracy of the camera + ML kit combination without biological confounders.

### Phase B: Distance-Dependent Error & Repeatability
**Setup:** Clear the Subject IPD field (returns to POPULATION prior).
**Execution:**
Record 10 independent trials at each distance:
- 300 mm
- 400 mm
- 500 mm
*Note: Physically pull the phone away or cover the lens between each trial to force a track reset.*
*Purpose:* Determines if the pinhole camera geometry breaks down at edges of the working range, and captures the baseline systematic bias (Mode A) caused by the population prior. 

### Phase C: Angle Robustness
**Setup:** Distance exactly 400 mm.
**Execution:**
- Rotate head drastically left/right. Tap "RECORD ANGLE".
- Pitch head drastically up/down. Tap "RECORD ANGLE".
*Purpose:* Verifies that pose gating triggers before 2D IPD foreshortening corrupts the measurement.

## 5. Engineering Acceptance Criteria

This experiment cannot validate "clinical accuracy" because production users will not know their IPD. Instead, it categorizes the engineering viability of the estimator:

- **PASS:**
  - Cross-trial Repeatability (SD of means in Phase B) is `< 15 mm`.
  - Distance-dependent scaling is linear (the bias at 300mm and 500mm differs by `< 5%`).
  - *Action:* The estimator is exceptionally stable. Proceed to Stage 4 (Clinical Scaling Integration).
- **CONDITIONAL:**
  - Repeatability is `< 25 mm`.
  - Bias is large but stable.
  - *Action:* The estimator works but requires careful statistical smoothing or mandatory clinical user-calibration in production. Proceed to Stage 4 with caution.
- **FAIL:**
  - Repeatability is `> 30 mm` (camera distances drift wildly on identical physical holds).
  - *Action:* The ML Kit landmarks or camera metadata are too unstable. Do NOT proceed to Stage 4. Redesign the measurement pipeline.

## 6. Data Integrity
All trials are saved synchronously to CSV upon 1.5-second stability completion. The CSV now strictly records:
`timestamp, device, camera_id, mode, ground_truth_mm, trial_num, mean_dist_mm, median_dist_mm, within_trial_stddev, abs_error, pct_error, bias, yaw, pitch, observed_ipd_px, focal_length_px, ipd_mm_scale, ipd_mode, status`
