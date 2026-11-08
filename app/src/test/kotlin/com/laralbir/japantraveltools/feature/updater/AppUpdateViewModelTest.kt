package com.laralbir.japantraveltools.feature.updater

import android.content.Intent
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
import com.laralbir.japantraveltools.feature.updater.ui.AppUpdateViewModel
import com.laralbir.japantraveltools.feature.updater.ui.UpdateUiState
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppUpdateViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testScope: CoroutineScope
    private lateinit var fakeRepository: FakeAutoUpdateRepository
    private lateinit var fakeExecutor: FakeApkInstallExecutor

    @Before
    fun setUp() {
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        fakeRepository = FakeAutoUpdateRepository()
        fakeExecutor = FakeApkInstallExecutor()
    }

    @After
    fun tearDown() {
        // Ensure test scope coroutines (including InstallStatusEventBus collectors) are cleaned up
    }

    @Test
    fun `initial state is Idle and onStartupCleanup delegates installedVersionCode to repository`() = runBlocking {
        val viewModel = createViewModel()
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)

        viewModel.onStartupCleanup()
        assertEquals(listOf(INSTALLED_VERSION_CODE), fakeRepository.cleanedUpVersionCodes)
        viewModel.clear()
    }

    @Test
    fun `automatic check sets UpdateAvailable with showDialog false and stays silent on offline or failure`() = runBlocking {
        val viewModel = createViewModel()
        val observedStates = mutableListOf<UpdateUiState>()
        val collectorJob = testScope.launch {
            viewModel.uiState.collect { observedStates.add(it) }
        }

        val info = sampleUpdateInfo()
        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpdateAvailable(info)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = false)

        assertEquals(
            UpdateUiState.UpdateAvailable(info = info, showDialog = false),
            viewModel.uiState.value
        )
        assertTrue(observedStates.contains(UpdateUiState.Checking))

        // Cooldown skip while banner is visible preserves the non-intrusive banner
        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.SkippedOfflineOrPolicy
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = false)
        assertEquals(
            UpdateUiState.UpdateAvailable(info = info, showDialog = false),
            viewModel.uiState.value
        )

        // Opening dialog from banner flips showDialog = true
        viewModel.openUpdateDialog()
        assertEquals(
            UpdateUiState.UpdateAvailable(info = info, showDialog = true),
            viewModel.uiState.value
        )

        // Reset and verify silent handling of offline/failure in automatic mode
        viewModel.remindMeLater()
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)

        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.OfflineUnavailable
        viewModel.checkForUpdates(forceOfflineByApp = true, manual = false)
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)

        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.Failed("SocketTimeoutException")
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = false)
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)

        collectorJob.cancel()
        viewModel.clear()
    }

    @Test
    fun `manual check opens dialog immediately on UpdateAvailable and surfaces UpToDate or Error`() = runBlocking {
        val viewModel = createViewModel()
        val info = sampleUpdateInfo()

        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpdateAvailable(info)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = true)
        assertEquals(
            UpdateUiState.UpdateAvailable(info = info, showDialog = true),
            viewModel.uiState.value
        )

        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpToDate(INSTALLED_VERSION_NAME)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = true)
        assertEquals(
            UpdateUiState.UpToDate(checkedVersion = INSTALLED_VERSION_NAME),
            viewModel.uiState.value
        )

        viewModel.dismissFeedback()
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)

        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.OfflineUnavailable
        viewModel.checkForUpdates(forceOfflineByApp = true, manual = true)
        assertTrue(viewModel.uiState.value is UpdateUiState.Error)

        viewModel.dismissFeedback()
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)

        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.Failed("HTTP 503 Service Unavailable")
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = true)
        assertEquals(
            UpdateUiState.Error(message = "HTTP 503 Service Unavailable", info = null),
            viewModel.uiState.value
        )

        viewModel.clear()
    }

    @Test
    fun `remindMeLater hides update for current session without persisting dismissedVersionTag`() = runBlocking {
        val viewModel = createViewModel()
        val info = sampleUpdateInfo()

        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpdateAvailable(info)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = true)
        assertTrue(viewModel.uiState.value is UpdateUiState.UpdateAvailable)

        viewModel.remindMeLater()
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)
        assertTrue(fakeRepository.dismissedTags.isEmpty())
        assertNull(viewModel.preferences.value.dismissedVersionTag)

        viewModel.clear()
    }

    @Test
    fun `skipThisVersion persists dismissed tag in repository and resets state to Idle`() = runBlocking {
        val viewModel = createViewModel()
        val info = sampleUpdateInfo(tagName = "v1.4.0")

        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpdateAvailable(info)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = false)
        assertEquals(
            UpdateUiState.UpdateAvailable(info = info, showDialog = false),
            viewModel.uiState.value
        )

        viewModel.skipThisVersion()
        assertEquals(UpdateUiState.Idle, viewModel.uiState.value)
        assertEquals(listOf("v1.4.0"), fakeRepository.dismissedTags)
        assertEquals("v1.4.0", viewModel.preferences.value.dismissedVersionTag)

        viewModel.clear()
    }

    @Test
    fun `startDownloadAndInstall transitions through Downloading Verifying and Installing when permission granted`() = runBlocking {
        val apkBytes = "signed-release-v1.4.0-binary".toByteArray(Charsets.UTF_8)
        val expectedSha256 = ApkSignatureVerifier.sha256BytesToHex(apkBytes)
        val apkFile = tempFolder.newFile("japan-travel-tools-v1.4.0.apk").apply {
            writeBytes(apkBytes)
        }
        val info = sampleUpdateInfo(apkSizeBytes = 20_000_000L, expectedSha256Hex = expectedSha256)

        val downloadGate = CompletableDeferred<Result<File>>()
        var capturedProgressCallback: ((Long, Long) -> Unit)? = null
        fakeRepository.downloadHandler = { _, _, onProgress ->
            capturedProgressCallback = onProgress
            downloadGate.await()
        }
        fakeExecutor.canInstall = true

        val viewModel = createViewModel()
        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpdateAvailable(info)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = true)

        viewModel.startDownloadAndInstall()
        assertEquals(
            UpdateUiState.Downloading(info = info, downloadedBytes = 0L, totalBytes = 20_000_000L),
            viewModel.uiState.value
        )

        capturedProgressCallback?.invoke(8_500_000L, 20_000_000L)
        assertEquals(
            UpdateUiState.Downloading(info = info, downloadedBytes = 8_500_000L, totalBytes = 20_000_000L),
            viewModel.uiState.value
        )

        capturedProgressCallback?.invoke(20_000_000L, 20_000_000L)
        assertEquals(UpdateUiState.Verifying(info = info), viewModel.uiState.value)

        downloadGate.complete(Result.success(apkFile))
        assertEquals(UpdateUiState.Installing(info = info), viewModel.uiState.value)
        assertEquals(listOf(apkFile), fakeExecutor.installedFiles)

        viewModel.clear()
    }

    @Test
    fun `cancelDownload cancels in-flight download job and restores UpdateAvailable with dialog open`() = runBlocking {
        val info = sampleUpdateInfo(apkSizeBytes = 18_000_000L)
        val neverCompletingGate = CompletableDeferred<Result<File>>()
        fakeRepository.downloadHandler = { _, _, onProgress ->
            onProgress(4_000_000L, 18_000_000L)
            try {
                neverCompletingGate.await()
            } catch (ce: CancellationException) {
                fakeRepository.downloadWasCancelled = true
                throw ce
            }
        }

        val viewModel = createViewModel()
        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpdateAvailable(info)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = false)

        viewModel.startDownloadAndInstall()
        assertEquals(
            UpdateUiState.Downloading(info = info, downloadedBytes = 4_000_000L, totalBytes = 18_000_000L),
            viewModel.uiState.value
        )

        viewModel.cancelDownload()
        assertTrue(fakeRepository.downloadWasCancelled)
        assertEquals(
            UpdateUiState.UpdateAvailable(info = info, showDialog = true),
            viewModel.uiState.value
        )

        viewModel.clear()
    }

    @Test
    fun `missing install permission transitions to AwaitingInstallPermission and onHostResume installs after SHA-256 revalidation`() = runBlocking {
        val apkBytes = "verified-production-apk-v1.4.0".toByteArray(Charsets.UTF_8)
        val expectedSha256 = ApkSignatureVerifier.sha256BytesToHex(apkBytes)
        val apkFile = tempFolder.newFile("verified-v1.4.0.apk").apply {
            writeBytes(apkBytes)
        }
        val info = sampleUpdateInfo(expectedSha256Hex = expectedSha256)

        fakeRepository.downloadHandler = { _, _, onProgress ->
            onProgress(info.apkSizeBytes, info.apkSizeBytes)
            Result.success(apkFile)
        }
        fakeExecutor.canInstall = false

        val viewModel = createViewModel()
        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpdateAvailable(info)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = true)

        viewModel.startDownloadAndInstall()
        assertEquals(
            UpdateUiState.AwaitingInstallPermission(
                info = info,
                verifiedApkFile = apkFile,
                verifiedSha256Hex = expectedSha256
            ),
            viewModel.uiState.value
        )
        assertTrue(fakeExecutor.installedFiles.isEmpty())

        // User returns from Settings without granting permission yet -> stays in AwaitingInstallPermission
        viewModel.onHostResume()
        assertTrue(viewModel.uiState.value is UpdateUiState.AwaitingInstallPermission)
        assertTrue(fakeExecutor.installedFiles.isEmpty())

        // User grants permission and returns -> SHA-256 re-verified and session committed automatically
        fakeExecutor.canInstall = true
        viewModel.onHostResume()
        assertEquals(UpdateUiState.Installing(info = info), viewModel.uiState.value)
        assertEquals(listOf(apkFile), fakeExecutor.installedFiles)

        viewModel.clear()
    }

    @Test
    fun `onHostResume fails closed deleting APK and blocking installer if cached file was tampered or deleted`() = runBlocking {
        val originalBytes = "authentic-signed-apk-content".toByteArray(Charsets.UTF_8)
        val expectedSha256 = ApkSignatureVerifier.sha256BytesToHex(originalBytes)
        val apkFile = tempFolder.newFile("tamper-check.apk").apply {
            writeBytes(originalBytes)
        }
        val info = sampleUpdateInfo(expectedSha256Hex = expectedSha256)

        fakeRepository.downloadHandler = { _, _, _ -> Result.success(apkFile) }
        fakeExecutor.canInstall = false

        val viewModel = createViewModel()
        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpdateAvailable(info)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = true)
        viewModel.startDownloadAndInstall()
        assertTrue(viewModel.uiState.value is UpdateUiState.AwaitingInstallPermission)

        // Simulate TOCTOU tampering of 1 byte before returning from system settings
        val tamperedBytes = originalBytes.copyOf().also { it[0] = (it[0].toInt() xor 0xFF).toByte() }
        apkFile.writeBytes(tamperedBytes)
        fakeExecutor.canInstall = true

        viewModel.onHostResume()

        val errorState = viewModel.uiState.value
        assertTrue("Expected Error state on tampered APK but got $errorState", errorState is UpdateUiState.Error)
        assertEquals(info, (errorState as UpdateUiState.Error).info)
        assertFalse("Tampered APK must be deleted immediately", apkFile.exists())
        assertEquals(1, fakeRepository.clearPendingApkCalls)
        assertTrue("PackageInstaller must never be invoked for tampered APK", fakeExecutor.installedFiles.isEmpty())

        viewModel.clear()
    }

    @Test
    fun `preferences mutators and InstallStatusEventBus events update state deterministically`() = runBlocking {
        val apkBytes = "valid-apk-for-session-events".toByteArray(Charsets.UTF_8)
        val expectedSha256 = ApkSignatureVerifier.sha256BytesToHex(apkBytes)
        val apkFile = tempFolder.newFile("session-events.apk").apply { writeBytes(apkBytes) }
        val info = sampleUpdateInfo(expectedSha256Hex = expectedSha256)

        fakeRepository.downloadHandler = { _, _, _ -> Result.success(apkFile) }
        fakeExecutor.canInstall = true

        val viewModel = createViewModel()

        viewModel.setAutoCheckEnabled(false)
        viewModel.setNetworkPolicy(UpdateNetworkPolicy.WIFI_AND_CELLULAR)
        viewModel.setIncludePreReleases(true)

        assertFalse(viewModel.preferences.value.autoCheckEnabled)
        assertEquals(UpdateNetworkPolicy.WIFI_AND_CELLULAR, viewModel.preferences.value.networkPolicy)
        assertTrue(viewModel.preferences.value.includePreReleases)

        fakeRepository.nextCheckOutcome = UpdateCheckOutcome.UpdateAvailable(info)
        viewModel.checkForUpdates(forceOfflineByApp = false, manual = true)
        viewModel.startDownloadAndInstall()
        assertEquals(UpdateUiState.Installing(info = info), viewModel.uiState.value)

        // System installer user cancellation returns to UpdateAvailable dialog
        InstallStatusEventBus.emit(InstallStatusEvent.Cancelled(sessionId = 42))
        assertEquals(
            UpdateUiState.UpdateAvailable(info = info, showDialog = true),
            viewModel.uiState.value
        )

        // Start install again and simulate PackageInstaller conflict failure
        viewModel.startDownloadAndInstall()
        assertEquals(UpdateUiState.Installing(info = info), viewModel.uiState.value)
        InstallStatusEventBus.emit(
            InstallStatusEvent.Failed(
                sessionId = 43,
                status = 5,
                message = "Conflicting package signature detected."
            )
        )
        assertEquals(
            UpdateUiState.Error(message = "Conflicting package signature detected.", info = info),
            viewModel.uiState.value
        )

        viewModel.clear()
    }

    private fun createViewModel(): AppUpdateViewModel =
        AppUpdateViewModel(
            repository = fakeRepository,
            installExecutor = fakeExecutor,
            installedVersionName = INSTALLED_VERSION_NAME,
            installedVersionCode = INSTALLED_VERSION_CODE,
            scope = testScope
        )

    private fun sampleUpdateInfo(
        tagName: String = "v1.4.0",
        apkSizeBytes: Long = 19_800_000L,
        expectedSha256Hex: String = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    ) = AppUpdateInfo(
        tagName = tagName,
        cleanVersionName = tagName.removePrefix("v"),
        releaseTitle = "Japan Travel Tools $tagName",
        changelogMarkdown = "## What's Changed\n- Offline-first OTA updates",
        publishedAtIso = "2026-11-08T08:00:00Z",
        apkAssetName = "japan-travel-tools-$tagName.apk",
        apkDownloadUrl = "https://github.com/laralbir/japan-travel-tools/releases/download/$tagName/japan-travel-tools-$tagName.apk",
        apkSizeBytes = apkSizeBytes,
        expectedSha256Hex = expectedSha256Hex,
        checksumAssetUrl = null,
        isPreRelease = false,
        releasePageUrl = "https://github.com/laralbir/japan-travel-tools/releases/tag/$tagName"
    )

    private class FakeAutoUpdateRepository : AutoUpdateRepository {
        private val _preferencesFlow = MutableStateFlow(UpdatePreferences())
        override val preferencesFlow: StateFlow<UpdatePreferences> = _preferencesFlow.asStateFlow()

        var nextCheckOutcome: UpdateCheckOutcome = UpdateCheckOutcome.UpToDate(INSTALLED_VERSION_NAME)
        var downloadHandler: (suspend (AppUpdateInfo, Long, (Long, Long) -> Unit) -> Result<File>)? = null
        var downloadWasCancelled: Boolean = false
        val dismissedTags = mutableListOf<String>()
        val cleanedUpVersionCodes = mutableListOf<Long>()
        var clearPendingApkCalls: Int = 0

        override fun updateAutoCheckEnabled(enabled: Boolean) {
            _preferencesFlow.value = _preferencesFlow.value.copy(autoCheckEnabled = enabled)
        }

        override fun updateNetworkPolicy(policy: UpdateNetworkPolicy) {
            _preferencesFlow.value = _preferencesFlow.value.copy(networkPolicy = policy)
        }

        override fun updateIncludePreReleases(include: Boolean) {
            _preferencesFlow.value = _preferencesFlow.value.copy(includePreReleases = include)
        }

        override fun dismissVersion(tagName: String) {
            dismissedTags.add(tagName)
            _preferencesFlow.value = _preferencesFlow.value.copy(dismissedVersionTag = tagName)
        }

        override fun clearPendingVerifiedApk() {
            clearPendingApkCalls += 1
            _preferencesFlow.value = _preferencesFlow.value.copy(pendingVerifiedApkPath = null)
        }

        override fun cleanupStaleUpdateFiles(currentVersionCode: Long) {
            cleanedUpVersionCodes.add(currentVersionCode)
        }

        override suspend fun checkForUpdates(
            installedVersionName: String,
            forceOfflineByApp: Boolean,
            manual: Boolean
        ): UpdateCheckOutcome = nextCheckOutcome

        override suspend fun downloadAndVerifyUpdateApk(
            updateInfo: AppUpdateInfo,
            installedVersionCode: Long,
            onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
        ): Result<File> {
            val handler = downloadHandler
                ?: return Result.failure(IllegalStateException("No downloadHandler configured"))
            return handler(updateInfo, installedVersionCode, onProgress)
        }
    }

    private class FakeApkInstallExecutor : ApkInstallExecutor {
        var canInstall: Boolean = true
        var nextResult: InstallExecutionResult = InstallExecutionResult.SessionCommitted
        val installedFiles = mutableListOf<File>()

        override fun canInstallPackages(): Boolean = canInstall

        override fun buildManageUnknownSourcesIntent(): Intent = Intent()

        override fun installVerifiedApk(apkFile: File): InstallExecutionResult {
            if (!canInstall) return InstallExecutionResult.PermissionRequired
            installedFiles.add(apkFile)
            return nextResult
        }
    }

    private companion object {
        const val INSTALLED_VERSION_NAME = "1.3.0"
        const val INSTALLED_VERSION_CODE = 36L
    }
}
