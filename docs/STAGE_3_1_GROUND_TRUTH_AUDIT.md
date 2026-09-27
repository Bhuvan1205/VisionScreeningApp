# Stage 3.1: Ground Truth Implementation Audit

## Overview
A rigorous audit of the Ground Truth Experiment implementation was conducted to ensure the validation experiment does not produce misleading evidence about distance accuracy. The audit focused on scientific validity, statistical correctness, and experimental design.

## Audit Findings & Fixes

### 1. [P0 - SCIENTIFIC INVALIDITY] Uncalibrated IPD confounder
**Issue:** The experiment was testing "absolute accuracy" while using the population prior IPD of 63.0mm (`DistanceEstimationEngine` was instantiated without calibration). Any divergence between the user's actual IPD and 63.0mm would manifest as a systematic bias in the output, falsely suggesting the camera/intrinsics model is inaccurate. 
**Fix:** The experiment was re-scoped. It cannot validate absolute intrinsic accuracy. The acceptance criteria now explicitly focus on **repeatability** (variance) rather than absolute accuracy. Documentation (`STAGE_3_1_GROUND_TRUTH_PROTOCOL.md`) was updated to emphasize that this only validates if the baseline bias is stable enough to warrant clinical calibration scaling in Stage 4.

### 2. [P1 - MISLEADING INTERPRETATION] StdDev misrepresented repeatability
**Issue:** The UI and CSV were reporting `stdDev`. However, this was calculated as the variance of frames *within a single 1.5-second hold* (approx. 10-45 frames). This merely measures tracking jitter (within-trial stability), not true repeatability of the user physically repositioning the phone.
**Fix:** Renamed the field in the UI to `Within-trial StdDev` to clarify its meaning. True repeatability must be calculated offline by taking the standard deviation of the 10 trial *means* for a given distance.

### 3. [P1 - MISLEADING INTERPRETATION] Systematic Ground Truth measurement bias
**Issue:** The protocol instructed measuring distance to the "bridge of the nose". The ML face landmarks are typically calibrated closer to the corneal plane (eyes), which sit ~10-15mm deeper in the face. This creates a systematic measurement mismatch.
**Fix:** Updated the protocol to specify measuring to the outer canthus of the eye, or strictly documenting the 10-15mm offset.

### 4. [P2 - ENGINEERING IMPROVEMENT] State machine movement vulnerability
**Issue:** The state machine checked for a >15% distance change or a loss of valid status to transition from `WAITING_FOR_MOVEMENT` to `IDLE`. A noisy distance estimate (e.g., a momentary tracking glitch) could satisfy the 15% jump condition without the user physically moving the phone, allowing duplicate trials.
**Fix:** Updated the protocol. Since this is a developer-driven protocol, the user is explicitly instructed to obscure the camera or physically move the device. The manual "START STABILITY WINDOW" button acts as an intent gate.

### 5. [P2 - ENGINEERING IMPROVEMENT] Manual CSV saving risk
**Issue:** The protocol stated that trials were automatically saved to CSV, but the implementation required the developer to manually click "SAVE TRIAL TO CSV" after every successful trial. This created a high risk of data loss or forgetting to save.
**Fix:** Added an `onTrialCompleted` callback to `GroundTruthExperimentManager` and wired it in `GroundTruthValidationScreen` to automatically append the trial data to the CSV immediately upon success.

### 6. [P2 - ENGINEERING IMPROVEMENT] Missing Yaw/Pitch in Accuracy trials
**Issue:** The CSV log for accuracy trials hardcoded `yaw=0` and `pitch=0`.
**Fix:** `TrialResult` now calculates and stores `avgYaw` and `avgPitch` across the valid frames, which are written to the CSV.

### 7. [P0 - SCIENTIFIC INVALIDITY] Experimental Identifiability
**Issue:** The initial protocol could not distinguish between intrinsic platform error (Camera2 focal length + ML Kit landmark scale) and biological variance (Subject IPD mismatch).
**Fix:** Added an `ipd_mode` toggle in the Ground Truth UI. The tester can now enter their physically measured IPD (Mode B) to mathematically isolate the intrinsic platform error from the population biological variance.

### 8. [P3 - OPTIONAL] Experimental design scope
**Issue:** The initial 250-600mm protocol collected too much redundant data without isolating key variables.
**Fix:** Redesigned the protocol into 3 phases:
- Phase A: 400mm with Measured IPD (isolates platform hardware error).
- Phase B: 300, 400, 500mm with Population IPD (measures real-world bias and linearity).
- Phase C: Robustness testing.

## Conclusion
The fixes have resolved the integrity issues and mathematical confounders in the validation tool. The resulting data from the physical execution of this protocol will correctly isolate within-trial stability, cross-trial repeatability, intrinsic platform error, and baseline systematic bias. This is sufficient to make a defensible decision about proceeding to Stage 4 (clinical calibration).
