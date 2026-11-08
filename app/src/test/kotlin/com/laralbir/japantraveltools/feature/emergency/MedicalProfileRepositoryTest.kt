package com.laralbir.japantraveltools.feature.emergency

import com.laralbir.japantraveltools.feature.emergency.data.local.LocalMedicalProfileStore
import com.laralbir.japantraveltools.feature.emergency.data.repository.MedicalProfileRepositoryImpl
import com.laralbir.japantraveltools.feature.emergency.domain.model.MedicalEmergencyProfile
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MedicalProfileRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var store: LocalMedicalProfileStore
    private lateinit var repository: MedicalProfileRepositoryImpl

    @Before
    fun setUp() {
        storageDir = tempFolder.newFolder("medical_data")
        store = LocalMedicalProfileStore(storageDir)
        repository = MedicalProfileRepositoryImpl(store)
    }

    @Test
    fun getMedicalProfile_whenNoFileExists_returnsDefaultEmptyProfile() = runTest {
        val profile = repository.getMedicalProfile()
        assertEquals("", profile.fullName)
        assertEquals("", profile.bloodType)
        assertTrue(profile.allergies.isEmpty())
        assertEquals("", profile.conditions)
        assertEquals("", profile.medications)
        assertEquals("", profile.insuranceCompany)
        assertEquals("", profile.insurancePolicyNumber)
        assertEquals("", profile.insuranceEmergencyPhone)
        assertEquals("", profile.emergencyContactName)
        assertEquals("", profile.emergencyContactPhone)
        assertEquals("", profile.emergencyContactRelation)
    }

    @Test
    fun updateMedicalProfile_persistsAndLoadsAllFields() = runTest {
        val testProfile = MedicalEmergencyProfile(
            fullName = "Carlos García",
            bloodType = "O+",
            allergies = listOf("Penicilina", "Frutos secos (cacahuetes)", "Marisco"),
            conditions = "Asma leve",
            medications = "Salbutamol inhalador si es necesario",
            insuranceCompany = "IATI Seguros Estrella",
            insurancePolicyNumber = "POL-987654321",
            insuranceEmergencyPhone = "+34 93 485 77 00",
            emergencyContactName = "María López",
            emergencyContactPhone = "+34 600 11 22 33",
            emergencyContactRelation = "Cónyuge"
        )

        repository.updateMedicalProfile(testProfile)

        // Read through repository
        val loadedProfile = repository.getMedicalProfile()
        assertEquals("Carlos García", loadedProfile.fullName)
        assertEquals("O+", loadedProfile.bloodType)
        assertEquals(3, loadedProfile.allergies.size)
        assertEquals("Penicilina", loadedProfile.allergies[0])
        assertEquals("Frutos secos (cacahuetes)", loadedProfile.allergies[1])
        assertEquals("Marisco", loadedProfile.allergies[2])
        assertEquals("Asma leve", loadedProfile.conditions)
        assertEquals("Salbutamol inhalador si es necesario", loadedProfile.medications)
        assertEquals("IATI Seguros Estrella", loadedProfile.insuranceCompany)
        assertEquals("POL-987654321", loadedProfile.insurancePolicyNumber)
        assertEquals("+34 93 485 77 00", loadedProfile.insuranceEmergencyPhone)
        assertEquals("María López", loadedProfile.emergencyContactName)
        assertEquals("+34 600 11 22 33", loadedProfile.emergencyContactPhone)
        assertEquals("Cónyuge", loadedProfile.emergencyContactRelation)

        // Check file was actually written to disk in storageDir
        val targetFile = File(storageDir, "medical_emergency_profile.json")
        assertTrue("medical_emergency_profile.json should exist on disk", targetFile.exists())
        val jsonContent = targetFile.readText()
        assertTrue(jsonContent.contains("Carlos García"))
        assertTrue(jsonContent.contains("POL-987654321"))

        // Create a new store instance pointing to same directory to verify persistence across instances
        val secondStore = LocalMedicalProfileStore(storageDir)
        val secondRepo = MedicalProfileRepositoryImpl(secondStore)
        val reloaded = secondRepo.getMedicalProfile()
        assertEquals(testProfile, reloaded)
    }

    @Test
    fun getMedicalProfile_corruptedFile_returnsDefaultProfileWithoutCrashing() = runTest {
        val targetFile = File(storageDir, "medical_emergency_profile.json")
        targetFile.writeText("{ invalid json : broken content [[[")

        val newStore = LocalMedicalProfileStore(storageDir)
        val newRepo = MedicalProfileRepositoryImpl(newStore)
        val fallback = newRepo.getMedicalProfile()

        assertEquals("", fallback.fullName)
        assertTrue(fallback.allergies.isEmpty())
    }
}
