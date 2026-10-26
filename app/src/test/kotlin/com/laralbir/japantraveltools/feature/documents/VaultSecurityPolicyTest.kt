package com.laralbir.japantraveltools.feature.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSecurityPolicyTest {

    @Test
    fun `vault starts locked by default when biometric lock is enabled and available on device`() {
        // Issue #5: Bóveda debe nacer bloqueada por defecto si la biometría está activa
        val shouldStartUnlocked = VaultSecurityPolicy.shouldStartUnlocked(
            biometricLockEnabled = true,
            biometricAvailable = true
        )
        assertFalse("La bóveda debe nacer bloqueada (false) por defecto", shouldStartUnlocked)
    }

    @Test
    fun `vault starts unlocked only when biometric lock is disabled or device lacks biometrics`() {
        val whenDisabled = VaultSecurityPolicy.shouldStartUnlocked(
            biometricLockEnabled = false,
            biometricAvailable = true
        )
        assertTrue(whenDisabled)

        val whenNotAvailable = VaultSecurityPolicy.shouldStartUnlocked(
            biometricLockEnabled = true,
            biometricAvailable = false
        )
        assertTrue(whenNotAvailable)
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
