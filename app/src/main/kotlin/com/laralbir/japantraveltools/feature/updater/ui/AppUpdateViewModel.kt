package com.laralbir.japantraveltools.feature.updater.ui

import com.laralbir.japantraveltools.feature.updater.domain.AppUpdateInfo
import com.laralbir.japantraveltools.feature.updater.domain.AutoUpdateRepository
import com.laralbir.japantraveltools.feature.updater.domain.UpdateCheckOutcome
import com.laralbir.japantraveltools.feature.updater.domain.UpdateNetworkPolicy
import com.laralbir.japantraveltools.feature.updater.domain.UpdatePreferences
import com.laralbir.japantraveltools.feature.updater.platform.ApkInstallExecutor
import com.laralbir.japantraveltools.feature.updater.platform.InstallExecutionResult
import com.laralbir.japantraveltools.feature.updater.platform.InstallStatusEvent
import com.laralbir.japantraveltools.feature.updater.platform.InstallStatusEventBus
import com.laralbir.japantraveltools.feature.updater.security.ApkSignatureVerifier
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val checkedVersion: String) : UpdateUiState
    data class UpdateAvailable(val info: AppUpdateInfo, val showDialog: Boolean = false) : UpdateUiState
    data class Downloading(val info: AppUpdateInfo, val downloadedBytes: Long, val totalBytes: Long) : UpdateUiState
    data class Verifying(val info: AppUpdateInfo) : UpdateUiState
    data class AwaitingInstallPermission(
        val info: AppUpdateInfo,
        val verifiedApkFile: File,
        val verifiedSha256Hex: String
    ) : UpdateUiState
    data class Installing(val info: AppUpdateInfo) : UpdateUiState
    data class Error(val message: String, val info: AppUpdateInfo? = null) : UpdateUiState
}

