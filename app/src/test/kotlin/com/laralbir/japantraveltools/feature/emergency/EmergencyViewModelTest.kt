package com.laralbir.japantraveltools.feature.emergency

import com.laralbir.japantraveltools.feature.emergency.data.local.LocalMedicalProfileStore
import com.laralbir.japantraveltools.feature.emergency.data.repository.EmergencyCatalogRepositoryImpl
import com.laralbir.japantraveltools.feature.emergency.data.repository.MedicalProfileRepositoryImpl
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyCategory
import com.laralbir.japantraveltools.feature.emergency.domain.model.MedicalEmergencyProfile
import com.laralbir.japantraveltools.feature.emergency.ui.EmergencyTab
import com.laralbir.japantraveltools.feature.emergency.ui.EmergencyViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var storageDir: File
    private lateinit var store: LocalMedicalProfileStore
    private lateinit var medicalRepo: MedicalProfileRepositoryImpl
    private lateinit var catalogRepo: EmergencyCatalogRepositoryImpl
    private lateinit var viewModel: EmergencyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        storageDir = tempFolder.newFolder("vm_medical_data")
        store = LocalMedicalProfileStore(storageDir)
        medicalRepo = MedicalProfileRepositoryImpl(store)
        catalogRepo = EmergencyCatalogRepositoryImpl()
        viewModel = EmergencyViewModel(catalogRepo, medicalRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsEmergencyDataCorrectly() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(EmergencyTab.NUMBERS, state.selectedTab)
        assertTrue(state.emergencyNumbers.isNotEmpty())
        assertEquals("ES", state.selectedCountryCode)
        assertTrue(state.consularMissions.isNotEmpty())
        assertTrue(state.consularMissions.all { it.countryCode.equals("ES", ignoreCase = true) })
        assertTrue(state.emergencyPhrases.isNotEmpty())
    }

    @Test
    fun selectTab_updatesSelectedTabInState() = runTest {
        viewModel.selectTab(EmergencyTab.CONSULATES)
        assertEquals(EmergencyTab.CONSULATES, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(EmergencyTab.MEDICAL)
        assertEquals(EmergencyTab.MEDICAL, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(EmergencyTab.PHRASES)
        assertEquals(EmergencyTab.PHRASES, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun selectCountry_updatesSelectedCountryAndFiltersMissions() = runTest {
        viewModel.selectCountry("MX")
        val state = viewModel.uiState.value
        assertEquals("MX", state.selectedCountryCode)
        assertTrue(state.consularMissions.isNotEmpty())
        assertTrue(state.consularMissions.all { it.countryCode.equals("MX", ignoreCase = true) })

        viewModel.selectCountry("")
        val allState = viewModel.uiState.value
        assertEquals("", allState.selectedCountryCode)
        assertTrue(allState.consularMissions.size > state.consularMissions.size)
    }

    @Test
    fun selectPhraseCategory_filtersPhrases() = runTest {
        viewModel.selectPhraseCategory(EmergencyCategory.MEDICAL)
        val state = viewModel.uiState.value
        assertEquals(EmergencyCategory.MEDICAL, state.selectedPhraseCategory)
        assertTrue(state.emergencyPhrases.isNotEmpty())
        assertTrue(state.emergencyPhrases.all { it.category == EmergencyCategory.MEDICAL })

        viewModel.selectPhraseCategory(null)
        val allPhrasesState = viewModel.uiState.value
        assertEquals(null, allPhrasesState.selectedPhraseCategory)
        assertTrue(allPhrasesState.emergencyPhrases.size > state.emergencyPhrases.size)
    }

    @Test
    fun saveMedicalProfile_updatesRepositoryAndUiState() = runTest {
        advanceUntilIdle()
        val testProfile = MedicalEmergencyProfile(
            fullName = "Elena Naranjo",
            bloodType = "A+",
            allergies = listOf("Lactosa"),
            conditions = "Ninguna",
            medications = "Ninguna",
            insuranceCompany = "Mondo",
            insurancePolicyNumber = "POL-112233",
            insuranceEmergencyPhone = "+34 91 123 45 67",
            emergencyContactName = "Pedro Naranjo",
            emergencyContactPhone = "+34 611 222 333",
            emergencyContactRelation = "Padre"
        )

        viewModel.saveMedicalProfile(testProfile)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(testProfile, state.medicalProfile)
        assertTrue(state.profileSaveSuccess)
        assertFalse(state.isSavingProfile)

        viewModel.resetProfileSaveSuccess()
        assertFalse(viewModel.uiState.value.profileSaveSuccess)

        // Verify repository was also updated
        val fromRepo = medicalRepo.getMedicalProfile()
        assertEquals(testProfile, fromRepo)
    }
}
