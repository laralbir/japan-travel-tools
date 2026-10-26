package com.laralbir.japantraveltools.feature.documents

/**
 * Política de seguridad y control de acceso para la Bóveda de Documentos e Identidad.
 * Garantiza que la verificación biométrica sea estrictamente obligatoria por diseño si
 * el dispositivo dispone de capacidad biométrica o credencial configurada (huella/rostro/PIN).
 */
object VaultSecurityPolicy {
    /** Tiempo de seguridad máximo de sesión antes del auto-bloqueo: 2 minutos (120 segundos). */
    const val AUTO_LOCK_TIMEOUT_SECONDS: Long = 120L
    const val AUTO_LOCK_TIMEOUT_MILLIS: Long = AUTO_LOCK_TIMEOUT_SECONDS * 1000L

    /**
     * Determina si el bloqueo biométrico es obligatorio en el dispositivo.
     * Si el hardware cuenta con biometría o PIN de desbloqueo configurado, es 100% obligatorio.
     */
    fun isLockMandatory(biometricAvailable: Boolean): Boolean = biometricAvailable

    /**
     * Determina si la bóveda debe nacer desbloqueada al entrar a la pantalla.
     * Si la protección biométrica es obligatoria (hay biometría en el dispositivo),
     * la bóveda SIEMPRE nace bloqueada (`false`). Solo nace desbloqueada si el dispositivo
     * no tiene ningún método biométrico ni credencial configurada.
     */
    fun shouldStartUnlocked(biometricAvailable: Boolean): Boolean = !biometricAvailable

    /**
     * Sobrecarga de compatibilidad anterior.
     */
    fun shouldStartUnlocked(biometricLockEnabled: Boolean, biometricAvailable: Boolean): Boolean =
        shouldStartUnlocked(biometricAvailable)

    /**
     * Evalúa si una sesión activa ha expirado por inactividad o tiempo límite de seguridad.
     */
    fun isSessionExpired(unlockEpochMillis: Long, currentEpochMillis: Long): Boolean {
        if (unlockEpochMillis <= 0L) return true
        return (currentEpochMillis - unlockEpochMillis) >= AUTO_LOCK_TIMEOUT_MILLIS
    }
}
