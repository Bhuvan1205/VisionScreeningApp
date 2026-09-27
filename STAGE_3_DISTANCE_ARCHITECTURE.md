# Stage 3 Distance Architecture

## Objective
Implement the most reliable offline, smartphone-only, zero-external-hardware distance estimation protocol for visual acuity testing. This method must provide continuous estimates and fail safely, degrading to a documented manual fallback if strict requirements are not met.

## Supported Hardware Constraints
* **Platform:** Android Smartphone (API 24+)
* **Sensors:** Front-facing camera
* **Processing:** Fully offline, no cloud inference
* **Dependencies:** ML Kit Face Detection (on-device only), CameraX

## Recommended Primary Method: Monocular IPD (Interpupillary Distance) Geometry
The standard human interpupillary distance (IPD) across adult populations has a tightly bounded median (roughly 63mm). By detecting facial landmarks (left and right eyes) using ML Kit and measuring their pixel distance in the camera frame, we can use the camera's horizontal Field of View (FOV) to project a highly stable geometric distance.

### Mathematical Model (Pinhole Camera)
1. **Camera Intrinsics ($f_{px}$):** 
   $f_{px} = \frac{W_{image}}{2 \cdot \tan(\frac{FOV_{horizontal}}{2})}$
2. **Pixel IPD ($IPD_{px}$):** 
   Distance between Left Eye and Right Eye landmarks in the image.
3. **Estimated Distance ($D_{mm}$):**
   $D_{mm} = \frac{IPD_{mm} \cdot f_{px}}{IPD_{px}}$

### Expected Error & Assumptions
* **Adult IPD Assumption:** $63.0\text{mm}$.
* **Standard Deviation (Population):** $\approx 3.5\text{mm}$ (range 55–70mm).
* **Distance Error Propagation:** The percentage error in distance exactly matches the percentage variance of the user's IPD from the assumed $63\text{mm}$. A user with a $68\text{mm}$ IPD will experience a $+7.9\%$ distance miscalculation. 
* At a $400\text{mm}$ target, this yields roughly $\pm30\text{mm}$ physical error. This is significantly better than unstructured manual string-guesses and entirely acceptable for MVP screening limits.

### Uncertainty Model (Temporal Smoothing)
Single-frame facial landmark estimation is noisy. The estimator will maintain a **Temporal Circular Buffer (Size = 30 frames)**:
* **Distance:** Median of the buffer.
* **Uncertainty ($\Delta D$):** Standard deviation of the buffer + a fixed population baseline ($\pm 5\%$).
* **Confidence:** An inverse function of $\Delta D$. High variance = Low Confidence.

## Fallback Method: Explicit Manual String/Arm length
If the camera is unsupported, permissions are denied, lighting is poor, or multiple faces are detected, the system degrades to `DistanceSource.MANUAL_FALLBACK`.
The user is instructed to measure exactly the `negotiatedDistanceMm` (e.g., $400\text{mm}$) physically. 
The system explicitly records this as `MANUAL_FALLBACK` with high uncertainty.

## Pipeline Integration (Stage 2 Renderability)
The distance estimator will NOT manipulate the staircase. Instead:
1. `DistanceEstimationEngine` dictates the verified physical distance ($D_{est}$).
2. To ensure worst-case legibility, the `MeasurementEngine` uses $D_{effective} = D_{est} - \Delta D$ (the closest possible bound of the uncertainty interval) to verify if the minimum optotype stroke exceeds 1 raw pixel.
3. If $D_{effective}$ is too close, testing is blocked until the user moves back.

## Failure Modes & Blocking
Testing is explicitly blocked (or paused) if:
1. `STATUS == TOO_CLOSE` or `TOO_FAR`.
2. `STATUS == MULTIPLE_FACES` (prevents tracking the wrong person).
3. `STATUS == FACE_NOT_DETECTED`.
4. `STATUS == LOW_CONFIDENCE` (high variance).

Data will explicitly be recorded in the `ScreeningSession` payload under `DistanceEstimate`, fully preserving scientific integrity.
