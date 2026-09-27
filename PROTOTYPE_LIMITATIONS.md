# SwasthyaTech Vision Screening Prototype
## Known Limitations

The current MVP codebase successfully implements a mathematically calibrated, offline-first visual acuity screening protocol. However, as a prototype, several functional and clinical limitations remain. These must be acknowledged before any field deployment or scientific study.

### 1. Clinical and Diagnostic Limitations
* **No Clinical Validation:** The current implementation algorithm (MVP Step-Down) and math formulas are engineered to standard geometric approximations, but the application has NOT yet undergone formal clinical validation against standard ETDRS or Snellen charts with human patients.
* **Not a Diagnostic Tool:** The application outputs a raw LogMAR score. It cannot diagnose myopia, hyperopia, astigmatism, or prescribe lenses. It is strictly a screening tool to flag potential vision issues.
* **Prototype Protocol:** The adaptive test logic uses a simplified 3-trial step-down methodology. Formal adaptive protocols (e.g., psychometric staircases or Bayesian estimation) may be required for research-grade accuracy in future iterations.

### 2. Environmental and Physical Limitations
* **Manual Viewing Distance Control:** The MVP requires the participant to maintain exactly 40 cm from the screen. There is no automated camera-based or LIDAR-based distance estimation to pause the test if the user leans forward. Operator enforcement is required.
* **Manual Physical Calibration:** The calibration relies on the user visually matching an on-screen box to a physical reference (e.g., an 85.6 mm credit card). Human error during this matching process will linearly skew all optotype sizing.
* **Manual Eye Occlusion:** The test assumes the user has physically covered the correct non-tested eye. The application cannot verify compliance automatically.
* **Screen Protector / Display Variability:** Thick screen protectors, varied screen brightness, contrast ratios, and physical pixel arrangements (e.g., Pentile OLED vs RGB LCD) may affect the optical clarity of the smallest optotypes. 

### 3. Application and Technical Limitations
* **Touch-Only Input:** The test currently relies exclusively on touch inputs. Voice recognition and gesture/gaze tracking are not implemented in this phase.
* **Local Persistence Only:** All screening data is saved exclusively to the device's internal application sandbox (offline `filesDir`). There is no automated cloud synchronization, backup backend, or cross-device patient profile syncing. 
* **Interrupted Session Resumption:** If a test is interrupted (e.g., app killed midway), the session is marked as `INTERRUPTED` and safely retained, but there is currently no UI mechanism to resume a test exactly from the trial it left off at. A new session must be started.
* **Participant Anonymity:** Participant identifiers are currently auto-generated UUIDs. There is no complex onboarding flow to capture distinct demographic profiles.
* **Interactive Device Validation:** While the build architecture and mathematical models have been validated via compilation constraints and static analysis, continuous automated execution on physical Android targets has been limited in this agentic development environment. 
