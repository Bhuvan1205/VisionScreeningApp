# SwasthyaTech Vision Screening Prototype
## Demonstration Checklist

Follow this procedure to operate and demonstrate the vertical slice of the offline vision screening MVP.

### Prerequisites
* Android device (running Android 7.0 / API 24 or higher).
* Application installed (SwasthyaTech).
* A standard physical ID card or Credit Card (exactly 85.6 mm wide) for calibration.
* An eye cover/occluder (or participant's hand) for testing individual eyes.
* Measuring tape (to position the participant precisely 40 cm from the screen).

### 1. Calibration (One-time or per-device setup)
1. Launch the SwasthyaTech application.
2. Observe the Home screen. If calibration is missing, the screen will warn "Device is not calibrated."
3. Tap **"Calibrate Screen"**.
4. Place the standard physical ID card gently against the phone screen.
5. Adjust the slider until the blue calibration box on the screen perfectly matches the physical width of the card.
6. Tap **"Save Calibration"**.
7. Observe that you are returned to the Home screen and the "Start Screening" button is now unlocked.

### 2. Screening Preparation
1. Ensure the participant is seated comfortably.
2. Measure a viewing distance of exactly **40 cm** from the participant's eyes to the screen.
3. Tap **"Start Screening"**.

### 3. Right Eye Test
1. The screen will display "Testing: RIGHT EYE".
2. Instruct the participant to **cover their left eye** (using an occluder or their hand).
3. The screen will display the first Tumbling-E optotype.
4. Ask the participant which way the "legs" of the E are pointing (Up, Down, Left, or Right).
5. The operator (or the user) taps the corresponding directional button on the screen.
6. The screen briefly pauses to log the response, then presents the next trial.
7. Continue until the Right Eye test concludes and saves.

### 4. Left Eye Test
1. The screen will transition to "Testing: LEFT EYE".
2. Instruct the participant to **cover their right eye**.
3. Repeat the Tumbling-E testing sequence.
4. Continue until the Left Eye test concludes.

### 5. Review Results & Persistence
1. Observe the **Screening Complete** result screen.
2. Note the LogMAR scores and equivalent Snellen fractions for both the Right and Left eyes.
3. Tap **"Return Home"**.
4. *(Optional/Developer)*: Connect the device via USB and use Android Studio Device Explorer to navigate to `/data/data/com.example.swasthyatech/files/screening_sessions/`.
5. Open the latest `.json` file to verify that all configurations, calibration metadata, exact trial-level responses, and final results were successfully persisted entirely offline.

### Known Operator Limitations
* **Distance:** The application cannot detect if the user leans in. The operator MUST maintain the 40 cm distance physically.
* **Occlusion:** The app cannot verify if the non-tested eye is actually covered. The operator must ensure compliance.
* **Environment:** Ensure testing occurs in a reasonably lit room to maintain standard pupil dilation and screen contrast visibility.
