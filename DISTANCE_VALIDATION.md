# Distance Validation Architecture

## Overview
Phase 7 shifts the assumption that the participant is *exactly* at 400 mm by introducing explicit distance validation metadata via the `DistanceValidation` domain model.

## Validation Methodologies
Presently, only `MANUAL_GUIDANCE` is supported as per safety constraints.
- **MANUAL_GUIDANCE**: The application explicitly pauses before the visual-acuity tests begin to present a localized setup screen (`DistanceValidationScreen`). It formally requests the operator or participant to physically position themselves using a ruler or string template to 40 cm (400 mm).

The prototype strictly avoids external tracking dependencies (e.g., AI/ML/ARCore) which could inject variable accuracy margins. Thus, no claims are made regarding continuous clinical distance adherence.

## Serialization
The validation outcome is captured inside the `ScreeningSession` payload:
```json
"distanceValidation": {
    "targetDistanceMm": 400.0,
    "measuredDistanceMm": 400.0,
    "toleranceMm": 20.0,
    "method": "MANUAL_GUIDANCE",
    "confidence": 0.5,
    "status": "VALIDATED"
}
```
If a test is manually guided, `confidence` is bounded at 0.5 to prevent algorithmic misinterpretation later (representing the lack of rigorous instrumental depth validation).

## Engine Dynamics
`MeasurementEngine` natively recalculates the trigonometric visual angle scaling according to the resolved measurement distance.
```kotlin
val sizeMm = 2.0 * viewingDistanceMm * tan(angleRadians / 2.0)
```
If `DistanceValidation` evaluates to `MANUAL_GUIDANCE`, the `targetDistanceMm` explicitly becomes the operating anchor driving adaptive sizing.
