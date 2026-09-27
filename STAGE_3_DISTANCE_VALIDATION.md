# Stage 3 Distance Validation Report

## Overview
This document outlines the testing, mathematical verification, and architectural validation for the Camera-based Interpupillary Distance (IPD) estimation algorithm integrated in Stage 3.

## 1. Mathematical Verification
The `DistanceEstimationEngine` and `FaceDistanceAnalyzer` compute physical distance using geometric similar triangles based on standard human IPD.

**Equations used:**
- $f_{px} = \frac{W_{image}}{2 \cdot \tan(\frac{FOV_{horizontal}}{2})}$
- $D_{mm} = \frac{IPD_{mm} \cdot f_{px}}{IPD_{px}}$

**Verification Analysis:**
- Assuming a 60-degree horizontal FOV and 1080p width ($W_{image} = 1080$), $f_{px} = 540 / \tan(30^\circ) = 935.3 \text{px}$.
- If a user with $63\text{mm}$ IPD is held at $400\text{mm}$, the detected $IPD_{px}$ will be $(63 \cdot 935.3) / 400 = 147.3 \text{px}$.
- If the analyzer sees $147.3\text{px}$, it outputs $400\text{mm}$.
- The equation is geometrically sound assuming a rectilinear lens (standard smartphone front camera). Wide-angle distortion at the edges of the frame may introduce slight non-linearities, but the user's face is centered during validation.

## 2. Temporal Smoothing & Uncertainty Convergence
- **Buffer Size:** 15 frames. At 30 FPS, this is 500ms of stable data, ensuring fast response without excessive jitter.
- **Uncertainty Model:** The uncertainty ($\Delta D$) is calculated as the standard deviation of the buffer plus a fixed 5% population variance scalar. This mathematically ensures that $\Delta D$ encompasses both sensor noise (movement/jitter) and biological variance (if the user's IPD isn't exactly 63mm).

## 3. Test Coverage (`DistanceEstimationEngineTest.kt`)
The following boundary conditions are successfully covered by unit tests:
1. **Low Confidence:** Engine correctly reports `LOW_CONFIDENCE` if fewer than 5 frames are available.
2. **Valid Smoothing:** Successfully computes the median of 15 stable frames and outputs `VALID` with high confidence.
3. **Too Close Rejection:** Correctly categorizes and rejects distances falling below the safety boundary (`TOO_CLOSE`).
4. **Error Recovery:** A `FACE_NOT_DETECTED` instantly clears the temporal buffer, preventing a spurious old distance from validating a new user.
5. **Manual Fallback:** Triggers a 100mm high-uncertainty validation via `MANUAL_FALLBACK` if the user explicitly skips camera guidance.

## 4. Optotype Integration
By integrating with the `MeasurementEngine`, if $\Delta D$ is high, the effective render distance decreases ($D_{effective} = D - \Delta D$). This guarantees the smallest optotype stroke remains $\ge 1\text{px}$. If it drops below 1px due to high uncertainty, testing is explicitly blocked until the user moves further back.

## 5. Physical Device Requirements
- Android Device (API 24+)
- Google Play Services (For ML Kit, or bundled ML Kit models).
- Face must be visible (not occluded by hands/hair).
- If requirements are unmet, system defaults safely to `DistanceSource.MANUAL_FALLBACK`.

## Conclusion
The IPD-based camera distance estimation satisfies the constraint of zero-external-hardware offline validation. It degrades gracefully into documented uncertainty intervals, ensuring scientific integrity is preserved.
