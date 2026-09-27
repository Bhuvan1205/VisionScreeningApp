package com.example.swasthyatech.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Handles persistence for the DeviceCalibration state.
 */
class CalibrationRepository(private val context: Context) {

    private val calibrationFile = File(context.filesDir, "active_calibration.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Saves the active calibration.
     */
    fun saveCalibration(calibration: DeviceCalibration): Boolean {
        return try {
            val jsonString = json.encodeToString(calibration)
            calibrationFile.writeText(jsonString)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Retrieves the currently active calibration. Validates against the current physical hardware
     * to prevent silent scaling corruption if app data was restored via backup to a new device.
     */
    fun getActiveCalibration(): DeviceCalibration? {
        return try {
            if (calibrationFile.exists()) {
                val jsonString = calibrationFile.readText()
                val calibration = json.decodeFromString<DeviceCalibration>(jsonString)
                
                val currentManufacturer = android.os.Build.MANUFACTURER ?: "Unknown"
                val currentModel = android.os.Build.MODEL ?: "Unknown"
                
                if (calibration.deviceManufacturer != currentManufacturer || calibration.deviceModel != currentModel) {
                    // Hardware mismatch. Invalidate and delete.
                    clearCalibration()
                    null
                } else {
                    calibration
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Clears the active calibration.
     */
    fun clearCalibration(): Boolean {
        return if (calibrationFile.exists()) {
            calibrationFile.delete()
        } else {
            false
        }
    }
}
