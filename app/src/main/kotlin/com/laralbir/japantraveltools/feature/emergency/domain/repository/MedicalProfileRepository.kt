package com.laralbir.japantraveltools.feature.emergency.domain.repository

import com.laralbir.japantraveltools.feature.emergency.domain.model.MedicalEmergencyProfile

interface MedicalProfileRepository {
    suspend fun getMedicalProfile(): MedicalEmergencyProfile
    suspend fun updateMedicalProfile(profile: MedicalEmergencyProfile)
}
