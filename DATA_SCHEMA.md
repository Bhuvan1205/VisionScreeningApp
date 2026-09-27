# Phase 1: Data Model & Screening Protocol

## Overview
This document describes the offline-first domain model designed for the SwasthyaTech Vision Screening Prototype. 

The models are serialized to JSON and persisted locally on the device's internal storage via `SessionRepository`, which allows for easy extraction for eventual clinical validation and research.

## Core Models

### ScreeningSession
The root entity representing an entire interaction with a participant.
* **`sessionId`**: Unique identifier for the screening session.
* **`participantId`**: Anonymized reference to the user.
* **`timestamp`**: Time of test initiation.
* **schemaVersion**: Currently 1, ensures backwards compatibility for future research schemas.
* **ppVersionName** & **ppVersionCode**: Captures application build metadata (via BuildConfig) for exact reproducibility of the test environment.
* **`testConfiguration`**: Contains exactly what algorithm, distance, and calibration were active.
* **`rightEyeTest` & `leftEyeTest`**: Holds `EyeTestResult` specific to each eye.
* **`finalResult`**: The overall test interpretation (not for clinical diagnosis, but for screening flags).
* **`sessionStatus`**: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, or `INTERRUPTED`.

### TestConfiguration
Provides reproducibility for future researchers to know exactly what parameters were active during the session.
* **`optotypeType`**: e.g., `TUMBLING_E`.
* **`supportedOrientations`**: e.g., `[UP, DOWN, LEFT, RIGHT]`.
* **`viewingDistance`**: Uses `ViewingDistance` model (nominal 40cm for MVP).
* **`deviceCalibration`**: Contains the device's measured pixels-per-mm for accurate scale recreation.

### Trial
Represents a single optotype presentation. Preserving this is crucial for machine learning and statistical validation.
* **`trialId`**: Identifier/Index.
* **`eye`**: Which eye is currently tested.
* **`targetAcuity`**: The target visual acuity presented (e.g., LogMAR).
* **`presentedOrientation`**: The direction the Tumbling E faced.
* **`userResponse`**: The normalized input (independent of touch/voice).
* **`isCorrect`**: Boolean evaluation of the response.
* **`responseTimeMs`**: Captured for cognitive/performance validation.

### EyeTestResult
Groups trials and their evaluated status by eye. By keeping Right and Left completely separate, the application gracefully handles monocular workflows, interrupted tests, or one-eye screenings.

## Persistence 
Local persistence is achieved using `kotlinx.serialization.json`. The `SessionRepository` writes directly to internal app storage (`context.filesDir / screening_sessions / <sessionId>.json`). This satisfies the requirement for the simplest reliable local persistence mechanism without requiring cloud networking or heavy Room database abstractions that slow down prototyping.

## Phase 7: Distance Validation
The ScreeningSession payload now tracks distance enforcement mechanics directly inside the distanceValidation: DistanceValidation node, noting its explicit string representation mapping to the ValidationMethod schema, recording MANUAL_GUIDANCE with associated confidence ratings.

