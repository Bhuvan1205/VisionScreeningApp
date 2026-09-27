package com.example.swasthyatech.data

object SampleFixtures {

    val sampleCalibration = DeviceCalibration(
        deviceManufacturer = "TestCorp",
        deviceModel = "TestPhone 1",
        screenResolution = "1080x1920",
        screenDensity = 2.0f,
        calibrationMethod = "MANUAL_REFERENCE_CARD",
        pixelsPerMm = 5.5f,
        calibrationTimestamp = 1672531200000L,
        isValid = true
    )

    val sampleViewingDistance = ViewingDistance(
        targetDistanceMm = 400f,
        actualDistanceMm = null
    )

    val sampleTestConfig = TestConfiguration(
        optotypeType = OptotypeType.TUMBLING_E,
        supportedOrientations = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT),
        acuityLevels = listOf(1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0.0f),
        testAlgorithmVersion = "1.0",
        viewingDistance = sampleViewingDistance,
        deviceCalibration = sampleCalibration
    )

    fun createTrials(eye: Eye): List<Trial> {
        return listOf(
            Trial(1, eye, 0.8f, OptotypeType.TUMBLING_E, Direction.UP, Direction.UP, true, 1200L, 1672531210000L),
            Trial(2, eye, 0.6f, OptotypeType.TUMBLING_E, Direction.LEFT, Direction.LEFT, true, 1000L, 1672531212000L),
            Trial(3, eye, 0.4f, OptotypeType.TUMBLING_E, Direction.DOWN, Direction.UP, false, 1500L, 1672531215000L),
            Trial(4, eye, 0.5f, OptotypeType.TUMBLING_E, Direction.RIGHT, Direction.RIGHT, true, 900L, 1672531217000L)
        )
    }

    val sampleRightEyeTestCompleted = EyeTestResult(
        eye = Eye.RIGHT,
        trials = createTrials(Eye.RIGHT),
        status = TestStatus.COMPLETED,
        calculatedAcuity = 0.5f,
        calculatedAcuityFraction = "6/18"
    )

    val sampleLeftEyeTestCompleted = EyeTestResult(
        eye = Eye.LEFT,
        trials = createTrials(Eye.LEFT),
        status = TestStatus.COMPLETED,
        calculatedAcuity = 0.5f,
        calculatedAcuityFraction = "6/18"
    )

    // 1. One completed two-eye session
    val completedSession = ScreeningSession(
        sessionId = "SESSION_TEST_001",
        participantId = "PARTICIPANT_999",
        timestamp = 1672531200000L,
        schemaVersion = 1,
        testConfiguration = sampleTestConfig,
        rightEyeTest = sampleRightEyeTestCompleted,
        leftEyeTest = sampleLeftEyeTestCompleted,
        finalResult = FinalResult(
            rightEyeAcuityLogMar = 0.5f,
            leftEyeAcuityLogMar = 0.5f,
            overallStatus = TestStatus.COMPLETED,
            resultAlgorithmVersion = "1.0",
            clinicalInterpretation = null
        ),
        sessionStatus = TestStatus.COMPLETED
    )

    // 2. One incomplete session
    val incompleteSession = ScreeningSession(
        sessionId = "SESSION_TEST_002",
        participantId = "PARTICIPANT_888",
        timestamp = 1672532200000L,
        schemaVersion = 1,
        testConfiguration = sampleTestConfig,
        rightEyeTest = null,
        leftEyeTest = null,
        finalResult = null,
        sessionStatus = TestStatus.NOT_STARTED
    )

    // 3. One completed right-eye test only
    val rightEyeOnlySession = ScreeningSession(
        sessionId = "SESSION_TEST_003",
        participantId = "PARTICIPANT_777",
        timestamp = 1672533200000L,
        schemaVersion = 1,
        testConfiguration = sampleTestConfig,
        rightEyeTest = sampleRightEyeTestCompleted,
        leftEyeTest = null,
        finalResult = null,
        sessionStatus = TestStatus.IN_PROGRESS
    )
}