class AppUpdateViewModel(
    private val repository: AutoUpdateRepository,
    private val installExecutor: ApkInstallExecutor,
    private val installedVersionName: String,
    private val installedVersionCode: Long,
    private val sha256Calculator: (File) -> String = { file ->
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(64 * 1024).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead < 0) break
                if (bytesRead > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<UpdatePreferences> get() = repository.preferencesFlow

    private var downloadJob: Job? = null
    private var eventJob: Job? = null

    init {
        eventJob = scope.launch {
            InstallStatusEventBus.events.collect { event ->
                val current = _uiState.value
                val info = when (current) {
                    is UpdateUiState.Installing -> current.info
                    is UpdateUiState.AwaitingInstallPermission -> current.info
                    is UpdateUiState.Downloading -> current.info
                    is UpdateUiState.Verifying -> current.info
                    is UpdateUiState.UpdateAvailable -> current.info
                    is UpdateUiState.Error -> current.info
                    else -> null
                }
                when (event) {
                    is InstallStatusEvent.Succeeded -> {
                        _uiState.value = UpdateUiState.Idle
                    }
                    is InstallStatusEvent.Cancelled -> {
                        if (info != null) {
                            _uiState.value = UpdateUiState.UpdateAvailable(info, showDialog = true)
                        } else {
                            _uiState.value = UpdateUiState.Idle
                        }
                    }
                    is InstallStatusEvent.Failed -> {
                        _uiState.value = UpdateUiState.Error(event.message, info)
                    }
                }
            }
        }
    }

    fun onStartupCleanup() {
        repository.cleanupStaleUpdateFiles(installedVersionCode)
    }

    fun checkForUpdates(forceOfflineByApp: Boolean, manual: Boolean) {
        scope.launch {
            val priorAvailable = _uiState.value as? UpdateUiState.UpdateAvailable
            _uiState.value = UpdateUiState.Checking
            kotlinx.coroutines.yield()

            val outcome = repository.checkForUpdates(
                installedVersionName = installedVersionName,
                forceOfflineByApp = forceOfflineByApp,
                manual = manual
            )

            when (outcome) {
                is UpdateCheckOutcome.UpdateAvailable -> {
                    _uiState.value = UpdateUiState.UpdateAvailable(
                        info = outcome.updateInfo,
                        showDialog = manual
                    )
                }
                is UpdateCheckOutcome.UpToDate -> {
                    if (manual) {
                        _uiState.value = UpdateUiState.UpToDate(outcome.checkedVersion)
                    } else {
                        _uiState.value = priorAvailable ?: UpdateUiState.Idle
                    }
                }
                is UpdateCheckOutcome.SkippedOfflineOrPolicy -> {
                    _uiState.value = priorAvailable ?: UpdateUiState.Idle
                }
                is UpdateCheckOutcome.OfflineUnavailable -> {
                    if (manual) {
                        _uiState.value = UpdateUiState.Error(
                            message = "Sin conexión a internet o modo sin conexión activo.",
                            info = null
                        )
                    } else {
                        _uiState.value = priorAvailable ?: UpdateUiState.Idle
                    }
                }
                is UpdateCheckOutcome.Failed -> {
                    if (manual) {
                        _uiState.value = UpdateUiState.Error(message = outcome.message, info = null)
                    } else {
                        _uiState.value = priorAvailable ?: UpdateUiState.Idle
                    }
                }
            }
        }
    }

    fun openUpdateDialog() {
        val current = _uiState.value
        if (current is UpdateUiState.UpdateAvailable) {
            _uiState.value = current.copy(showDialog = true)
        }
    }

    fun remindMeLater() {
        _uiState.value = UpdateUiState.Idle
    }

    fun skipThisVersion() {
        val current = _uiState.value
        if (current is UpdateUiState.UpdateAvailable) {
            repository.dismissVersion(current.info.tagName)
        }
        _uiState.value = UpdateUiState.Idle
    }

    fun startDownloadAndInstall() {
        val current = _uiState.value
        val info = when (current) {
            is UpdateUiState.UpdateAvailable -> current.info
            is UpdateUiState.Error -> current.info
            is UpdateUiState.Downloading -> current.info
            is UpdateUiState.Verifying -> current.info
            is UpdateUiState.AwaitingInstallPermission -> current.info
            else -> null
        } ?: return

        downloadJob?.cancel()
        _uiState.value = UpdateUiState.Downloading(
            info = info,
            downloadedBytes = 0L,
            totalBytes = info.apkSizeBytes
        )

        downloadJob = scope.launch {
            val result = repository.downloadAndVerifyUpdateApk(
                updateInfo = info,
                installedVersionCode = installedVersionCode,
                onProgress = { downloaded, total ->
                    if (downloaded >= total && total > 0L) {
                        _uiState.value = UpdateUiState.Verifying(info)
                    } else {
                        _uiState.value = UpdateUiState.Downloading(info, downloaded, total)
                    }
                }
            )

            result.fold(
                onSuccess = { apkFile ->
                    val verifiedSha256 = try {
                        sha256Calculator(apkFile)
                    } catch (e: Exception) {
                        info.expectedSha256Hex ?: ""
                    }
                    if (!installExecutor.canInstallPackages()) {
                        _uiState.value = UpdateUiState.AwaitingInstallPermission(
                            info = info,
                            verifiedApkFile = apkFile,
                            verifiedSha256Hex = verifiedSha256
                        )
                    } else {
                        proceedWithInstall(info, apkFile)
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    _uiState.value = UpdateUiState.Error(
                        message = error.message ?: "Error al descargar la actualización",
                        info = info
                    )
                }
            )
        }
    }

    fun cancelDownload() {
        val current = _uiState.value
        val info = when (current) {
            is UpdateUiState.Downloading -> current.info
            is UpdateUiState.Verifying -> current.info
            else -> null
        }
        downloadJob?.cancel()
        downloadJob = null
        if (info != null) {
            _uiState.value = UpdateUiState.UpdateAvailable(info, showDialog = true)
        }
    }

    fun onHostResume() {
        val current = _uiState.value
        if (current is UpdateUiState.AwaitingInstallPermission) {
            if (installExecutor.canInstallPackages()) {
                val apkFile = current.verifiedApkFile
                if (!apkFile.exists()) {
                    repository.clearPendingVerifiedApk()
                    _uiState.value = UpdateUiState.Error(
                        message = "El archivo de actualización no se encuentra disponible.",
                        info = current.info
                    )
                    return
                }

                val currentSha = runCatching { sha256Calculator(apkFile) }.getOrNull()
                if (currentSha == null || !currentSha.equals(current.verifiedSha256Hex, ignoreCase = true)) {
                    runCatching { apkFile.delete() }
                    repository.clearPendingVerifiedApk()
                    _uiState.value = UpdateUiState.Error(
                        message = "Verificación de integridad fallida tras cambio de permisos.",
                        info = current.info
                    )
                    return
                }

                proceedWithInstall(current.info, apkFile)
            }
        }
    }

    private fun proceedWithInstall(info: AppUpdateInfo, apkFile: File) {
        _uiState.value = UpdateUiState.Installing(info)
        when (val result = installExecutor.installVerifiedApk(apkFile)) {
            is InstallExecutionResult.SessionCommitted -> {
                // Session committed successfully; awaits system broadcast event
            }
            is InstallExecutionResult.PermissionRequired -> {
                val sha = runCatching { sha256Calculator(apkFile) }.getOrDefault(info.expectedSha256Hex ?: "")
                _uiState.value = UpdateUiState.AwaitingInstallPermission(info, apkFile, sha)
            }
            is InstallExecutionResult.Failed -> {
                _uiState.value = UpdateUiState.Error(result.reason, info)
            }
        }
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        repository.updateAutoCheckEnabled(enabled)
    }

    fun setNetworkPolicy(policy: UpdateNetworkPolicy) {
        repository.updateNetworkPolicy(policy)
    }

    fun setIncludePreReleases(include: Boolean) {
        repository.updateIncludePreReleases(include)
    }

    fun dismissFeedback() {
        val current = _uiState.value
        if (current is UpdateUiState.UpToDate || current is UpdateUiState.Error) {
            _uiState.value = UpdateUiState.Idle
        }
    }

    fun clear() {
        downloadJob?.cancel()
        eventJob?.cancel()
    }
}
