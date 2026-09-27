# SwasthyaTech Vision Screening Prototype - Architecture

## Project Purpose
The SwasthyaTech Vision Screening Prototype is an Android-based, low-cost visual-acuity screening tool intended primarily for students and underserved/resource-constrained populations. It operates on standard smartphones and does not rely on high-end hardware, continuous internet connectivity, or expensive ophthalmic equipment.

## MVP Scope & Boundaries
The two-day prototype aims to build a functional, demonstrable smartphone-based visual-acuity screening prototype. This prototype establishes the measurement/calibration foundation for a larger AI-enabled vision-screening platform.

**In-Scope (MVP):**
* Android application.
* Controlled viewing distance of 40 cm.
* Optotype rendering scaled physically according to visual-angle principles.
* Screen calibration to ensure consistent physical dimensions across different smartphones.
* Tumbling E optotype.
* Touch-based directional input.
* Trial-level screening data capture (local/offline data storage).
* Modular architecture to allow for future additions without rewriting the core measurement engine.
* Screening tool (not a diagnostic tool).

**Out-of-Scope (Explicit MVP Exclusions):**
* AI models.
* Transformer models.
* Cloud backend / remote database.
* Camera-based distance estimation.
* Sophisticated image analysis / Computer Vision.
* Speech recognition / Voice input.
* Advanced clinical diagnosis.
* Authentication and unnecessary networking.

## Architecture and Modules
The application is structured into clearly separated modular packages to ensure the UI, measurement logic, input, and data layers do not tightly couple with one another.

### 1. CalibrationModule (`com.example.swasthyatech.calibration`)
**Responsibility:** Handles manual calibration of the screen to establish the relationship between physical screen dimensions (mm) and pixels.
**Future Extensions:** Camera-based distance estimator.

### 2. OptotypeModule (`com.example.swasthyatech.optotype`)
**Responsibility:** Contains the logic to render and scale the Tumbling E optotype based on the target visual acuity, viewing distance, and calibration data.

### 3. TestEngine (`com.example.swasthyatech.engine`)
**Responsibility:** The scientific/measurement core of the prototype. Contains the adaptive test logic, calculating difficulty, preserving test metadata, and orchestrating the trial.
**Constraints:** Must not depend directly on Android UI screens, touch-specific implementations, voice recognition, or camera processing.

### 4. InputModule (`com.example.swasthyatech.input`)
**Responsibility:** Handles user interaction. For MVP, it interprets touch-based directional inputs (UP, DOWN, LEFT, RIGHT).
**Future Extensions:** Voice input, Vision/Camera-based input.

### 5. DataModule (`com.example.swasthyatech.data`)
**Responsibility:** Local persistence for screening session metadata, trial data, participant info, calibration, and calculated results.
**Future Extensions:** Remote synchronization, secure backend storage, anonymized analytics export.

### 6. ResultModule (`com.example.swasthyatech.result`)
**Responsibility:** Converts test performances into a visual-acuity representation (and logMAR representation), and determines final outcomes for both eyes.

### 7. UI/Application Layer (`com.example.swasthyatech.ui`)
**Responsibility:** Binds the abstract logic, test engine, and inputs into the Android Compose UI. Orchestrates the screen flow from Launch -> Calibration -> Instructions -> Test (Right/Left) -> Results.

## Major Assumptions and Known Technical Risks
1. **Clinical Precision:** Physical screen calibration by ordinary users may contain slight inaccuracies, affecting the absolute clinical validity of the exact visual angle. 
2. **Device Scaling Limits:** Very small target acuities might fall below the pixel-density threshold of cheaper, low-DPI Android devices, causing rendering artifacts for the Tumbling E.
3. **Viewing Distance Adherence:** Since the MVP relies on a fixed 40 cm distance without camera tracking, it assumes the user/participant strictly maintains this distance. Any deviation directly compromises the measurement correctness.
4. **Monocular Occlusion:** Physical occlusion (covering the non-tested eye) is assumed and must be correctly performed by the user/supervisor, as software cannot enforce it.
