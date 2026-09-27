# SwasthyaTech Vision Screening Prototype — Phase-Wise Execution Plan

**Purpose:** Execution blueprint for the coding agent
**Platform:** Android
**Prototype horizon:** 2 days
**Current objective:** Build a functional, demonstrable smartphone-based visual-acuity screening prototype that establishes the measurement/calibration foundation for the larger AI-enabled vision-screening platform.

---

## 0. Project Context and Prototype Objective

### Project background

The broader project is a **low-cost, AI-based vision screening platform for school children and underserved populations**, intended to operate on standard smartphones and eventually support image, video and speech-based inputs, multilingual interaction, non-expert users, clinical validation and anonymized health-data reporting. 

The target environment is resource-constrained. Therefore, the product should not assume:

* high-end smartphones;
* sophisticated cameras;
* continuous internet connectivity;
* technically trained operators;
* English literacy;
* expensive ophthalmic equipment.

The SwasthyaTech program specifically expects a functional prototype/PoC and evaluates technical feasibility, prototype maturity, clinical relevance, scalability and commercialization potential. 

### Immediate prototype objective

The two-day prototype is **not intended to implement the complete AI vision-screening platform**.

The immediate objective is:

> **Build a calibrated Android-based visual-acuity screening system that can present appropriately scaled optotypes at a controlled short viewing distance, capture user responses, estimate visual-acuity performance, and persist structured screening data.**

The prototype should establish the technical foundation upon which future capabilities can be added.

---

# 1. Core Design Decisions Already Made

These decisions should be treated as the baseline unless a later phase identifies a technical reason to change them.

### 1.1 Platform

**Android application**

The application should be designed for ordinary Android smartphones rather than assuming flagship hardware.

---

### 1.2 Testing methodology

The prototype will use a **controlled short viewing distance**, provisionally:

> **40 cm from the eye to the screen**

The application should not attempt to reproduce a physical multi-metre eye chart.

Instead, the application will reproduce the appropriate **visual/angular size of the optotype** on the smartphone screen.

The core principle is:

```text
Required visual angle
        +
Known viewing distance
        +
Known physical screen dimensions
        ↓
Required physical optotype size
        ↓
Required screen pixels
```

---

### 1.3 Screen calibration

The application should not blindly rely on Android's reported DPI.

A manual calibration mechanism should establish the relationship between physical screen dimensions and pixels.

The prototype should therefore contain:

> **Screen Calibration → pixels/mm → optotype rendering**

This is fundamental to making the system usable across different smartphone models.

---

### 1.4 Optotype

The working prototype will use a **Tumbling E optotype** rather than alphabetic letters.

Reason:

* does not require English literacy;
* reduces language dependence;
* is appropriate for children;
* supports directional responses;
* can be randomized;
* works naturally with touch input.

---

### 1.5 User input

The MVP will use:

> **Touch-based directional input**

The user sees the Tumbling E and selects:

```text
↑
↓
←
→
```

This deliberately avoids requiring:

* microphone quality;
* speech recognition;
* camera capability;
* internet connectivity;
* advanced AI.

Voice and vision-based interaction will be designed as future interchangeable input modules.

---

### 1.6 Data architecture

Data collection is a **first-class component**, not an afterthought.

Every screening session should produce structured data that can eventually support:

* clinical validation;
* statistical analysis;
* model development;
* anonymized population-level analysis;
* future AI training.

The ANRF proposal explicitly includes anonymized data/reporting as part of the broader system. 

For the MVP, local persistence is sufficient.

---

### 1.7 AI

**AI is explicitly out of the critical path for the MVP.**

Future AI capabilities may include:

* camera-based distance estimation;
* computer-vision interaction;
* ocular-image analysis;
* speech analysis;
* multimodal models;
* screening-risk prediction.

The proposal identifies image, video and speech modalities as components of the larger research platform. 

The prototype should therefore be architected so these capabilities can be added later without rewriting the core screening engine.

---

# 2. Overall Execution Architecture

The coding work should proceed through the following phases:

```text
PHASE 0
Project Understanding & Technical Setup
        ↓
PHASE 1
Data Model & Screening Protocol
        ↓
PHASE 2
Calibration & Measurement Engine
        ↓
PHASE 3
Optotype & Visual-Acuity Test Engine
        ↓
PHASE 4
Input & Interaction Layer
        ↓
PHASE 5
Android Application Integration
        ↓
PHASE 6
Results & Data Persistence
        ↓
PHASE 7
Cross-Device Testing & Validation
        ↓
PHASE 8
Prototype Hardening & Demonstration Build
```

