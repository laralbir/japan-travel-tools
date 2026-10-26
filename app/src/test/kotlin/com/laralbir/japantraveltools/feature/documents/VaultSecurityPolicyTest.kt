package com.laralbir.japantraveltools.feature.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSecurityPolicyTest {

    @Test
    fun `biometric protection is strictly mandatory when biometric or pin is available on device`() {
        assertTrue(VaultSecurityPolicy.isLockMandatory(biometricAvailable = true))
        assertFalse(VaultSecurityPolicy.isLockMandatory(biometricAvailable = false))
    }

    @Test
    fun `vault starts locked by default when biometric is available on device without toggle`() {
        val shouldStartUnlocked = VaultSecurityPolicy.shouldStartUnlocked(biometricAvailable = true)
        assertFalse("La bóveda debe nacer bloqueada (false) por defecto si hay biometría disponible", shouldStartUnlocked)
    }

    @Test
    fun `vault starts unlocked only when device lacks biometrics or lock credentials`() {
        val shouldStartUnlocked = VaultSecurityPolicy.shouldStartUnlocked(biometricAvailable = false)
        assertTrue(shouldStartUnlocked)
    }

    @Test
    fun `auto lock timeout is configured to prudent security window of 120 seconds`() {
        assertEquals(120L, VaultSecurityPolicy.AUTO_LOCK_TIMEOUT_SECONDS)
        assertEquals(120_000L, VaultSecurityPolicy.AUTO_LOCK_TIMEOUT_MILLIS)
    }

    @Test
    fun `session expires after 120 seconds of inactivity or active display`() {
        val unlockTime = 1_000_000L

        // Tras 60 segundos (menos del límite), no expira
        assertFalse(VaultSecurityPolicy.isSessionExpired(unlockTime, unlockTime + 60_000L))

        // Justo al límite de 120 segundos, expira
        assertTrue(VaultSecurityPolicy.isSessionExpired(unlockTime, unlockTime + 120_000L))

        // Pasados 121 segundos, expira
        assertTrue(VaultSecurityPolicy.isSessionExpired(unlockTime, unlockTime + 121_000L))

        // Si nunca fue desbloqueada (unlockTime = 0), se considera expirada
        assertTrue(VaultSecurityPolicy.isSessionExpired(0L, unlockTime))
    }
}
