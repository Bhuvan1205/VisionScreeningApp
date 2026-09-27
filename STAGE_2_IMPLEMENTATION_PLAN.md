# Stage 2 Implementation Plan & Gap Reconciliation

## Current-State Gap Reconciliation

Based on the `STAGE_2_BACKEND_AUDIT.md` and the existing codebase:

| Gap ID | Description | Current State | Reconciliation Notes |
| :--- | :--- | :--- | :--- |
| **GAP-01** | Sub-Pixel Aliasing & Optotype Degradation | **NOT IMPLEMENTED** | `MeasurementEngine` uses direct float calculations and `TumblingERenderer` draws with anti-aliasing. P0-A will address this by forcing integer-snapping and disabling anti-alias. |
| **GAP-02** | Device Backup Calibration Invalidation | **NOT IMPLEMENTED** | `CalibrationRepository` blindly loads JSON without validating `Build.MODEL` or `Build.MANUFACTURER`. P0-B will implement device fingerprinting. |
| **GAP-03** | Hardcoded Viewing Distance | **PARTIALLY IMPLEMENTED** | `DistanceValidationEngine` exists and tracks state, but `SessionViewModel` hardcodes `400f` instead of validating physical feasibility. P0-C will dynamically calculate and negotiate this distance. |
| **GAP-04** | Non-Standard Threshold Algorithm | **NOT IMPLEMENTED** | `MvpStepDownAlgorithm` is a crude 2-out-of-3 level progression. P0-D will replace this with a proper adaptive threshold algorithm (e.g., psychometric staircase). |
| **GAP-05** | Process Death Orphaned Sessions | **NOT IMPLEMENTED** | Sessions stay `IN_PROGRESS` if the OS kills the app. P1 will implement a recovery sweep to mark these as `INTERRUPTED` safely. |
| **GAP-06** | Destructive Tallying of NOT_VISIBLE | **NOT IMPLEMENTED** | `NOT_VISIBLE` acts just like an incorrect guess. P0-E will separate this semantically and use it to instantly trigger thresholds. |
| **GAP-07** | Insufficient Data Versioning | **NOT IMPLEMENTED** | Missing `VERSION_CODE` and `VERSION_NAME` in `ScreeningSession`. P1 will inject `BuildConfig` data. |
| **GAP-08** | Unlocked Activity Orientation | **NOT IMPLEMENTED** | Manifest lacks orientation locks. P2 will enforce portrait. |

## Implementation Order
1. **P0-A**: Optotype Physical Rendering Integrity (Math/Rendering)
2. **P0-B**: Device-Specific Calibration Integrity (Device matching)
3. **P0-C**: Viewing Distance / Hardware Feasibility (Distance negotiation)
4. **P0-E & P0-D**: `NOT_VISIBLE` Semantics & Adaptive Test Algorithm
5. **P1**: Persistence Hardening & Research Versioning
6. **P2**: Portrait Lock
7. **P3**: AI Layer (Explicitly deferred)
8. **Final**: Documentation updates, testing, and QA Report generation.