AI, voice and computer vision remain future extension layers.

---

# PHASE 0 — Project Understanding and Technical Foundation

## Main Goal

Ensure the coding agent understands the project, constraints, intended users and architectural decisions **before writing application code**.

The agent should not immediately start implementing screens.

It should first establish the technical interpretation of the project and the MVP boundary.

## Things to be done

* Review the available project documentation.
* Understand the broader ANRF project objective.
* Understand the SwasthyaTech submission context.
* Identify what belongs to the two-day prototype versus the longer-term research system.
* Establish the Android technology stack.
* Inspect the existing repository/project structure, if one exists.
* Determine whether the project is starting from scratch.
* Establish a clean package/module structure.
* Document the agreed MVP decisions.
* Explicitly identify assumptions that may later require clinical validation.

## Deliverables

1. **Technical project understanding**
2. **MVP scope definition**
3. **Initial Android project structure**
4. **Architecture/module plan**
5. **Explicit list of MVP exclusions**
6. **Buildable initial Android project**

## Important boundary

The agent should not introduce AI, cloud infrastructure, camera processing or speech recognition merely because those appear in the larger project proposal.

---

# PHASE 1 — Data Model and Screening Protocol

## Main Goal

Define exactly **what the application measures, what it records and how a screening session is represented**.

This phase comes before the actual test implementation because the screening engine should generate structured research-ready data from its first execution.

## Things to be done

Define the complete screening-session structure.

At minimum, capture:

### Participant/session metadata

* session ID;
* participant ID or anonymized identifier;
* timestamp;
* age, where appropriate;
* device model;
* Android version;
* screen resolution;
* screen dimensions/density where available;
* calibration parameters;
* testing distance.

### Test metadata

* optotype type;
* eye being tested;
* target acuity;
* trial number;
* displayed optotype orientation;
* user's response;
* correctness;
* response time.

### Results

* right-eye result;
* left-eye result;
* visual-acuity representation;
* logMAR representation where implemented;
* test completion status;
* calibration status.

The data structure should be designed so that future fields can be added without breaking existing records.

## Deliverables

1. **Screening session data model**
2. **Trial-level data model**
3. **Result data model**
4. **Local persistence mechanism**
5. **Sample test dataset**
6. **Data-flow documentation**

## Expected architecture

```text
Screening Session
      │
      ├── Participant Metadata
      │
      ├── Calibration Metadata
      │
      ├── Test Configuration
      │
      ├── Right Eye Trials
      │
      ├── Left Eye Trials
      │
      └── Final Results
```

## Important principle

Do not store only the final answer.

The individual trial responses should be preserved because they may become valuable for future clinical validation and AI development.

---

# PHASE 2 — Calibration and Measurement Engine

## Main Goal

Build the **scientific/measurement core of the prototype**.

This is the most important technical phase.

The objective is to convert:

```text
Target visual acuity
+
Viewing distance
+
Screen calibration
```

into:

```text
Correct physical optotype size
+
Correct pixel dimensions
```

## Things to be done

### 2.1 Screen calibration

Implement a manual calibration mechanism.

The user should be shown a known physical reference and instructed to adjust the displayed reference until it matches the physical measurement.

The application should derive:

```text
pixels per millimetre
```

and store the calibration result.

---

### 2.2 Viewing distance

Use the agreed MVP distance:

> **400 mm / 40 cm**

The prototype should clearly instruct the user to maintain the specified distance.

Do not introduce camera-based distance estimation in this phase.

---

### 2.3 Optotype scaling

Implement the mathematical conversion from desired visual angle and viewing distance to physical optotype dimensions.

Conceptually:

```text
Target acuity
      ↓
Required visual angle
      ↓
Required physical optotype size
      ↓
Physical size in mm
      ↓
pixels/mm
      ↓
Optotype dimensions in pixels
```

The implementation should use physical dimensions rather than arbitrary Android font sizes.

---

### 2.4 Device independence

The calculation must work independently of:

* screen resolution;
* screen density;
* physical screen size;
* manufacturer.

The same physical optotype should therefore be rendered at approximately the same physical size on different phones after calibration.

## Deliverables

1. **Calibration screen**
2. **Calibration storage**
3. **Physical-unit calculation engine**
4. **Optotype-size calculation engine**
5. **Viewing-distance configuration**
6. **Unit tests for the mathematical calculations**
7. **Calibration/debug information**
8. **Documentation of the calculation pipeline**

## Acceptance criterion

