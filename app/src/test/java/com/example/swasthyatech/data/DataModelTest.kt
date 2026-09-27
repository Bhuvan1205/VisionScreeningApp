package com.example.swasthyatech.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DataModelTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `test complete session serialization and deserialization`() {
        val originalSession = SampleFixtures.completedSession
        val jsonString = json.encodeToString(originalSession)
        
        val decodedSession = json.decodeFromString<ScreeningSession>(jsonString)
        
        assertEquals(originalSession.sessionId, decodedSession.sessionId)
        assertEquals(originalSession.schemaVersion, decodedSession.schemaVersion)
        assertEquals(TestStatus.COMPLETED, decodedSession.sessionStatus)
        assertNotNull(decodedSession.rightEyeTest)
        assertNotNull(decodedSession.leftEyeTest)
        assertEquals(4, decodedSession.rightEyeTest?.trials?.size)
    }

    @Test
    fun `test incomplete session serialization and deserialization`() {
        val originalSession = SampleFixtures.incompleteSession
        val jsonString = json.encodeToString(originalSession)
        
        val decodedSession = json.decodeFromString<ScreeningSession>(jsonString)
        
        assertEquals(originalSession.sessionId, decodedSession.sessionId)
        assertEquals(TestStatus.NOT_STARTED, decodedSession.sessionStatus)
        assertNull(decodedSession.rightEyeTest)
        assertNull(decodedSession.leftEyeTest)
    }

    @Test
    fun `test right eye only session serialization and deserialization`() {
        val originalSession = SampleFixtures.rightEyeOnlySession
        val jsonString = json.encodeToString(originalSession)
        
        val decodedSession = json.decodeFromString<ScreeningSession>(jsonString)
        
        assertEquals(originalSession.sessionId, decodedSession.sessionId)
        assertEquals(TestStatus.IN_PROGRESS, decodedSession.sessionStatus)
        assertNotNull(decodedSession.rightEyeTest)
        assertNull(decodedSession.leftEyeTest)
        assertEquals(Eye.RIGHT, decodedSession.rightEyeTest?.eye)
    }

    @Test
    fun `test trial separation`() {
        val trials = SampleFixtures.createTrials(Eye.RIGHT)
        
        val hasRightEye = trials.all { it.eye == Eye.RIGHT }
        assertEquals(true, hasRightEye)
        
        val trialWithIncorrectResponse = trials.find { !it.isCorrect }
        assertNotNull(trialWithIncorrectResponse)
        assertEquals(Direction.DOWN, trialWithIncorrectResponse?.presentedOrientation)
        assertEquals(Direction.UP, trialWithIncorrectResponse?.userResponse)
    }
}
