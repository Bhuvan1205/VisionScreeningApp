# Stage 3.1 Physical Distance Validation Protocol

> [!IMPORTANT]
> This protocol defines a physical experiment. The distance estimator is NOT validated until this experiment is performed and documented. Compilation success is not experimental validation.

## Objective
Empirically measure the geometric accuracy and precision of the `DistanceEstimationEngine` on real physical devices. Collect data to characterize:
- Systematic bias (offset from true distance)
- Random error (frame-to-frame variability)
- Failure rates at each distance
- Effect of calibration vs. population prior
- Effect of Camera2 metadata availability

## Required Materials
- 1× Android smartphone (front camera, API 24+)
- 1× Metal ruler or rigid measuring tape (±1mm accuracy)
- 1× Stable phone mount (tripod, box, or foam block — must hold phone motionless)
- 1× Well-lit indoor environment (≥300 lux, no strong backlight)
- 1× Stationary human participant or standardized face photograph printed at life-size (185×140mm face approximation)

## Measurement Distances
Collect data at exactly:

| Ground Truth Distance | Notes |
|---|---|
| 300 mm | Below nominal 400mm target (expect TOO_CLOSE classification) |
| 350 mm | Low boundary of acceptable range |
| 400 mm | Nominal test distance |
| 450 mm | Mid acceptable range |
| 500 mm | High boundary |
| 600 mm | Above range (expect TOO_FAR classification) |

Measure each distance independently with the ruler from the **front glass surface of the phone** to the participant's eyes (bridge of nose is acceptable if eyes are not directly measurable). Record to ±2mm.

## Procedure

### Setup
1. Mount the phone vertically (portrait) on the stable mount.
2. Position the participant's face or printed face card at the first distance.
3. Independently verify the distance with the ruler.
4. Enable developer/logging mode in the app (when implemented).

### Data Collection (per distance)
1. Allow the estimator to warm up for 5 seconds before recording.
2. Record 60 consecutive `DistanceEstimate` outputs (approximately 2 seconds at 30fps).
3. Note: number of frames rejected (INVALID_HEAD_POSE, INVALID_FACE_GEOMETRY, etc.).
4. Note: whether Camera2 metadata was available (`cameraIntrinsicsAvailable`).

### Repeat
- Perform each distance measurement **3 times** with the participant briefly moving away between trials.
- Repeat the full set with scale calibration applied at 400mm before each set (second pass).

## Metrics to Report per Distance

| Metric | Formula |
|---|---|
| Mean estimated distance | `mean(estimatedDistanceMm)` |
| Median estimated distance | `median(estimatedDistanceMm)` |
| Systematic error (bias) | `median(estimated) - groundTruth` |
| Relative bias | `bias / groundTruth × 100%` |
| Standard deviation | `std(estimatedDistanceMm)` |
| Valid frame rate | `count(VALID) / total frames` |
| Rejection breakdown | Count per status code |
| Uncertainty accuracy | `Does reportedUncertainty ≥ |bias|?` |
| Calibration improvement | Bias reduction after scale calibration |

## Data Recording Template (per trial)

```
Date:
Device:            [Manufacturer, Model, Android version]
CameraIntrinsics:  [Available / Unavailable; focalLengthMm, sensorWidthMm if known]
Calibration:       [None / Scale-calibrated at 400mm]
GroundTruth_mm:
Trial:             [1 / 2 / 3]
Frames collected:  60
  mean_mm:
  median_mm:
  std_mm:
  bias_mm:         [median - groundTruth]
  rel_bias_pct:
  valid_frames:
  too_close:
  too_far:
  low_confidence:
  invalid_pose:
  invalid_geometry:
  reported_uncertainty_mm (median):
```

## Acceptance Criteria (for prototype use)

> [!NOTE]
> These are engineering thresholds for prototype screening suitability. They are NOT clinical accuracy standards.

The estimator is considered **prototype-suitable** if, at 400mm with scale calibration:
- `|bias| ≤ 30mm` (≤7.5% relative error)
- `std ≤ 20mm`
- `valid_frame_rate ≥ 80%`
- `reported_uncertainty_mm ≥ |bias|` (uncertainty is not over-optimistic)

Without calibration, a `|bias| ≤ 60mm` at 400mm is acceptable (within the biological prior uncertainty).

## What Must NOT Be Claimed Without This Experiment
- That the estimator produces accurate distances
- That calibration fully eliminates systematic error
- That the uncertainty model is calibrated (it is an engineering budget)
- That the system is suitable for any clinical purpose
