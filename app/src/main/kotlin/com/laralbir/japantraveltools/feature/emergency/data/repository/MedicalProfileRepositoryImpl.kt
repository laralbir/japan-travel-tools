package com.laralbir.japantraveltools.feature.emergency.data.repository

import com.laralbir.japantraveltools.feature.emergency.data.local.LocalMedicalProfileStore
import com.laralbir.japantraveltools.feature.emergency.domain.model.MedicalEmergencyProfile
import com.laralbir.japantraveltools.feature.emergency.domain.repository.MedicalProfileRepository

class MedicalProfileRepositoryImpl(
    private val localStore: LocalMedicalProfileStore
) : MedicalProfileRepository {

    override suspend fun getMedicalProfile(): MedicalEmergencyProfile {
        return localStore.getProfile()
    }

    override suspend fun updateMedicalProfile(profile: MedicalEmergencyProfile) {
        localStore.saveProfile(profile)
    }
}
