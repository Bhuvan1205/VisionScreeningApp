# Stage 2 QA Report

## Summary
Stage 2 focused on hardening the MVP architecture by eliminating critical measurement flaws, standardizing rendering, and introducing a defensible threshold algorithm. All P0 and P1 objectives from the `STAGE_2_IMPLEMENTATION_PLAN.md` have been successfully implemented.

## Verification Checklist

### P0-A — Optotype Physical Rendering Integrity
- [x] Disabled anti-aliasing in `TumblingERenderer` via `drawIntoCanvas` with raw `Paint`.
- [x] Implemented integer pixel snapping in `MeasurementEngine` (stroke width rounded to nearest whole integer).
- [x] Restructured `OptotypeDimensions` into `Requested` and `Rendered` to track mathematical error.
- [x] **QA Note:** Tumbling E is now perfectly crisp on low-DPI devices, eliminating contrast sensitivity bleed.

### P0-B — Device-Specific Calibration Integrity
- [x] Updated `CalibrationRepository` to cross-check `Build.MANUFACTURER` and `Build.MODEL` on JSON load.
- [x] **QA Note:** If a Google Cloud backup restores a calibration JSON onto a physically different phone, it is now successfully intercepted and invalidated.

### P0-C — Viewing Distance / Hardware Feasibility
- [x] Added `MeasurementEngine.calculateMinimumViewingDistance`.
- [x] Updated `SessionViewModel.startNewSession` to dynamically negotiate distance instead of hardcoding 400mm.
- [x] **QA Note:** Device hardware limits are properly respected, guaranteeing no sub-pixel targets are presented.

### P0-D & P0-E — Clinical Algorithm & NOT_VISIBLE Semantics
- [x] Deprecated `MvpStepDownAlgorithm`.
- [x] Implemented `Staircase3Down1UpAlgorithm` (3-down, 1-up, targeting ~79% threshold).
- [x] Migrated `VisualAcuityTestEngine` to delegate state management to the new `TestAlgorithm` interface.
- [x] Differentiated `NOT_VISIBLE` responses to fast-fail limits and instantly trigger staircase reversals.

### P1 — Persistence & Session Hardening
- [x] Migrated `SessionRepository` and `CalibrationRepository` to `Dispatchers.IO` and `androidx.core.util.AtomicFile` for corruption-safe disk I/O.
- [x] Bound `saveSession` calls in `SessionViewModel` to `viewModelScope.launch` to prevent Main thread blocking.
- [x] Added `recoverOrphanedSessions` sweep to map abandoned process-death `IN_PROGRESS` sessions to `INTERRUPTED`.

### P1 — Research Data Versioning
- [x] Injected `appVersionCode` and `appVersionName` (via `BuildConfig`) into `ScreeningSession` schema for data provenance.

### P2 — Portrait Lock
- [x] Added `android:screenOrientation="portrait"` to `MainActivity` in `AndroidManifest.xml`.

## Next Steps
Proceeding to Phase 7: Automated camera-based distance estimation.
