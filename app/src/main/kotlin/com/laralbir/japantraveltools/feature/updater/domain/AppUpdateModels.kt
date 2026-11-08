package com.laralbir.japantraveltools.feature.updater.domain

import java.io.File
import kotlinx.coroutines.flow.StateFlow

enum class UpdateNetworkPolicy {
    WIFI_ONLY,
    WIFI_AND_CELLULAR
}

data class UpdatePreferences(
    val autoCheckEnabled: Boolean = true,
    val networkPolicy: UpdateNetworkPolicy = UpdateNetworkPolicy.WIFI_ONLY,
    val includePreReleases: Boolean = false,
    val lastUpdateCheckEpochMillis: Long = 0L,
    val dismissedVersionTag: String? = null,
    val lastEtag: String? = null,
    val cachedReleaseJson: String? = null,
    val pendingVerifiedApkPath: String? = null,
    val lastKnownInstalledVersionCode: Long = 0L
)

data class AppUpdateInfo(
    val tagName: String,
    val cleanVersionName: String,
    val releaseTitle: String,
    val changelogMarkdown: String,
    val publishedAtIso: String,
    val apkAssetName: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val expectedSha256Hex: String?,
    val checksumAssetUrl: String?,
    val isPreRelease: Boolean,
    val releasePageUrl: String
)

sealed interface UpdateCheckOutcome {
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateCheckOutcome
    data class UpToDate(val checkedVersion: String) : UpdateCheckOutcome
    data object SkippedOfflineOrPolicy : UpdateCheckOutcome
    data object OfflineUnavailable : UpdateCheckOutcome
    data class Failed(val message: String) : UpdateCheckOutcome
}

interface AutoUpdateRepository {
    val preferencesFlow: StateFlow<UpdatePreferences>
    fun updateAutoCheckEnabled(enabled: Boolean)
    fun updateNetworkPolicy(policy: UpdateNetworkPolicy)
    fun updateIncludePreReleases(include: Boolean)
    fun dismissVersion(tagName: String)
    fun clearPendingVerifiedApk()
    fun cleanupStaleUpdateFiles(currentVersionCode: Long)
    suspend fun checkForUpdates(
        installedVersionName: String,
        forceOfflineByApp: Boolean,
        manual: Boolean
    ): UpdateCheckOutcome
    suspend fun downloadAndVerifyUpdateApk(
        updateInfo: AppUpdateInfo,
        installedVersionCode: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File>
}
