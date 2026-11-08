package com.laralbir.japantraveltools.feature.updater.data

import android.content.Context
import com.laralbir.japantraveltools.core.network.ActiveConnectionKind
import com.laralbir.japantraveltools.core.network.ConnectivityStatusHelper
import com.laralbir.japantraveltools.core.network.ConnectivityStatusSnapshot
import com.laralbir.japantraveltools.core.preferences.SharedPrefsKeyValueStore
import com.laralbir.japantraveltools.feature.updater.domain.AppUpdateInfo
import com.laralbir.japantraveltools.feature.updater.domain.AutoUpdateRepository
import com.laralbir.japantraveltools.feature.updater.domain.SemanticVersionComparator
import com.laralbir.japantraveltools.feature.updater.domain.UpdateCheckOutcome
import com.laralbir.japantraveltools.feature.updater.domain.UpdateNetworkPolicy
import com.laralbir.japantraveltools.feature.updater.domain.UpdatePreferences
import com.laralbir.japantraveltools.feature.updater.security.AndroidPackageSignatureInspector
import com.laralbir.japantraveltools.feature.updater.security.ApkSignatureVerifier
import com.laralbir.japantraveltools.feature.updater.security.ApkVerificationResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class GitHubAutoUpdateRepository(
    private val preferencesDataStore: UpdatePreferencesDataStore,
    private val apiClient: GitHubReleasesApiClient,
    private val signatureVerifier: ApkSignatureVerifier,
    private val updatesDirProvider: () -> File,
    private val connectivityInspector: (forceOfflineByApp: Boolean) -> ConnectivityStatusSnapshot,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) : AutoUpdateRepository {

    override val preferencesFlow: StateFlow<UpdatePreferences> =
        preferencesDataStore.preferencesFlow

    override fun updateAutoCheckEnabled(enabled: Boolean) {
        preferencesDataStore.setAutoCheckEnabled(enabled)
    }

    override fun updateNetworkPolicy(policy: UpdateNetworkPolicy) {
        preferencesDataStore.setNetworkPolicy(policy)
    }

    override fun updateIncludePreReleases(include: Boolean) {
        preferencesDataStore.setIncludePreReleases(include)
    }

    override fun dismissVersion(tagName: String) {
        preferencesDataStore.setDismissedVersionTag(tagName)
    }

    override fun clearPendingVerifiedApk() {
        preferencesDataStore.setPendingVerifiedApkPath(null)
    }

    override fun cleanupStaleUpdateFiles(currentVersionCode: Long) {
        val updatesDir = updatesDirProvider()
        if (updatesDir.exists() && updatesDir.isDirectory) {
            val lastInstalledVersion = preferencesDataStore.current.lastKnownInstalledVersionCode
            if (currentVersionCode > lastInstalledVersion) {
                updatesDir.listFiles()?.forEach { file ->
                    runCatching { file.delete() }
                }
                clearPendingVerifiedApk()
                preferencesDataStore.setLastKnownInstalledVersionCode(currentVersionCode)
            } else {
                // Delete any leftover .part files from interrupted downloads
                updatesDir.listFiles { _, name -> name.endsWith(".part") }?.forEach { file ->
                    runCatching { file.delete() }
                }
            }
        }
    }

    override suspend fun checkForUpdates(
        installedVersionName: String,
        forceOfflineByApp: Boolean,
        manual: Boolean
    ): UpdateCheckOutcome = withContext(Dispatchers.IO) {
        val prefs = preferencesDataStore.current

        if (manual) {
            if (forceOfflineByApp) {
                return@withContext UpdateCheckOutcome.OfflineUnavailable
            }
            val snapshot = connectivityInspector(false)
            if (!snapshot.isOnline) {
                return@withContext UpdateCheckOutcome.OfflineUnavailable
            }
        } else {
            if (!prefs.autoCheckEnabled || forceOfflineByApp) {
                return@withContext UpdateCheckOutcome.SkippedOfflineOrPolicy
            }
            val snapshot = connectivityInspector(false)
            if (!snapshot.isOnline || snapshot.isRoaming) {
                return@withContext UpdateCheckOutcome.SkippedOfflineOrPolicy
            }
            if (prefs.networkPolicy == UpdateNetworkPolicy.WIFI_ONLY && snapshot.kind != ActiveConnectionKind.WIFI) {
                return@withContext UpdateCheckOutcome.SkippedOfflineOrPolicy
            }

            val now = clockMillis()
            val timeSinceLastCheck = now - prefs.lastUpdateCheckEpochMillis
            if (timeSinceLastCheck < COOLDOWN_MILLIS) {
                return@withContext UpdateCheckOutcome.SkippedOfflineOrPolicy
            }
        }

        try {
            val etagToSend = if (!manual) prefs.lastEtag else null
            val response = apiClient.fetchReleasesJson(
                includePreReleases = prefs.includePreReleases,
                etag = etagToSend
            )

            when (response) {
                is GitHubApiReleaseResponse.NotModified -> {
                    preferencesDataStore.recordCheckTimestampOnly(clockMillis())
                    val cachedJson = prefs.cachedReleaseJson
                    if (cachedJson != null) {
                        evaluateReleaseJson(cachedJson, installedVersionName, prefs, manual)
                    } else {
                        UpdateCheckOutcome.UpToDate(installedVersionName)
                    }
                }
                is GitHubApiReleaseResponse.Success -> {
                    preferencesDataStore.recordSuccessfulCheck(
                        epochMillis = clockMillis(),
                        etag = response.etag,
                        cachedJson = response.jsonBody
                    )
                    evaluateReleaseJson(response.jsonBody, installedVersionName, prefs, manual)
                }
            }
        } catch (t: Throwable) {
            UpdateCheckOutcome.Failed(t.message ?: t.toString())
        }
    }

    private fun evaluateReleaseJson(
        json: String,
        installedVersionName: String,
        prefs: UpdatePreferences,
        manual: Boolean
    ): UpdateCheckOutcome {
        val trimmed = json.trim()
        val updateInfo = if (trimmed.startsWith("[")) {
            val list = GitHubReleaseJsonParser.parseReleaseList(trimmed)
            val bestDto = GitHubReleaseJsonParser.selectBestRelease(list, prefs.includePreReleases)
            bestDto?.let { GitHubReleaseJsonParser.toAppUpdateInfo(it, prefs.includePreReleases) }
        } else {
            val dto = GitHubReleaseJsonParser.parseSingleRelease(trimmed)
            GitHubReleaseJsonParser.toAppUpdateInfo(dto, prefs.includePreReleases)
        }

        if (updateInfo == null) {
            return UpdateCheckOutcome.UpToDate(installedVersionName)
        }

        val isNewer = SemanticVersionComparator.isRemoteNewer(
            installedVersion = installedVersionName,
            remoteTag = updateInfo.tagName
        )

        if (!isNewer) {
            return UpdateCheckOutcome.UpToDate(installedVersionName)
        }

        if (!manual) {
            val dismissed = prefs.dismissedVersionTag
            if (dismissed != null && SemanticVersionComparator.compare(updateInfo.tagName, dismissed) <= 0) {
                return UpdateCheckOutcome.SkippedOfflineOrPolicy
            }
        }

        return UpdateCheckOutcome.UpdateAvailable(updateInfo)
    }

    override suspend fun downloadAndVerifyUpdateApk(
        updateInfo: AppUpdateInfo,
        installedVersionCode: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(updateInfo.apkDownloadUrl)) {
            return@withContext Result.failure(
                SecurityException("Untrusted or disallowed download URL: ${updateInfo.apkDownloadUrl}")
            )
        }

        val expectedSha = updateInfo.expectedSha256Hex
            ?: if (updateInfo.checksumAssetUrl != null) {
                if (!HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(updateInfo.checksumAssetUrl)) {
                    return@withContext Result.failure(
                        SecurityException("Untrusted checksum URL: ${updateInfo.checksumAssetUrl}")
                    )
                }
                try {
                    val checksumText = apiClient.fetchTextFile(updateInfo.checksumAssetUrl)
                    GitHubReleaseJsonParser.parseSha256ChecksumFile(checksumText, updateInfo.apkAssetName)
                } catch (t: Throwable) {
                    return@withContext Result.failure(t)
                }
            } else {
                null
            }

        if (expectedSha == null) {
            return@withContext Result.failure(
                SecurityException("No trusted expected SHA-256 checksum available for ${updateInfo.apkAssetName}")
            )
        }

        val updatesDir = updatesDirProvider()
        updatesDir.mkdirs()
        val partFile = File(updatesDir, "jtt-${updateInfo.cleanVersionName}.apk.part")
        val finalApkFile = File(updatesDir, "jtt-${updateInfo.cleanVersionName}.apk")

        try {
            apiClient.downloadBinaryFile(
                httpsUrl = updateInfo.apkDownloadUrl,
                targetPartFile = partFile,
                onProgress = onProgress
            )

            val verificationResult = signatureVerifier.verifyApk(
                apkFile = partFile,
                expectedSha256Hex = expectedSha,
                installedVersionCode = installedVersionCode
            )

            when (verificationResult) {
                is ApkVerificationResult.Verified -> {
                    if (finalApkFile.exists()) {
                        finalApkFile.delete()
                    }
                    if (!partFile.renameTo(finalApkFile)) {
                        partFile.copyTo(finalApkFile, overwrite = true)
                        partFile.delete()
                    }
                    preferencesDataStore.setPendingVerifiedApkPath(finalApkFile.absolutePath)
                    Result.success(finalApkFile)
                }
                else -> {
                    if (partFile.exists()) {
                        partFile.delete()
                    }
                    preferencesDataStore.setPendingVerifiedApkPath(null)
                    val failureMessage = when (verificationResult) {
                        is ApkVerificationResult.SignatureMismatch ->
                            "La firma criptográfica del paquete no coincide con la versión instalada en este dispositivo (tienes instalada una versión de desarrollo 'Debug'). Desinstala primero la versión Debug para poder instalar la versión oficial de GitHub."
                        is ApkVerificationResult.VersionDowngradeBlocked ->
                            "La versión descargada es igual o anterior a la versión ya instalada en este dispositivo."
                        is ApkVerificationResult.ChecksumMismatch ->
                            "La suma de verificación SHA-256 no coincide con la publicada en GitHub."
                        is ApkVerificationResult.PackageNameMismatch ->
                            "El paquete descargado no corresponde a Japan Travel Tools."
                        is ApkVerificationResult.InvalidArchive ->
                            "El archivo APK descargado no es válido: ${verificationResult.reason}"
                        is ApkVerificationResult.Verified ->
                            "Verificado con éxito."
                    }
                    Result.failure(SecurityException(failureMessage))
                }
            }
        } catch (t: Throwable) {
            if (partFile.exists()) {
                runCatching { partFile.delete() }
            }
            preferencesDataStore.setPendingVerifiedApkPath(null)
            Result.failure(t)
        }
    }

    companion object {
        const val COOLDOWN_MILLIS: Long = 12 * 60 * 60 * 1000L // 12 hours

        fun createDefault(context: Context): GitHubAutoUpdateRepository {
            val appContext = context.applicationContext
            val store = SharedPrefsKeyValueStore(appContext)
            val prefsDataStore = UpdatePreferencesDataStore(store)
            val apiClient = HttpGitHubReleasesApiClient()
            val signatureInspector = AndroidPackageSignatureInspector(appContext)
            val verifier = ApkSignatureVerifier(signatureInspector)
            val updatesDir = { File(appContext.cacheDir, "updates") }

            return GitHubAutoUpdateRepository(
                preferencesDataStore = prefsDataStore,
                apiClient = apiClient,
                signatureVerifier = verifier,
                updatesDirProvider = updatesDir,
                connectivityInspector = { forceOffline ->
                    ConnectivityStatusHelper.inspectCurrentConnection(appContext, forceOffline)
                }
            )
        }
    }
}