A known physical calibration dimension should render approximately at the same physical size after calibration, and the calculated optotype dimensions should remain physically consistent across different Android devices.

---

# PHASE 3 — Optotype and Visual-Acuity Test Engine

## Main Goal

Implement the actual visual-acuity testing mechanism independently of the Android UI.

The test engine should be a reusable component rather than code embedded directly inside individual screens.

## Things to be done

### 3.1 Tumbling E generation

Implement a reusable Tumbling E renderer capable of displaying:

* up;
* down;
* left;
* right.

The renderer must accept a calculated physical/pixel size.

---

### 3.2 Randomization

The orientation of each optotype should be randomized so that the participant cannot simply memorize a sequence.

---

### 3.3 Acuity levels

Define the supported acuity levels for the MVP.

The exact levels should be represented internally in a consistent numerical form rather than hard-coded as visual strings throughout the UI.

---

### 3.4 Adaptive testing

Implement the agreed screening strategy.

The engine should:

```text
Display optotype
      ↓
Capture response
      ↓
Determine correctness
      ↓
Select next difficulty
      ↓
Repeat
      ↓
Determine threshold
```

The testing algorithm should be isolated from the UI so that the algorithm can later be modified without redesigning the application.

---

### 3.5 Monocular testing

The MVP should support:

```text
Right Eye
   ↓
Left Eye
```

The application should explicitly indicate which eye is currently being tested.

The other eye should be covered using an appropriate physical occlusion instruction/accessory rather than attempting to solve occlusion through software.

## Deliverables

1. Tumbling-E renderer
2. Optotype randomization
3. Acuity-level configuration
4. Adaptive test controller
5. Right-eye test
6. Left-eye test
7. Trial recording
8. Test-engine unit tests

---

# PHASE 4 — Input and Interaction Layer

## Main Goal

Create a hardware-light interaction system suitable for the project's target population.

## Primary MVP input

**Touch-based directional selection.**

Example:

```text
             ↑

        [ OPTOTYPE ]

       ←    ↓    →
```

The exact layout can be optimized for usability.

## Things to be done

* Create large touch targets.
* Make directional options visually obvious.
* Minimize text.
* Avoid dependence on English literacy.
* Prevent accidental double submissions.
* Provide immediate transition to the next trial.
* Clearly indicate which eye is being tested.
* Provide simple instructions before testing.
* Make the interaction usable by a child with minimal supervision.

## Future interface abstraction

The input architecture should allow:

```text
InputProvider
    │
    ├── TouchInput       ← MVP
    ├── VoiceInput       ← Future
    └── VisionInput      ← Future
```

This is important.

The test engine should not care whether the answer came from:

```text
touch
voice
camera
```

It should simply receive:

```text
UP / DOWN / LEFT / RIGHT
```

## Deliverables

1. Touch input component
2. Direction-selection interface
3. Instruction screens
4. Eye-selection interface
5. Input abstraction/interface for future modalities
6. Interaction validation

---

# PHASE 5 — Android Application Integration

## Main Goal

Combine the independently developed measurement, testing, input and data components into one coherent Android workflow.

## Target user flow

```text
Launch
  ↓
Start Screening
  ↓
Participant / Session Setup
  ↓
Device Calibration
  ↓
Testing Instructions
  ↓
Viewing Distance Instructions
  ↓
Right Eye Test
  ↓
Left Eye Test
  ↓
Results
  ↓
Save Session
```

## Things to be done

Build the actual application screens.

### Screen 1 — Home

Purpose:

* explain the purpose of the screening;
* start a new screening session.

---

### Screen 2 — Session Setup

Collect only the information genuinely required by the MVP.

Avoid unnecessary personal data.

---

### Screen 3 — Calibration

Run the manual calibration procedure.

---

### Screen 4 — Test Instructions

Explain:

* viewing distance;
* eye being tested;
* how to respond;
* need to follow the optotype carefully.

---

### Screen 5 — Right Eye

Run the complete adaptive test.

---

### Screen 6 — Left Eye

Run the complete adaptive test.

---

### Screen 7 — Results

Present the screening result clearly.

---

### Screen 8 — Session Saved

Confirm that the screening data has been stored.

## Deliverables

1. Fully navigable Android application
2. Integrated calibration
3. Integrated testing engine
4. Integrated touch input
5. Integrated local persistence
6. Functional end-to-end workflow

---

# PHASE 6 — Results and Data Persistence

## Main Goal

Make the prototype capable of producing **structured, reproducible screening records** rather than merely displaying a result on screen.

## Things to be done

