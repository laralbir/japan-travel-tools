package com.laralbir.japantraveltools.feature.emergency

import com.laralbir.japantraveltools.feature.emergency.data.repository.EmergencyCatalogRepositoryImpl
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EmergencyCatalogRepositoryTest {

    private lateinit var repository: EmergencyCatalogRepositoryImpl

    @Before
    fun setUp() {
        repository = EmergencyCatalogRepositoryImpl()
    }

    @Test
    fun getEmergencyNumbers_containsPrimaryEmergencyNumbers() {
        val numbers = repository.getEmergencyNumbers()
        assertTrue(numbers.isNotEmpty())

        val phoneNumbers = numbers.map { it.phoneNumber }
        assertTrue("Must contain 110 (Police)", phoneNumbers.contains("110"))
        assertTrue("Must contain 119 (Fire & Ambulance)", phoneNumbers.contains("119"))
        assertTrue("Must contain 118 (Coast Guard)", phoneNumbers.contains("118"))
        assertTrue("Must contain 0570-000-911 (JNTO Hotline)", phoneNumbers.contains("0570-000-911"))
        assertTrue("Must contain 03-3501-0110 (Tokyo English Police Helpline)", phoneNumbers.contains("03-3501-0110"))
        assertTrue("Must contain #7119 (Medical Consultation)", phoneNumbers.contains("#7119"))

        val primaryNumbers = numbers.filter { it.isPrimaryEmergency }
        assertTrue(primaryNumbers.any { it.phoneNumber == "110" })
        assertTrue(primaryNumbers.any { it.phoneNumber == "119" })
        assertTrue(primaryNumbers.any { it.phoneNumber == "118" })
        assertTrue(primaryNumbers.any { it.phoneNumber == "0570-000-911" })
    }

    @Test
    fun getEmergencyNumbers_hasBilingualDescriptionsAndKanji() {
        val numbers = repository.getEmergencyNumbers()
        for (item in numbers) {
            assertTrue(item.id.isNotBlank())
            assertTrue(item.nameEn.isNotBlank())
            assertTrue(item.nameEs.isNotBlank())
            assertTrue(item.japaneseName.isNotBlank())
            assertTrue(item.phoneNumber.isNotBlank())
            assertTrue(item.descriptionEn.isNotBlank())
            assertTrue(item.descriptionEs.isNotBlank())
            assertTrue(item.iconName.isNotBlank())
        }
    }

    @Test
    fun getConsularMissions_withNullCountry_returnsAllMissions() {
        val allMissions = repository.getConsularMissions(null)
        assertTrue(allMissions.isNotEmpty())

        val countries = allMissions.map { it.countryCode }.toSet()
        assertTrue(countries.contains("ES"))
        assertTrue(countries.contains("MX"))
        assertTrue(countries.contains("CO"))
        assertTrue(countries.contains("AR"))
        assertTrue(countries.contains("CL"))
        assertTrue(countries.contains("PE"))
    }

    @Test
    fun getConsularMissions_filteredByCountryCode_returnsOnlyRequestedCountry() {
        val spainMissions = repository.getConsularMissions("ES")
        assertTrue(spainMissions.isNotEmpty())
        assertTrue(spainMissions.all { it.countryCode.equals("ES", ignoreCase = true) })

        val embassy = spainMissions.firstOrNull { it.id == "es-embassy-tokyo" }
        assertNotNull(embassy)
        assertEquals("España", embassy?.countryName)
        assertEquals("Tokio", embassy?.city)
        assertEquals("+81 90 1055 1410", embassy?.emergencyPhone24h)
        assertTrue(embassy?.standardPhone?.isNotBlank() == true)
        assertTrue(embassy?.addressJapanese?.isNotBlank() == true)
        assertTrue(embassy?.addressRomaji?.isNotBlank() == true)

        val mexicoMissions = repository.getConsularMissions("mx")
        assertTrue(mexicoMissions.isNotEmpty())
        assertTrue(mexicoMissions.all { it.countryCode.equals("MX", ignoreCase = true) })
    }

    @Test
    fun getConsularMissions_withUnknownCountry_returnsEmptyList() {
        val emptyList = repository.getConsularMissions("ZZ")
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun getEmergencyPhrases_withNullCategory_returnsAllPhrases() {
        val phrases = repository.getEmergencyPhrases(null)
        assertTrue(phrases.isNotEmpty())

        val categories = phrases.map { it.category }.toSet()
        assertTrue(categories.contains(EmergencyCategory.MEDICAL))
        assertTrue(categories.contains(EmergencyCategory.POLICE))
        assertTrue(categories.contains(EmergencyCategory.DISASTER))
        assertTrue(categories.contains(EmergencyCategory.ALLERGY))
    }

    @Test
    fun getEmergencyPhrases_filteredByCategory_returnsOnlyThatCategory() {
        for (cat in EmergencyCategory.values()) {
            val filtered = repository.getEmergencyPhrases(cat)
            assertTrue("Category $cat should not be empty", filtered.isNotEmpty())
            assertTrue(filtered.all { it.category == cat })

            for (phrase in filtered) {
                assertTrue(phrase.id.isNotBlank())
                assertTrue(phrase.spanishText.isNotBlank())
                assertTrue(phrase.englishText.isNotBlank())
                assertTrue(phrase.japaneseKanji.isNotBlank())
                assertTrue(phrase.romaji.isNotBlank())
            }
        }
    }
}
