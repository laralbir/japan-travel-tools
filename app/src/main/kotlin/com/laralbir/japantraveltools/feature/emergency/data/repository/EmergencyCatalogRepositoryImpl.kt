package com.laralbir.japantraveltools.feature.emergency.data.repository

import com.laralbir.japantraveltools.feature.emergency.data.catalog.SeedEmergencyCatalog
import com.laralbir.japantraveltools.feature.emergency.domain.model.ConsularMissionItem
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyCategory
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyContactNumber
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyPhraseItem
import com.laralbir.japantraveltools.feature.emergency.domain.repository.EmergencyCatalogRepository

class EmergencyCatalogRepositoryImpl(
    private val catalog: SeedEmergencyCatalog = SeedEmergencyCatalog
) : EmergencyCatalogRepository {

    override fun getEmergencyNumbers(): List<EmergencyContactNumber> {
        return catalog.emergencyNumbers
    }

    override fun getConsularMissions(countryCode: String?): List<ConsularMissionItem> {
        if (countryCode.isNullOrBlank()) {
            return catalog.consularMissions
        }
        return catalog.consularMissions.filter {
            it.countryCode.equals(countryCode.trim(), ignoreCase = true)
        }
    }

    override fun getEmergencyPhrases(category: EmergencyCategory?): List<EmergencyPhraseItem> {
        if (category == null) {
            return catalog.emergencyPhrases
        }
        return catalog.emergencyPhrases.filter { it.category == category }
    }
}
