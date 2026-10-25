package com.laralbir.japantraveltools.core.preferences

import com.laralbir.japantraveltools.core.i18n.AppLanguagePreference
import com.laralbir.japantraveltools.feature.settings.OpenSourceLibrariesCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferencesRepositoryTest {

    @Test
    fun `default preferences match specification SPEC-00, SPEC-01 and SPEC-02`() = runTest {
        val store = InMemoryKeyValueStore()
        val repository = DefaultUserPreferencesRepository(store)

        val prefs = repository.preferencesFlow.first()
        assertEquals(AppThemeMode.SYSTEM, prefs.themeMode)
        assertEquals(AppLanguagePreference.SYSTEM, prefs.language)
        assertEquals("Europe/Madrid", prefs.originZoneId)
        assertTrue(prefs.use24HourFormat)
        assertEquals("EUR", prefs.baseCurrency)
        assertEquals("JPY", prefs.targetCurrency)
        assertNull(prefs.customExchangeRate)
        assertFalse(prefs.biometricLockEnabled)
        assertFalse(prefs.forceOfflineMode)
        assertTrue(prefs.syncOnlyOnWifi)
        assertNull(prefs.googleAccountEmail)
    }

    @Test
    fun `updating theme, language, origin zone, currency and sync settings persists across new repository instances`() = runTest {
        val store = InMemoryKeyValueStore()
        val repository = DefaultUserPreferencesRepository(store)

        repository.setThemeMode(AppThemeMode.DARK)
        repository.setLanguage(AppLanguagePreference.SPANISH)
        repository.setOriginZoneId("America/Mexico_City")
        repository.setCurrencyPair("USD", "JPY")
        repository.setCustomExchangeRate(164.25)
        repository.setBiometricLockEnabled(true)
        repository.setForceOfflineMode(true)
        repository.setGoogleAccountSync(email = "traveler@gmail.com", lastSyncedMillis = 1730000000000L)

        // Create a second instance backed by the same persistent store to simulate app restart
        val restartedRepo = DefaultUserPreferencesRepository(store)
        val restored = restartedRepo.preferencesFlow.first()

        assertEquals(AppThemeMode.DARK, restored.themeMode)
        assertEquals(AppLanguagePreference.SPANISH, restored.language)
        assertEquals("America/Mexico_City", restored.originZoneId)
        assertEquals("USD", restored.baseCurrency)
        assertEquals("JPY", restored.targetCurrency)
        assertEquals(164.25, restored.customExchangeRate ?: 0.0, 0.001)
        assertTrue(restored.biometricLockEnabled)
        assertTrue(restored.forceOfflineMode)
        assertEquals("traveler@gmail.com", restored.googleAccountEmail)
        assertEquals(1730000000000L, restored.lastCloudSyncEpochMillis)
    }

    @Test
    fun `app version string in UI never contains beta label per SPEC-00 RF-00_5`() {
        assertFalse(OpenSourceLibrariesCatalog.APP_VERSION_NAME.contains("beta", ignoreCase = true))
    }
}

