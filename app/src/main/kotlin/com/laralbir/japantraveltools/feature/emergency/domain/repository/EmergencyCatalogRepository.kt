package com.laralbir.japantraveltools.feature.emergency.domain.repository

import com.laralbir.japantraveltools.feature.emergency.domain.model.ConsularMissionItem
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyCategory
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyContactNumber
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyPhraseItem

interface EmergencyCatalogRepository {
    fun getEmergencyNumbers(): List<EmergencyContactNumber>
    fun getConsularMissions(countryCode: String? = null): List<ConsularMissionItem>
    fun getEmergencyPhrases(category: EmergencyCategory? = null): List<EmergencyPhraseItem>
}