### Result calculation

Convert the test performance into the selected visual-acuity representation.

Where appropriate, maintain both:

```text
Human-readable representation
+
Numerical/logMAR representation
```

---

### Result screen

Show separately:

```text
Right Eye
Left Eye
```

and relevant testing metadata.

The result should be described as a **screening result**, not a definitive medical diagnosis.

---

### Data storage

Persist:

* session metadata;
* calibration data;
* viewing distance;
* all trials;
* responses;
* calculated results.

---

### Future compatibility

The data model should eventually support:

```text
Android
   ↓
Secure backend
   ↓
Anonymized dataset
   ↓
Clinical validation
   ↓
AI development
   ↓
Population-level analytics
```

The current prototype only needs the first portion.

## Deliverables

1. Result calculation module
2. Result screen
3. Local database/storage
4. Session retrieval
5. Structured export/debug representation
6. Sample screening records

---

# PHASE 7 — Cross-Device Testing and Technical Validation

## Main Goal

Verify that the fundamental claim of the prototype works across different Android devices.

This phase is critical because **device independence is one of the central engineering challenges of the project**.

## Things to be tested

Use multiple Android devices where available.

Ideally test devices with:

* different screen sizes;
* different resolutions;
* different manufacturers;
* different Android versions;
* different screen densities.

### Calibration test

Verify that the calibrated physical dimensions are consistent.

### Optotype test

Verify that a particular target acuity produces approximately the expected physical optotype size.

### UI test

Verify:

* buttons;
* screen transitions;
* readability;
* responsiveness;
* orientation;
* touch accuracy.

### Screening test

Run complete sessions:

```text
Calibration
→ Right eye
→ Left eye
→ Result
→ Data saved
```

### Data test

Verify that no trials are lost and that the final result corresponds to the recorded responses.

## Deliverables

1. Cross-device test report
2. Calibration comparison
3. Bug list
4. Fixed critical issues
5. Tested APK
6. Reproducible test procedure

---

# PHASE 8 — Prototype Hardening and Demonstration Build

## Main Goal

Turn the functional engineering prototype into a **stable demonstration-ready SwasthyaTech prototype**.

The focus here is not adding new technology.

The focus is making the existing technology reliable and understandable.

## Things to be done

### Reliability

* Remove crashes.
* Handle incomplete sessions.
* Handle calibration failures.
* Prevent invalid inputs.
* Handle app restarts gracefully where feasible.

### Usability

Reduce unnecessary text and complexity.

The intended operator may be a:

* teacher;
* community worker;
* Anganwadi worker;
* other non-expert.

This aligns with the broader proposal's intended deployment model. 

### Demonstration mode

Prepare a clean demonstration sequence:

```text
Start
 ↓
Calibration
 ↓
Distance setup
 ↓
Right eye
 ↓
Left eye
 ↓
Result
 ↓
Saved screening
```

### Documentation

Produce:

* README;
* installation instructions;
* prototype architecture;
* test instructions;
* known limitations;
* future-development roadmap.

## Deliverables

1. Stable APK
2. Final source code
3. README
4. Architecture documentation
5. Test report
6. Prototype demonstration workflow
7. Known limitations document

---

# 3. Features Explicitly Deferred Beyond the MVP

The following should **not become blockers** during the two-day implementation.

## Voice interaction

Future:

```text
Child says "left"
        ↓
Speech recognition
        ↓
Normalized response
        ↓
Test engine
```

Potential considerations:

* multilingual speech;
* offline speech recognition;
* low-quality microphones;
* background noise;
* accent variation.

---

## Camera-based distance estimation

Future:

```text
Front camera
     ↓
Face detection
     ↓
Eye/face geometry
     ↓
Distance estimation
     ↓
Dynamic optotype scaling
```

This can eventually eliminate the manual 40-cm distance assumption.

---

## Computer vision

Potential future capabilities:

* posture/distance monitoring;
* response through gestures;
* ocular-region analysis;
* eye-position estimation.

---

## AI/Transformer layer

The broader proposal envisions Transformer-based multimodal AI and image/video/speech processing. 

This should come **after sufficient structured data and validated measurement methodology exist**.

Potential future pipeline:

```text
Validated screening data
        +
Clinical labels
        +
Image/video/speech data
        ↓
Feature extraction
        ↓
AI/ML models
        ↓
Risk prediction
        ↓
Screening recommendation
```

The AI should augment a validated screening process rather than compensate for an unvalidated measurement system.

---

# 4. Final MVP Definition

At the end of the two-day execution, the prototype should be capable of doing this:

