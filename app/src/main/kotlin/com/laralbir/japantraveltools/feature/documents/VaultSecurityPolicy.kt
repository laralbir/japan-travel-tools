package com.laralbir.japantraveltools.feature.documents

/**
 * Política de seguridad y control de acceso para la Bóveda de Documentos e Identidad.
 * Garantiza que la bóveda permanezca protegida por defecto y gestione el auto-bloqueo temporal.
 */
object VaultSecurityPolicy {
    /** Tiempo de seguridad máximo de sesión antes del auto-bloqueo: 2 minutos (120 segundos). */
    const val AUTO_LOCK_TIMEOUT_SECONDS: Long = 120L
    const val AUTO_LOCK_TIMEOUT_MILLIS: Long = AUTO_LOCK_TIMEOUT_SECONDS * 1000L

    /**
     * Determina si la bóveda debe nacer desbloqueada al entrar a la pantalla.
     * Si la protección biométrica está activa y el hardware dispone de biometría/PIN,
     * la bóveda SIEMPRE nace bloqueada (`false`), obligando a la tarjeta de bloqueo inicial.
     */
    fun shouldStartUnlocked(biometricLockEnabled: Boolean, biometricAvailable: Boolean): Boolean {
        val isLockActive = biometricLockEnabled && biometricAvailable
        return !isLockActive
    }

    /**
     * Evalúa si una sesión activa ha expirado por inactividad o tiempo límite de seguridad.
     */
    fun isSessionExpired(unlockEpochMillis: Long, currentEpochMillis: Long): Boolean {
        if (unlockEpochMillis <= 0L) return true
        return (currentEpochMillis - unlockEpochMillis) >= AUTO_LOCK_TIMEOUT_MILLIS
    }
}
