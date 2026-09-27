# MILESTONE: DISTANCE ESTIMATOR VALIDATION v1

## A. Current Mathematical Model
The distance estimation engine relies on the thin-lens pinhole camera model. 
The mathematical equation used is:
**Estimated Distance (D_mm) = (IPD_mm_scale * focal_length_px) / observed_ipd_px**

Where:
- **IPD_mm_scale**: The physical assumed distance between the pupils in millimeters.
- **focal_length_px**: The camera's focal length converted to pixel units.
- **observed_ipd_px**: The Euclidean distance between the center of the left and right eyes in the 2D image plane, calculated using ML Kit face landmarks.

## B. Input Variables
- `focalLengthMm` (from `CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS`)
- `sensorWidthMm`, `sensorHeightMm` (from `CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE`)
- `analysisWidthPx`, `analysisHeightPx` (from `ImageProxy` dimensions)
- `leftEye.position.x`, `leftEye.position.y` (from `FaceLandmark.LEFT_EYE`)
- `rightEye.position.x`, `rightEye.position.y` (from `FaceLandmark.RIGHT_EYE`)
- `headEulerAngleX`, `headEulerAngleY` (Pitch and Yaw)

## C. Camera Assumptions
1. **Pinhole Camera Linearity**: Assumes the lens has negligible radial distortion near the center crop.
2. **Focal Length Invariance**: Assumes the optical focal length `f_mm` maps linearly to the `ImageProxy` pixel buffer using the ratio of the maximum image dimension to the maximum physical sensor dimension. 
   *(Note: If the OEM applies an unadvertised digital selfie crop prior to analysis, the true active array size will differ from the physical sensor bounds, inducing a constant scale error).*
3. **Square/Center Crop**: `PreviewView`'s `FILL_CENTER` visually crops the center of the 4:3 camera stream, keeping the geometric center of the ML Kit coordinate system precisely aligned with the visual UI.

## D. Facial-Scale Assumption
- In the current **UNCALIBRATED** mode, the system enforces a strict **Population Prior**: `IPD_mm_scale = 63.0 mm`.
- The mathematical budget assigns a `±5.5%` uncertainty (`σ_scale ≈ 3.5 mm`) based on the standard biological variance of human adult IPD. 
- *Crucially*, this means that individuals with smaller IPDs (e.g., 55mm) will inherently have their distance mathematically overestimated by exactly `(63/55) - 1 = +14.5%` unless calibrated.

## E. Ground-Truth Definition
- **Reference Start**: The optical center of the front-facing camera lens.
- **Reference End**: The facial plane connecting the user's two eyes (bridge of the nose/corneal apex).
- Since the screen glass and the camera lens are physically co-planar, this perfectly mirrors the clinical definition of "Viewing Distance" (Eye-to-Screen).

## F. Known Limitations
- The system correctly outputs high relative temporal stability (jitter < 2mm) but its absolute scale is strictly handcuffed to the 63.0mm population prior.
- Pose estimations > 15° Pitch/Yaw will falsely shrink the projected 2D IPD (`IPD_px`), falsely increasing the reported distance.
- Device OEM digital cropping (like Samsung's wide/narrow selfie mode) may corrupt `focal_length_px` mapping if `CameraCharacteristics` does not accurately report the cropped active array.

---

## G. Experiment Design

### 1. Controlled Accuracy Experiment
**Objective:** Isolate IPD biological variance from random camera noise.
- **Subjects:** 3 distinct individuals (preferably varied facial profiles/IPDs).
- **Distances:** 300, 350, 400, 450, 500 mm.
- **Trials:** 10 independent trials per distance per subject.
- **Protocol:**
  1. Use a physical tape measure/ruler to establish the distance from the camera lens to the bridge of the nose.
  2. The subject must maintain a neutral head pose (0° yaw/pitch).
  3. The subject enters the Ground Truth distance into the app and holds still for the 1.5s stability window.
  4. The app will automatically save the metrics and clear the state.
  5. The subject must physically pull back and re-align for the next trial.
- **Total:** 150 trials minimum.

### 2. Controlled Pose Robustness Experiment
**Objective:** Quantify IPD projection shrinkage under extreme angles.
- **Distance:** Fixed at 400 mm.
- **Conditions:** Neutral, Yaw Left, Yaw Right, Pitch Up, Pitch Down.
- **Trials:** 5 frames/trials per condition per subject.
- **Protocol:** The subject holds the phone at 400mm and rotates their head while the experimenter logs the robustness frames via the UI.

---

## H. Acceptance Criteria

Data will be ingested into `experiment_analysis.py`. We will independently evaluate:

1. **Repeatability:** Within-trial standard deviation (`within_trial_stddev`) must be consistently `< 5.0 mm`.
2. **Absolute Accuracy:** Mean Absolute Error (MAE) across all subjects must be evaluated without arbitrary cutoffs.
3. **Between-Person Variability:** If `Mean Signed Bias` shifts drastically (e.g., > 30 mm) between Subject A and Subject B at the exact same physical distance, the 63mm IPD Population Prior is definitively invalidated for clinical screening.
4. **Pose Robustness:** Signed bias should systematically increase linearly with `1 - cos(yaw)` or `1 - cos(pitch)`.

---

## I. Decision Gate

Based on the Python script outputs, we will execute ONE of the following architectural paths for Stage 4:

- **PATH A (Sufficient Accuracy):** MAPE < 5%, minimal between-subject variation. Proceed to visual acuity validation directly.
- **PATH B (Systematic Scale Bias):** High MAE, but constant scale error across ALL subjects. Investigate `focal_length_px` OEM crop mapping.
- **PATH C (High Between-Subject Variance):** MAE highly dependent on subject. `error_mm` strongly correlates with biological IPD variation. *Action: Implement personalized IPD scale calibration (credit-card or string method).*
- **PATH D (Unstable Jitter):** High within-trial variance (>15mm). Investigate ML Kit landmark jitter or Auto-Focus breathing.
