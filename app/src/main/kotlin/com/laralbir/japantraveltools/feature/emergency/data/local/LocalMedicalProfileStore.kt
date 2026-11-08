package com.laralbir.japantraveltools.feature.emergency.data.local

import android.content.Context
import com.laralbir.japantraveltools.feature.emergency.domain.model.MedicalEmergencyProfile
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

class LocalMedicalProfileStore(
    private val storageDir: File
) {

    constructor(context: Context) : this(context.filesDir)

    private val profileFile = File(storageDir, FILE_NAME)
    private val mutex = Mutex()

    suspend fun getProfile(): MedicalEmergencyProfile = mutex.withLock {
        if (!profileFile.exists()) {
            return@withLock MedicalEmergencyProfile()
        }
        try {
            val content = profileFile.readText()
            if (content.isBlank()) {
                MedicalEmergencyProfile()
            } else {
                parseJsonToProfile(content)
            }
        } catch (_: Exception) {
            MedicalEmergencyProfile()
        }
    }

    suspend fun saveProfile(profile: MedicalEmergencyProfile) = mutex.withLock {
        try {
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val jsonString = serializeProfileToJson(profile)
            profileFile.writeText(jsonString)
        } catch (_: Exception) {
            // Silently fall back to avoid crashing during emergency operations
        }
    }

    private fun serializeProfileToJson(profile: MedicalEmergencyProfile): String {
        val json = JSONObject()
        json.put("fullName", profile.fullName)
        json.put("bloodType", profile.bloodType)

        val allergiesArray = JSONArray()
        for (allergy in profile.allergies) {
            allergiesArray.put(allergy)
        }
        json.put("allergies", allergiesArray)

        json.put("conditions", profile.conditions)
        json.put("medications", profile.medications)
        json.put("insuranceCompany", profile.insuranceCompany)
        json.put("insurancePolicyNumber", profile.insurancePolicyNumber)
        json.put("insuranceEmergencyPhone", profile.insuranceEmergencyPhone)
        json.put("emergencyContactName", profile.emergencyContactName)
        json.put("emergencyContactPhone", profile.emergencyContactPhone)
        json.put("emergencyContactRelation", profile.emergencyContactRelation)

        return json.toString(2)
    }

    private fun parseJsonToProfile(jsonString: String): MedicalEmergencyProfile {
        val json = JSONObject(jsonString)
        val allergiesList = mutableListOf<String>()
        val allergiesArray = json.optJSONArray("allergies")
        if (allergiesArray != null) {
            for (i in 0 until allergiesArray.length()) {
                val item = allergiesArray.optString(i)
                if (item.isNotBlank()) {
                    allergiesList.add(item)
                }
            }
        }

        return MedicalEmergencyProfile(
            fullName = json.optString("fullName", ""),
            bloodType = json.optString("bloodType", ""),
            allergies = allergiesList,
            conditions = json.optString("conditions", ""),
            medications = json.optString("medications", ""),
            insuranceCompany = json.optString("insuranceCompany", ""),
            insurancePolicyNumber = json.optString("insurancePolicyNumber", ""),
            insuranceEmergencyPhone = json.optString("insuranceEmergencyPhone", ""),
            emergencyContactName = json.optString("emergencyContactName", ""),
            emergencyContactPhone = json.optString("emergencyContactPhone", ""),
            emergencyContactRelation = json.optString("emergencyContactRelation", "")
        )
    }

    companion object {
        const val FILE_NAME = "medical_emergency_profile.json"
    }
}