```text
┌──────────────────────────────────────────┐
│          SWASTHYATECH MVP                │
│                                          │
│  Android Smartphone                     │
│          │                               │
│          ▼                               │
│  Manual Screen Calibration              │
│          │                               │
│          ▼                               │
│  40-cm Controlled Viewing Distance      │
│          │                               │
│          ▼                               │
│  Calibrated Tumbling-E Optotypes        │
│          │                               │
│          ▼                               │
│  Touch Direction Response               │
│          │                               │
│          ▼                               │
│  Adaptive Acuity Testing                │
│          │                               │
│          ▼                               │
│  Right Eye + Left Eye                   │
│          │                               │
│          ▼                               │
│  Visual-Acuity Screening Result         │
│          │                               │
│          ▼                               │
│  Structured Local Data Record            │
└──────────────────────────────────────────┘
```

---

# 5. Definition of Done

The prototype should be considered complete only when all of the following are true:

### Measurement

* [ ] Screen can be calibrated.
* [ ] Calibration is stored.
* [ ] Viewing distance is explicitly defined.
* [ ] Optotype dimensions are calculated from physical measurement principles.
* [ ] Optotypes are not based on arbitrary `sp`/font sizes.

### Testing

* [ ] Tumbling E is implemented.
* [ ] Orientations are randomized.
* [ ] Touch responses work.
* [ ] Right eye can be tested.
* [ ] Left eye can be tested.
* [ ] Adaptive testing works.
* [ ] Trials are recorded.

### Data

* [ ] Session IDs are generated.
* [ ] Calibration information is stored.
* [ ] Device information is recorded.
* [ ] Trial-level responses are stored.
* [ ] Final results are stored.
* [ ] Data can be retrieved/exported for development/testing.

### Android

* [ ] Application builds successfully.
* [ ] Application runs on a physical Android device.
* [ ] Complete screening flow works without developer intervention.
* [ ] UI is usable on a normal-sized smartphone.
* [ ] No internet connection is required for the core MVP.

### Validation

* [ ] Prototype has been tested on more than one Android device where possible.
* [ ] Calibration has been physically checked.
* [ ] Optotype scaling has been checked.
* [ ] Test results are reproducible.
* [ ] Known limitations are documented.

---

# 6. Architectural Principle for the Coding Agent

The most important instruction across all phases is:

> **Keep the measurement engine independent from the user interface and independent from the input modality.**

The architecture should ultimately permit:

```text
                 ┌── Touch
                 │
Input Layer ─────┼── Voice
                 │
                 └── Computer Vision
                         │
                         ▼
                 Test Controller
                         │
                         ▼
                  Measurement Engine
                         │
             ┌───────────┴───────────┐
             ▼                       ▼
        Calibration              Optotype
             │                       │
             └───────────┬───────────┘
                         ▼
                    Result Engine
                         │
                         ▼
                    Data Layer
```

This prevents the MVP from becoming a dead-end prototype.

---

# 7. Priority Order for the Coding Agent

If time becomes constrained, implementation priority should be:

**P0 — Must work**

1. Android project
2. Data model
3. Screen calibration
4. 40-cm testing methodology
5. Optotype scaling
6. Tumbling E
7. Touch response
8. Right/left eye test
9. Result calculation
10. Local data storage

**P1 — Should work**

11. Adaptive test refinement
12. Polished instructions
13. Better result presentation
14. Cross-device testing
15. Export/debug data

**P2 — Only if time remains**

16. Voice-input abstraction
17. Additional UI polish
18. Demo mode
19. Advanced reporting

**Explicitly P3 / future**

20. Speech recognition
21. Camera distance estimation
22. Computer vision
23. AI/Transformer models
24. Cloud infrastructure
25. Clinical-validation analytics

---

## Final execution philosophy

The prototype should progress in this order:

> **Define what we measure → establish how we measure it → calibrate the physical measurement → implement the optotype test → capture responses → integrate into Android → store the data → validate across devices → only then introduce automation and AI.**

This order is consistent with the project's broader goal of developing a low-cost, scalable screening platform while keeping the two-day prototype focused on a demonstrable core technology. The proposal assigns clinical validation, backend/data management, UI/usability and AI validation as distinct responsibilities, reinforcing the need for a modular architecture rather than a single AI-heavy application. 

The SwasthyaTech requirements also explicitly value prototype maturity, technical feasibility, clinical impact and scalability, so the immediate objective should be a **small but technically defensible working system**, not an incomplete attempt at the entire long-term platform. 
