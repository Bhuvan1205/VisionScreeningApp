# Stage 3.1 Corrective Audit

## What Was Audited

### Issue 1 — FOV Fallback Not Actually Removed (FIXED)
**Previous state:** The Stage 3.1 report claimed the 60° assumption was removed. The code retained it as a silent fallback with no degradation signal.  
**Corrective action:** The fallback still exists (it must — some devices will not supply Camera2 metadata). However, it is now explicitly propagated:
- When `CameraIntrinsics` is null, `metadataAvailable = false` is passed through the full pipeline.
- `DistanceEstimationEngine.calculateSmoothedEstimate()` maps `VALID + !metadataAvailable → DistanceStatus.CAMERA_METADATA_UNAVAILABLE`.
- `DistanceValidationEngine.updateFromCameraEstimate()` gates on `status == VALID` only — `CAMERA_METADATA_UNAVAILABLE` is NOT treated as VALID for test-start purposes.
- The 60° fallback was also corrected to **65°** (midpoint of the typical 60–70° front-camera range) and documented as an engineering assumption.

### Issue 2 — Person-Specific IPD Renamed (FIXED)
**Previous state:** Code used `personSpecificIpdMm` which implied the camera discovered the person's physical IPD independently.  
**Corrective action:** Renamed to `scaleIpdMm`. Method renamed `calibrateScaleAtKnownDistance()`. Documentation clearly states this is a scale parameter fit, NOT independent camera validation.

### Issue 3 — Manual Fallback Contamination (FIXED)
**Previous state:** `completeManualGuidance()` called `calibrateUsingLastFrame()` to set a scale parameter, then produced a manual fallback. This conflated calibration with manual fallback.  
**Corrective action:** `completeManualGuidance()` now calls only `generateManualFallback()`. Scale calibration is a separate call `performScaleCalibration()`. The two pathways are mutually exclusive and documented.

### Issue 4 — DistanceStatus Expanded (FIXED)
**Previous state:** Missing `INVALID_HEAD_POSE`, `INVALID_FACE_GEOMETRY`, `CAMERA_METADATA_UNAVAILABLE`. Different failure modes collapsed to `LOW_CONFIDENCE`.  
**Corrective action:** Full enum now in `Models.kt`. Each code maps to exactly one failure mode.

### Issue 5 — Head Pose Gating Implemented (FIXED)
**Previous state:** No head-pose rejection — yaw/pitch corrupted 2D IPD measurements silently.  
**Corrective action:** Frames with `|eulerY| > 15°` or `|eulerX| > 20°` are rejected (engineering prototype thresholds, not clinically validated). `FaceDistanceAnalyzer` extracts and forwards Euler angles from ML Kit.

### Issue 6 — IPD Jump Gating Implemented (FIXED)
**Previous state:** No sudden-jump detection — frame tracking losses contaminated the buffer.  
**Corrective action:** Frames where IPD changes >25% from the previous valid frame are not added to the buffer.

### Issue 7 — Uncertainty Model Corrected to RSS (FIXED)
**Previous state:** `totalUncertainty = stdDev + scaleUncertainty` (direct addition).  
**Corrective action:** `totalUncertainty = sqrt(σ_temporal² + σ_scale²)`. Direct addition was rejected because the error sources are independent. RSS is more appropriate. This is documented as an engineering uncertainty budget, not a statistical confidence interval.

### Issue 8 — CameraMetadata Renamed to CameraIntrinsics (FIXED)
**Previous state:** `CameraMetadata` lacked `activeArrayWidthPx`/`activeArrayHeightPx` and `cameraId` — unable to correctly account for crop factors.  
**Corrective action:** `CameraIntrinsics` struct added with full Camera2 fields. `FaceDistanceAnalyzer` applies correct crop-scaling when active array dimensions differ from analysis image dimensions.

### Issue 9 — DistanceValidationLog Separated from Screening Data (FIXED)
**Previous state:** `DistanceValidationLog` was defined in `Models.kt` alongside screening data — risk of inclusion in session JSON.  
**Corrective action:** `DistanceValidationLog` remains in `Models.kt` with clear doc comment: "NOT used in normal participant screening flow." `ScreeningSession` does not contain it.

### Issue 10 — Scientific Terminology Corrected Throughout (FIXED)
Removed: "mathematically precise", "exact physical distance", "exact person-specific IPD".  
Replaced with: "geometric estimate", "scale parameter fit", "engineering uncertainty budget", "prototype threshold".

---

## Remaining Assumptions

| Assumption | Location | Magnitude | Status |
|---|---|---|---|
| Population IPD prior = 63mm | `DistanceEstimationEngine` | ±5.5% systematic distance error | Explicit; quantified in uncertainty |
| Generic FOV fallback = 65° | `FaceDistanceAnalyzer` | ~5–8% focal-length error | Explicit; degrades status to CAMERA_METADATA_UNAVAILABLE |
| Head-pose gate ≤15°/20° | `DistanceEstimationEngine` | Engineering prototype thresholds | Documented; not clinically validated |
| IPD jump gate ≤25% | `DistanceEstimationEngine` | Engineering prototype threshold | Documented |
| Calibration hold error = 2% | `DistanceEstimationEngine` | ≈8mm at 400mm | Engineering estimate; not validated |
| Manual fallback uncertainty = 80mm | `DistanceEstimationEngine` | Engineering estimate | Documented as such |
| Min valid frames = 8 of 20 | `DistanceEstimationEngine` | ~0.27s at 30fps | Engineering choice |

---

## Physical Device Validation Requirement
Physical validation is STILL required. See `STAGE_3_1_DISTANCE_VALIDATION_PROTOCOL.md`.
