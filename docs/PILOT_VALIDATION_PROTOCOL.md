# Pilot Validation Protocol

## 1. Objective
To physically validate the usability, ergonomics, and instructional clarity of the offline visual acuity screening prototype on non-technical users and field operators, without clinical validation goals.

## 2. Equipment Required
- Android smartphone (pre-installed with SwasthyaTech Prototype Phase 6+ APK).
- Standard physical ID or bank card (exactly 85.6 mm width).
- An opaque eye cover (or operator hand).
- Measuring tape (to set initial 40 cm baseline).
- Printout of `PILOT_OBSERVATION_CHECKLIST.md`.

## 3. Procedure

### A. Calibration Procedure
1. Operator hands the device to the participant.
2. Operator asks participant to calibrate the screen using a standard ID card based *only* on the on-screen instructions.
3. Operator observes whether the participant understands parallax errors and adjusts the slider comfortably.

### B. Viewing-Distance Procedure
1. Operator asks participant to hold the phone at the requested distance based *only* on the "forearm" description.
2. Operator measures the actual distance chosen by the participant using the measuring tape to verify if the "forearm" analogy successfully approximates 40 cm.

### C. Right-Eye / Left-Eye Testing Procedure
1. Participant initiates the test.
2. Operator observes whether the participant successfully covers the correct eye without prompting.
3. Operator observes interaction with the Practice screen.
4. Participant completes the Right Eye test. Operator notes any accidental touches or hesitations.
5. Participant transitions to Left Eye test. Operator observes if the transition screen is understood.

### D. What the Operator Should Observe
- **Usability Problems:** Hesitation over which arrow to press, ignoring the "Can't see" button when struggling, holding the phone too close, swiping to exit accidentally.
- **Measurement Problems:** Noticeable screen glare, inability to see the 1.0 LogMAR 'E' clearly, system crashes.

## 4. What Data Should Be Recorded
- Observations from the `PILOT_OBSERVATION_CHECKLIST.md`.
- No personally identifiable information (PII) or clinical metadata is collected. The device UUID securely isolates local data.

## 5. Limitations & Safety Disclaimer
* **NOT A CLINICAL DEVICE:** This session is strictly a UI/UX usability pilot.
* **NO MEDICAL DIAGNOSIS:** Under no circumstances should the LogMAR score produced during this pilot be presented to the participant as a medical diagnosis or prescription. 
* **DATA ISOLATION:** All data remains completely offline in the local application sandbox.
