# Distance Monitoring System

## Architecture
The system employs an on-device front-camera pipeline to continuously validate viewing distance consistency without replacing the explicit screen measurement.
1. **CameraX** captures frames.
2. **ML Kit Face Detection** extracts bounding boxes (`FaceGeometry`).
3. **DistanceMonitor** establishes a user-specific baseline facial scale at the requested 40cm start position.
4. **TestScreen** pauses rendering / input if the scale violently diverges.

## Thresholds & Parameters
- `tooCloseThreshold`: 1.15x relative scale (face appears 15% larger).
- `tooFarThreshold`: 0.85x relative scale (face appears 15% smaller).
- `validFramesToResume`: 3 frames (reduces jitter/flickering).
- `invalidFramesToPause`: 3 frames (prevents accidental pauses during blinks or micro-movements).

## Fallback
If the user denies camera permission, or if the hardware fails to initialize CameraX, a manual fallback enables the user to proceed using the standard 40cm approximate physical measurement without automated enforcement.

## Privacy
- Fully offline and on-device processing via Google ML Kit.
- No images or frames are captured or persisted to storage.
- No biometric identity vectors are generated or saved.
- Camera access is detached upon exiting the testing phase.

## Limitations & Empirical Validation Needed
This prototype assumes linear bounding box scaling maps directly to Z-depth over small perturbations. Because varying ISPs and facial structures deform bounding boxes differently at the periphery, this mechanism should NOT be treated as clinically precise, but rather as a coarse prototype safeguard against gross participant movement. Further empirical validation with physical rulers across diverse hardware is required.
