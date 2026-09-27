package com.example.swasthyatech.data

import android.content.Context
import androidx.core.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class SessionRepository(private val context: Context) {

    private val sessionDir = File(context.filesDir, "screening_sessions").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun saveSession(session: ScreeningSession): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(sessionDir, "${session.sessionId}.json")
            val atomicFile = AtomicFile(file)
            val jsonString = json.encodeToString(session)
            
            var stream: FileOutputStream? = null
            try {
                stream = atomicFile.startWrite()
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(stream)
                true
            } catch (e: Exception) {
                atomicFile.failWrite(stream)
                e.printStackTrace()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getSession(sessionId: String): ScreeningSession? = withContext(Dispatchers.IO) {
        try {
            val file = File(sessionDir, "$sessionId.json")
            if (file.exists()) {
                val atomicFile = AtomicFile(file)
                val jsonString = atomicFile.readFully().toString(Charsets.UTF_8)
                json.decodeFromString<ScreeningSession>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAllSessions(): List<ScreeningSession> = withContext(Dispatchers.IO) {
        try {
            val files = sessionDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            files.mapNotNull { file ->
                try {
                    val atomicFile = AtomicFile(file)
                    val jsonString = atomicFile.readFully().toString(Charsets.UTF_8)
                    json.decodeFromString<ScreeningSession>(jsonString)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun deleteSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(sessionDir, "$sessionId.json")
        if (file.exists()) {
            AtomicFile(file).delete()
            true
        } else {
            false
        }
    }
    
    /**
     * Recovery mechanism for process death/orphaned sessions.
     * Sweeps the directory for IN_PROGRESS sessions and marks them INTERRUPTED.
     */
    suspend fun recoverOrphanedSessions() = withContext(Dispatchers.IO) {
        val sessions = getAllSessions()
        sessions.filter { it.sessionStatus == TestStatus.IN_PROGRESS }.forEach { orphaned ->
            val recovered = orphaned.copy(sessionStatus = TestStatus.INTERRUPTED)
            saveSession(recovered)
        }
    }
}
