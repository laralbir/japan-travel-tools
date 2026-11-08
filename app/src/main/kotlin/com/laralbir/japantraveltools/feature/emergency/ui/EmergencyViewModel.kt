package com.laralbir.japantraveltools.feature.emergency.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.laralbir.japantraveltools.feature.emergency.data.local.LocalMedicalProfileStore
import com.laralbir.japantraveltools.feature.emergency.data.repository.EmergencyCatalogRepositoryImpl
import com.laralbir.japantraveltools.feature.emergency.data.repository.MedicalProfileRepositoryImpl
import com.laralbir.japantraveltools.feature.emergency.domain.model.ConsularMissionItem
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyCategory
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyContactNumber
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyPhraseItem
import com.laralbir.japantraveltools.feature.emergency.domain.model.MedicalEmergencyProfile
import com.laralbir.japantraveltools.feature.emergency.domain.repository.EmergencyCatalogRepository
import com.laralbir.japantraveltools.feature.emergency.domain.repository.MedicalProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EmergencyTab {
    NUMBERS,
    CONSULATES,
    MEDICAL,
    PHRASES
}

data class EmergencyUiState(
    val selectedTab: EmergencyTab = EmergencyTab.NUMBERS,
    val emergencyNumbers: List<EmergencyContactNumber> = emptyList(),
    val selectedCountryCode: String = "ES",
    val availableCountries: List<Pair<String, String>> = listOf(
        "" to "Todos / All",
        "ES" to "España",
        "MX" to "México",
        "CO" to "Colombia",
        "AR" to "Argentina",
        "CL" to "Chile",
        "PE" to "Perú"
    ),
    val consularMissions: List<ConsularMissionItem> = emptyList(),
    val medicalProfile: MedicalEmergencyProfile = MedicalEmergencyProfile(),
    val isSavingProfile: Boolean = false,
    val profileSaveSuccess: Boolean = false,
    val selectedPhraseCategory: EmergencyCategory? = null,
    val emergencyPhrases: List<EmergencyPhraseItem> = emptyList()
)

class EmergencyViewModel(
    private val catalogRepository: EmergencyCatalogRepository = EmergencyCatalogRepositoryImpl(),
    private val medicalProfileRepository: MedicalProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val numbers = catalogRepository.getEmergencyNumbers()
        val missions = catalogRepository.getConsularMissions(_uiState.value.selectedCountryCode)
        val phrases = catalogRepository.getEmergencyPhrases(_uiState.value.selectedPhraseCategory)

        _uiState.update { current ->
            current.copy(
                emergencyNumbers = numbers,
                consularMissions = missions,
                emergencyPhrases = phrases
            )
        }

        viewModelScope.launch {
            val profile = medicalProfileRepository.getMedicalProfile()
            _uiState.update { it.copy(medicalProfile = profile) }
        }
    }

    fun selectTab(tab: EmergencyTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectCountry(countryCode: String) {
        val filtered = catalogRepository.getConsularMissions(countryCode.ifBlank { null })
        _uiState.update {
            it.copy(
                selectedCountryCode = countryCode,
                consularMissions = filtered
            )
        }
    }

    fun selectPhraseCategory(category: EmergencyCategory?) {
        val filtered = catalogRepository.getEmergencyPhrases(category)
        _uiState.update {
            it.copy(
                selectedPhraseCategory = category,
                emergencyPhrases = filtered
            )
        }
    }

    fun saveMedicalProfile(profile: MedicalEmergencyProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, profileSaveSuccess = false) }
            medicalProfileRepository.updateMedicalProfile(profile)
            _uiState.update {
                it.copy(
                    medicalProfile = profile,
                    isSavingProfile = false,
                    profileSaveSuccess = true
                )
            }
        }
    }

    fun resetProfileSaveSuccess() {
        _uiState.update { it.copy(profileSaveSuccess = false) }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val store = LocalMedicalProfileStore(context.applicationContext.filesDir)
                    val profileRepo = MedicalProfileRepositoryImpl(store)
                    val catalogRepo = EmergencyCatalogRepositoryImpl()
                    return EmergencyViewModel(catalogRepo, profileRepo) as T
                }
            }
    }
}
