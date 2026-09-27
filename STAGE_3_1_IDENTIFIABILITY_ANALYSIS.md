# Stage 3.1: Identifiability Analysis

## The Distance Equation
The exact distance equation used in `DistanceEstimationEngine` is:
`estimated_distance_mm = (ipdMmScale * focalLengthPx) / ipdPx`

Where:
* `ipdPx`: **Measured** via ML Kit face landmarks (specifically the Euclidean distance between `FaceLandmark.LEFT_EYE` and `RIGHT_EYE` centers).
* `focalLengthPx`: **Obtained** from Camera2 metadata (or assumed from a 60-degree generic FOV if hardware metadata is denied).
* `ipdMmScale`: **Assumed** from a population prior (63.0 mm) OR **Calibrated/Measured** if explicitly set.

## Error Sources & Identifiability

If we test only with the uncalibrated population prior (Mode A), we cannot isolate camera/hardware accuracy from biological variance. Introducing a Measured IPD mode (Mode B) allows us to break this confounder.

### A. Camera/intrinsic geometry error
* **Source:** Manufacturer misreporting of focal length or active array dimensions in Camera2 API.
* **Identifiability:** **Distinguishable in Mode B.** If we provide the exact subject IPD and hold at a known ground-truth distance, any systematic error must originate from the camera intrinsics (or a constant scale error in ML Kit landmarks).

### B. Face-landmark/IPD measurement error
* **Source:** ML Kit consistently placing eye landmarks slightly narrower/wider than the true physical pupils, or the scale of pixels being distorted.
* **Identifiability:** **Confounded with A.** The experiment cannot mathematically distinguish whether `focalLengthPx` is misreported by 2% or whether `ipdPx` is consistently localized 2% too small. They multiply together. However, combining A and B represents the "true" intrinsic platform error we care about for the application.

### C. Subject-specific IPD error
* **Source:** The participant's physical IPD differs from the 63.0 mm population prior.
* **Identifiability:** **Distinguishable.** By running trials in Mode A (Prior) and Mode B (Measured), the difference in mean bias between the modes exactly isolates the effect of the population assumption.

### D. Camera-to-face reference-point offset
* **Source:** Ground truth is measured to the outer canthus, but ML Kit eye centers sit deeper/shallower in the 3D head geometry.
* **Identifiability:** **Confounded with C.** A constant mm offset in physical measurement acts similarly to a bias. To minimize this, we measure to the outer canthus (coplanar to the eyes) rather than the bridge of the nose.

### E. Temporal tracking noise
* **Source:** Frame-to-frame jitter in ML Kit's 2D localization.
* **Identifiability:** **Independently Measurable.** This is perfectly isolated by the `within_trial_stddev` metric across the 1.5-second stability window.

### F. Head-pose effects
* **Source:** Non-frontal yaw or pitch causes 2D foreshortening of `ipdPx`.
* **Identifiability:** **Independently Measurable.** The Phase C (Robustness) protocol explicitly tests this by holding physical distance constant and varying pose, confirming that gating prevents severe foreshortening.

## Most Important Conclusion
The experiment **CANNOT** validate absolute clinical accuracy of the uncalibrated prototype, because it cannot know the end-user's IPD in production. 

The experiment **CAN** prove:
1. Whether the platform (Camera + ML Kit) is **consistent and repeatable** for a single user (variance across 10 trials).
2. The exact magnitude of the population-prior confounder (by comparing Mode A and Mode B).
3. Whether distance-dependent scaling errors exist (e.g., if the bias changes drastically between 300mm and 500mm, the pinhole camera model is failing).

If the repeatability is high (low variance) but Mode A shows a 10% bias, the system works but *requires* clinical scaling/calibration (Stage 4).
