package com.laralbir.japantraveltools.feature.updater

import com.laralbir.japantraveltools.core.network.ActiveConnectionKind
import com.laralbir.japantraveltools.core.network.ConnectivityStatusSnapshot
import com.laralbir.japantraveltools.core.preferences.InMemoryKeyValueStore
import com.laralbir.japantraveltools.feature.updater.data.GitHubApiReleaseResponse
import com.laralbir.japantraveltools.feature.updater.data.GitHubAutoUpdateRepository
import com.laralbir.japantraveltools.feature.updater.data.GitHubReleasesApiClient
import com.laralbir.japantraveltools.feature.updater.data.HttpGitHubReleasesApiClient
import com.laralbir.japantraveltools.feature.updater.data.UpdatePreferencesDataStore
import com.laralbir.japantraveltools.feature.updater.domain.AppUpdateInfo
import com.laralbir.japantraveltools.feature.updater.domain.UpdateCheckOutcome
import com.laralbir.japantraveltools.feature.updater.domain.UpdateNetworkPolicy
import com.laralbir.japantraveltools.feature.updater.security.ApkSignatureVerifier
import com.laralbir.japantraveltools.feature.updater.security.ArchiveSignatureMetadata
import com.laralbir.japantraveltools.feature.updater.security.PackageSignatureInspector
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubAutoUpdateRepositoryTest {

    private val officialCertFingerprint =
        "4f8ab12cd34ef567890123456789abcdef0123456789abcdef0123456789abc1"
    private val attackerCertFingerprint =
        "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"

    @Test
    fun `automatic check performs zero network calls when disabled offline roaming cellular on wifi-only or within 12h cooldown`() = runBlocking {
        val tempDir = createTempUpdatesDir()
        try {
            val store = InMemoryKeyValueStore()
            val prefsStore = UpdatePreferencesDataStore(store)
            val fakeApi = FakeGitHubReleasesApiClient(
                nextReleaseResponse = GitHubApiReleaseResponse.Success(
                    jsonBody = sampleReleaseJson(tagName = "v1.4.0"),
                    etag = "W/\"v140\""
                )
            )
            var currentNow = 1_700_000_000_000L
            var activeSnapshot = wifiOnlineSnapshot()

            val repo = GitHubAutoUpdateRepository(
                preferencesDataStore = prefsStore,
                apiClient = fakeApi,
                signatureVerifier = createVerifier(),
                updatesDirProvider = { tempDir },
                connectivityInspector = { forceOffline ->
                    if (forceOffline) {
                        activeSnapshot.copy(
                            kind = ActiveConnectionKind.OFFLINE,
                            isOnline = false,
                            forceOfflineByApp = true
                        )
                    } else {
                        activeSnapshot
                    }
                },
                clockMillis = { currentNow }
            )

            // 1. autoCheckEnabled == false
            repo.updateAutoCheckEnabled(false)
            assertEquals(
                UpdateCheckOutcome.SkippedOfflineOrPolicy,
                repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            )
            assertEquals(0, fakeApi.fetchReleasesCalls.size)
            repo.updateAutoCheckEnabled(true)

            // 2. forceOfflineMode == true
            assertEquals(
                UpdateCheckOutcome.SkippedOfflineOrPolicy,
                repo.checkForUpdates("1.3.0", forceOfflineByApp = true, manual = false)
            )
            assertEquals(0, fakeApi.fetchReleasesCalls.size)

            // 3. Device has no internet connection
            activeSnapshot = ConnectivityStatusSnapshot(
                kind = ActiveConnectionKind.OFFLINE,
                isOnline = false,
                isMetered = false,
                isRoaming = false,
                forceOfflineByApp = false
            )
            assertEquals(
                UpdateCheckOutcome.SkippedOfflineOrPolicy,
                repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            )
            assertEquals(0, fakeApi.fetchReleasesCalls.size)

            // 4. Cellular roaming active even under WIFI_AND_CELLULAR policy
            repo.updateNetworkPolicy(UpdateNetworkPolicy.WIFI_AND_CELLULAR)
            activeSnapshot = ConnectivityStatusSnapshot(
                kind = ActiveConnectionKind.CELLULAR_ROAMING,
                isOnline = true,
                isMetered = true,
                isRoaming = true,
                forceOfflineByApp = false
            )
            assertEquals(
                UpdateCheckOutcome.SkippedOfflineOrPolicy,
                repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            )
            assertEquals(0, fakeApi.fetchReleasesCalls.size)

            // 5. Cellular eSIM blocked when policy is WIFI_ONLY
            repo.updateNetworkPolicy(UpdateNetworkPolicy.WIFI_ONLY)
            activeSnapshot = ConnectivityStatusSnapshot(
                kind = ActiveConnectionKind.CELLULAR_ESIM,
                isOnline = true,
                isMetered = true,
                isRoaming = false,
                forceOfflineByApp = false
            )
            assertEquals(
                UpdateCheckOutcome.SkippedOfflineOrPolicy,
                repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            )
            assertEquals(0, fakeApi.fetchReleasesCalls.size)

            // 6. Metered Pocket Wi-Fi blocked when policy is WIFI_ONLY
            activeSnapshot = ConnectivityStatusSnapshot(
                kind = ActiveConnectionKind.WIFI_METERED,
                isOnline = true,
                isMetered = true,
                isRoaming = false,
                forceOfflineByApp = false
            )
            assertEquals(
                UpdateCheckOutcome.SkippedOfflineOrPolicy,
                repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            )
            assertEquals(0, fakeApi.fetchReleasesCalls.size)

            // 7. Eligible Wi-Fi succeeds once, then < 12 hours cooldown blocks subsequent auto check
            activeSnapshot = wifiOnlineSnapshot()
            val firstOutcome = repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            assertTrue(firstOutcome is UpdateCheckOutcome.UpdateAvailable)
            assertEquals(1, fakeApi.fetchReleasesCalls.size)

            // Advance 2 hours (< 12 hours)
            currentNow += 2L * 60L * 60L * 1000L
            val withinCooldownOutcome = repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            assertEquals(UpdateCheckOutcome.SkippedOfflineOrPolicy, withinCooldownOutcome)
            assertEquals(1, fakeApi.fetchReleasesCalls.size)

            // Advance to exactly 12 hours after first check -> eligible again
            currentNow += 10L * 60L * 60L * 1000L
            val afterCooldownOutcome = repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            assertTrue(afterCooldownOutcome is UpdateCheckOutcome.UpdateAvailable)
            assertEquals(2, fakeApi.fetchReleasesCalls.size)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `etag 304 NotModified updates lastUpdateCheckEpochMillis and evaluates cached release json`() = runBlocking {
        val tempDir = createTempUpdatesDir()
        try {
            val prefsStore = UpdatePreferencesDataStore(InMemoryKeyValueStore())
            val fakeApi = FakeGitHubReleasesApiClient(
                nextReleaseResponse = GitHubApiReleaseResponse.Success(
                    jsonBody = sampleReleaseJson(tagName = "v1.4.0"),
                    etag = "W/\"etag-140\""
                )
            )
            var currentNow = 1_700_000_000_000L

            val repo = GitHubAutoUpdateRepository(
                preferencesDataStore = prefsStore,
                apiClient = fakeApi,
                signatureVerifier = createVerifier(),
                updatesDirProvider = { tempDir },
                connectivityInspector = { wifiOnlineSnapshot() },
                clockMillis = { currentNow }
            )

            val initial = repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            assertTrue(initial is UpdateCheckOutcome.UpdateAvailable)
            assertEquals("W/\"etag-140\"", repo.preferencesFlow.value.lastEtag)
            assertEquals(currentNow, repo.preferencesFlow.value.lastUpdateCheckEpochMillis)
            assertNull(fakeApi.fetchReleasesCalls[0].etag)

            // 13 hours later GitHub returns 304 Not Modified
            val secondCheckTime = currentNow + 13L * 60L * 60L * 1000L
            currentNow = secondCheckTime
            fakeApi.nextReleaseResponse = GitHubApiReleaseResponse.NotModified

            val second = repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            assertEquals("W/\"etag-140\"", fakeApi.fetchReleasesCalls[1].etag)
            assertEquals(secondCheckTime, repo.preferencesFlow.value.lastUpdateCheckEpochMillis)
            assertTrue(second is UpdateCheckOutcome.UpdateAvailable)
            assertEquals("v1.4.0", (second as UpdateCheckOutcome.UpdateAvailable).updateInfo.tagName)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `dismissedVersionTag suppresses automatic notifications reactivates for newer version and is bypassed on manual check`() = runBlocking {
        val tempDir = createTempUpdatesDir()
        try {
            val prefsStore = UpdatePreferencesDataStore(InMemoryKeyValueStore())
            val fakeApi = FakeGitHubReleasesApiClient(
                nextReleaseResponse = GitHubApiReleaseResponse.Success(
                    jsonBody = sampleReleaseJson(tagName = "v1.4.0"),
                    etag = "W/\"etag-140\""
                )
            )
            var currentNow = 1_700_000_000_000L

            val repo = GitHubAutoUpdateRepository(
                preferencesDataStore = prefsStore,
                apiClient = fakeApi,
                signatureVerifier = createVerifier(),
                updatesDirProvider = { tempDir },
                connectivityInspector = { forceOffline ->
                    if (forceOffline) {
                        ConnectivityStatusSnapshot(
                            kind = ActiveConnectionKind.OFFLINE,
                            isOnline = false,
                            isMetered = false,
                            isRoaming = false,
                            forceOfflineByApp = true
                        )
                    } else {
                        wifiOnlineSnapshot()
                    }
                },
                clockMillis = { currentNow }
            )

            // Dismiss v1.4.0
            repo.dismissVersion("v1.4.0")
            assertEquals("v1.4.0", repo.preferencesFlow.value.dismissedVersionTag)

            // Automatic check finds v1.4.0 -> suppressed!
            val suppressedOutcome = repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            assertEquals(UpdateCheckOutcome.SkippedOfflineOrPolicy, suppressedOutcome)

            // Manual check 5 minutes later (< 12h cooldown) with v1.4.0 still dismissed -> bypasses BOTH cooldown and dismissedVersionTag!
            currentNow += 5L * 60L * 1000L
            val manualOutcome = repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = true)
            assertTrue(manualOutcome is UpdateCheckOutcome.UpdateAvailable)
            assertEquals("v1.4.0", (manualOutcome as UpdateCheckOutcome.UpdateAvailable).updateInfo.tagName)

            // Manual check while forceOfflineByApp is true -> immediately returns OfflineUnavailable
            val manualOffline = repo.checkForUpdates("1.3.0", forceOfflineByApp = true, manual = true)
            assertEquals(UpdateCheckOutcome.OfflineUnavailable, manualOffline)

            // 13 hours later v1.4.1 is published -> automatic check reactivates automatically since v1.4.1 > v1.4.0!
            currentNow += 13L * 60L * 60L * 1000L
            fakeApi.nextReleaseResponse = GitHubApiReleaseResponse.Success(
                jsonBody = sampleReleaseJson(tagName = "v1.4.1"),
                etag = "W/\"etag-141\""
            )
            val reactivatedOutcome = repo.checkForUpdates("1.3.0", forceOfflineByApp = false, manual = false)
            assertTrue(reactivatedOutcome is UpdateCheckOutcome.UpdateAvailable)
            assertEquals("v1.4.1", (reactivatedOutcome as UpdateCheckOutcome.UpdateAvailable).updateInfo.tagName)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `isAllowedHttpsDownloadUrl strictly enforces https and official GitHub hosts`() {
        assertTrue(
            HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(
                "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/app.apk"
            )
        )
        assertTrue(
            HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(
                "https://objects.githubusercontent.com/github-production-release-asset-2e65be/app.apk"
            )
        )
        assertTrue(
            HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(
                "https://release-assets.githubusercontent.com/ asset".trim()
            )
        )
        assertTrue(
            HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(
                "https://api.github.com/repos/laralbir/japan-travel-tools/releases/latest"
            )
        )

        // Disallowed schemes or hosts
        assertFalse(
            HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(
                "http://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/app.apk"
            )
        )
        assertFalse(
            HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(
                "https://evil-github.com/app.apk"
            )
        )
        assertFalse(
            HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(
                "https://github.com@attacker.example.com/app.apk"
            )
        )
        assertFalse(HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl("file:///data/local/tmp/app.apk"))
        assertFalse(HttpGitHubReleasesApiClient.isAllowedHttpsDownloadUrl(""))
    }

    @Test
    fun `downloadAndVerifyUpdateApk streams via part file emits progress resolves sha256 fallback and atomically renames verified apk`() = runBlocking {
        val tempDir = createTempUpdatesDir()
        try {
            val apkBytes = ByteArray(150_000) { idx -> ((idx * 31) and 0xFF).toByte() }
            val expectedSha256 = sha256Hex(apkBytes)
            val progressEvents = mutableListOf<Pair<Long, Long>>()

            val prefsStore = UpdatePreferencesDataStore(InMemoryKeyValueStore())
            val fakeApi = FakeGitHubReleasesApiClient(
                binaryPayload = apkBytes,
                textResponses = mapOf(
                    "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/japan-travel-tools-v1.4.0.apk.sha256" to
                        "$expectedSha256  japan-travel-tools-v1.4.0.apk\n"
                )
            )
            val repo = GitHubAutoUpdateRepository(
                preferencesDataStore = prefsStore,
                apiClient = fakeApi,
                signatureVerifier = createVerifier(
                    archiveVersionCode = 37L,
                    archiveCert = officialCertFingerprint,
                    installedCert = officialCertFingerprint
                ),
                updatesDirProvider = { tempDir },
                connectivityInspector = { wifiOnlineSnapshot() }
            )

            val updateInfo = sampleUpdateInfo(
                expectedSha256Hex = null,
                checksumAssetUrl = "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/japan-travel-tools-v1.4.0.apk.sha256"
            )

            val result = repo.downloadAndVerifyUpdateApk(
                updateInfo = updateInfo,
                installedVersionCode = 36L,
                onProgress = { downloaded, total ->
                    progressEvents.add(downloaded to total)
                }
            )

            assertTrue(result.isSuccess)
            val verifiedFile = result.getOrNull()
            assertNotNull(verifiedFile)
            assertEquals("jtt-1.4.0.apk", verifiedFile!!.name)
            assertTrue(verifiedFile.exists())
            assertEquals(apkBytes.size.toLong(), verifiedFile.length())
            assertFalse(File(tempDir, "jtt-1.4.0.apk.part").exists())
            assertEquals(verifiedFile.absolutePath, repo.preferencesFlow.value.pendingVerifiedApkPath)

            // Verify progressive emission reached 100%
            assertTrue(progressEvents.size >= 3)
            assertEquals(0L to apkBytes.size.toLong(), progressEvents.first())
            assertEquals(apkBytes.size.toLong() to apkBytes.size.toLong(), progressEvents.last())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `downloadAndVerifyUpdateApk rejects untrusted urls and immediately deletes part file on checksum or certificate mismatch`() = runBlocking {
        val tempDir = createTempUpdatesDir()
        try {
            val apkBytes = "authentic-apk-binary-payload".toByteArray(Charsets.UTF_8)
            val realSha256 = sha256Hex(apkBytes)
            val wrongSha256 = "0".repeat(64)

            val prefsStore = UpdatePreferencesDataStore(InMemoryKeyValueStore())
            val fakeApi = FakeGitHubReleasesApiClient(binaryPayload = apkBytes)

            // 1. Disallowed HTTP URL rejected before touching network
            val repo = GitHubAutoUpdateRepository(
                preferencesDataStore = prefsStore,
                apiClient = fakeApi,
                signatureVerifier = createVerifier(),
                updatesDirProvider = { tempDir },
                connectivityInspector = { wifiOnlineSnapshot() }
            )

            val insecureUrlResult = repo.downloadAndVerifyUpdateApk(
                updateInfo = sampleUpdateInfo(
                    apkDownloadUrl = "http://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/app.apk",
                    expectedSha256Hex = realSha256
                ),
                installedVersionCode = 36L,
                onProgress = { _, _ -> }
            )
            assertTrue(insecureUrlResult.isFailure)
            assertTrue(insecureUrlResult.exceptionOrNull() is SecurityException)
            assertEquals(0, fakeApi.downloadBinaryCalls)

            // 2. Missing both expectedSha256Hex and checksumAssetUrl fails closed
            val noChecksumResult = repo.downloadAndVerifyUpdateApk(
                updateInfo = sampleUpdateInfo(expectedSha256Hex = null, checksumAssetUrl = null),
                installedVersionCode = 36L,
                onProgress = { _, _ -> }
            )
            assertTrue(noChecksumResult.isFailure)
            assertEquals(0, fakeApi.downloadBinaryCalls)

            // 3. Corrupted SHA-256 deletes .part immediately
            val badChecksumResult = repo.downloadAndVerifyUpdateApk(
                updateInfo = sampleUpdateInfo(expectedSha256Hex = wrongSha256),
                installedVersionCode = 36L,
                onProgress = { _, _ -> }
            )
            assertTrue(badChecksumResult.isFailure)
            assertEquals(emptyList<String>(), tempDir.list()?.toList().orEmpty())
            assertNull(repo.preferencesFlow.value.pendingVerifiedApkPath)

            // 4. MITM / wrong X.509 certificate deletes .part immediately
            val mitmRepo = GitHubAutoUpdateRepository(
                preferencesDataStore = prefsStore,
                apiClient = fakeApi,
                signatureVerifier = createVerifier(
                    archiveVersionCode = 37L,
                    archiveCert = attackerCertFingerprint,
                    installedCert = officialCertFingerprint
                ),
                updatesDirProvider = { tempDir },
                connectivityInspector = { wifiOnlineSnapshot() }
            )
            val mitmResult = mitmRepo.downloadAndVerifyUpdateApk(
                updateInfo = sampleUpdateInfo(expectedSha256Hex = realSha256),
                installedVersionCode = 36L,
                onProgress = { _, _ -> }
            )
            assertTrue(mitmResult.isFailure)
            assertTrue(mitmResult.exceptionOrNull() is SecurityException)
            assertTrue(
                mitmResult.exceptionOrNull()?.message?.contains("La firma criptográfica") == true
            )
            assertEquals(emptyList<String>(), tempDir.list()?.toList().orEmpty())
            assertNull(mitmRepo.preferencesFlow.value.pendingVerifiedApkPath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `cancelling downloadAndVerifyUpdateApk mid-stream immediately deletes part file`() = runBlocking {
        val tempDir = createTempUpdatesDir()
        try {
            val firstChunkWritten = CompletableDeferred<File>()
            val allowContinue = CompletableDeferred<Unit>()
            val payload = ByteArray(128 * 1024) { 0x42 }
            val payloadSha = sha256Hex(payload)

            val blockingStreamingApi = object : GitHubReleasesApiClient {
                override suspend fun fetchReleasesJson(
                    includePreReleases: Boolean,
                    etag: String?
                ): GitHubApiReleaseResponse = GitHubApiReleaseResponse.NotModified

                override suspend fun fetchTextFile(httpsUrl: String): String = ""

                override suspend fun downloadBinaryFile(
                    httpsUrl: String,
                    targetPartFile: File,
                    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
                ) {
                    try {
                        targetPartFile.parentFile?.mkdirs()
                        FileOutputStream(targetPartFile).use { out ->
                            out.write(payload, 0, 64 * 1024)
                            out.flush()
                            onProgress(64L * 1024L, payload.size.toLong())
                            firstChunkWritten.complete(targetPartFile)
                            allowContinue.await()
                            currentCoroutineContext().ensureActive()
                            out.write(payload, 64 * 1024, 64 * 1024)
                        }
                    } catch (t: Throwable) {
                        if (targetPartFile.exists()) {
                            targetPartFile.delete()
                        }
                        throw t
                    }
                }
            }

            val repo = GitHubAutoUpdateRepository(
                preferencesDataStore = UpdatePreferencesDataStore(InMemoryKeyValueStore()),
                apiClient = blockingStreamingApi,
                signatureVerifier = createVerifier(),
                updatesDirProvider = { tempDir },
                connectivityInspector = { wifiOnlineSnapshot() }
            )

            val job = launch {
                repo.downloadAndVerifyUpdateApk(
                    updateInfo = sampleUpdateInfo(expectedSha256Hex = payloadSha),
                    installedVersionCode = 36L,
                    onProgress = { _, _ -> }
                )
            }

            val partFile = firstChunkWritten.await()
            assertTrue(partFile.exists())
            assertTrue(partFile.name.endsWith(".part"))

            job.cancelAndJoin()

            assertFalse(partFile.exists())
            assertEquals(emptyList<String>(), tempDir.list()?.toList().orEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `cleanupStaleUpdateFiles purges leftover part files and wipes updatesDir when versionCode increases`() {
        val tempDir = createTempUpdatesDir()
        try {
            val prefsStore = UpdatePreferencesDataStore(InMemoryKeyValueStore())
            prefsStore.setLastKnownInstalledVersionCode(36L)

            val leftoverPart = File(tempDir, "jtt-1.4.0.apk.part").apply { writeText("partial") }
            val verifiedApk = File(tempDir, "jtt-1.4.0.apk").apply { writeText("verified-apk") }
            prefsStore.setPendingVerifiedApkPath(verifiedApk.absolutePath)

            val repo = GitHubAutoUpdateRepository(
                preferencesDataStore = prefsStore,
                apiClient = FakeGitHubReleasesApiClient(),
                signatureVerifier = createVerifier(),
                updatesDirProvider = { tempDir },
                connectivityInspector = { wifiOnlineSnapshot() }
            )

            // Same versionCode (36) -> deletes leftover .part but keeps fresh pending verified .apk
            repo.cleanupStaleUpdateFiles(currentVersionCode = 36L)
            assertFalse(leftoverPart.exists())
            assertTrue(verifiedApk.exists())
            assertEquals(verifiedApk.absolutePath, repo.preferencesFlow.value.pendingVerifiedApkPath)

            // App updated to versionCode = 37 -> deletes ALL files in updatesDir, clears pending path, records 37
            File(tempDir, "orphan.apk.part").writeText("orphan")
            repo.cleanupStaleUpdateFiles(currentVersionCode = 37L)
            assertFalse(verifiedApk.exists())
            assertEquals(emptyList<String>(), tempDir.list()?.toList().orEmpty())
            assertNull(repo.preferencesFlow.value.pendingVerifiedApkPath)
            assertEquals(37L, repo.preferencesFlow.value.lastKnownInstalledVersionCode)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun createTempUpdatesDir(): File =
        Files.createTempDirectory("jtt-updater-test-").toFile()

    private fun wifiOnlineSnapshot(): ConnectivityStatusSnapshot =
        ConnectivityStatusSnapshot(
            kind = ActiveConnectionKind.WIFI,
            isOnline = true,
            isMetered = false,
            isRoaming = false,
            forceOfflineByApp = false
        )

    private fun createVerifier(
        archivePackageName: String = "com.laralbir.japantraveltools",
        archiveVersionCode: Long = 37L,
        archiveCert: String = officialCertFingerprint,
        installedCert: String = officialCertFingerprint
    ): ApkSignatureVerifier {
        val inspector = object : PackageSignatureInspector {
            override fun getInstalledSignatureMetadata(packageName: String): ArchiveSignatureMetadata =
                ArchiveSignatureMetadata(
                    packageName = packageName,
                    versionCode = 36L,
                    certSha256Fingerprints = setOf(installedCert)
                )

            override fun getArchiveSignatureMetadata(apkFile: File): ArchiveSignatureMetadata =
                ArchiveSignatureMetadata(
                    packageName = archivePackageName,
                    versionCode = archiveVersionCode,
                    certSha256Fingerprints = setOf(archiveCert)
                )
        }
        return ApkSignatureVerifier(signatureInspector = inspector)
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun sampleUpdateInfo(
        tagName: String = "v1.4.0",
        cleanVersionName: String = "1.4.0",
        apkDownloadUrl: String = "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/japan-travel-tools-v1.4.0.apk",
        expectedSha256Hex: String? = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        checksumAssetUrl: String? = null
    ): AppUpdateInfo = AppUpdateInfo(
        tagName = tagName,
        cleanVersionName = cleanVersionName,
        releaseTitle = "Japan Travel Tools $tagName",
        changelogMarkdown = "### Added\n- Auto-updater",
        publishedAtIso = "2026-11-08T08:00:00Z",
        apkAssetName = "japan-travel-tools-$tagName.apk",
        apkDownloadUrl = apkDownloadUrl,
        apkSizeBytes = 150_000L,
        expectedSha256Hex = expectedSha256Hex,
        checksumAssetUrl = checksumAssetUrl,
        isPreRelease = false,
        releasePageUrl = "https://github.com/laralbir/japan-travel-tools/releases/tag/$tagName"
    )

    private fun sampleReleaseJson(tagName: String): String = """
        {
          "id": 101,
          "tag_name": "$tagName",
          "name": "Japan Travel Tools $tagName",
          "body": "### Added\n- Auto-updater",
          "draft": false,
          "prerelease": false,
          "published_at": "2026-11-08T08:00:00Z",
          "html_url": "https://github.com/laralbir/japan-travel-tools/releases/tag/$tagName",
          "assets": [
            {
              "id": 501,
              "name": "japan-travel-tools-$tagName.apk",
              "content_type": "application/vnd.android.package-archive",
              "size": 19800000,
              "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/$tagName/japan-travel-tools-$tagName.apk",
              "digest": "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            }
          ]
        }
    """.trimIndent()

    private class FakeGitHubReleasesApiClient(
        var nextReleaseResponse: GitHubApiReleaseResponse = GitHubApiReleaseResponse.NotModified,
        private val textResponses: Map<String, String> = emptyMap(),
        private val binaryPayload: ByteArray = ByteArray(0)
    ) : GitHubReleasesApiClient {

        data class FetchCall(val includePreReleases: Boolean, val etag: String?)

        val fetchReleasesCalls = mutableListOf<FetchCall>()
        var downloadBinaryCalls: Int = 0

        override suspend fun fetchReleasesJson(
            includePreReleases: Boolean,
            etag: String?
        ): GitHubApiReleaseResponse {
            fetchReleasesCalls.add(FetchCall(includePreReleases, etag))
            return nextReleaseResponse
        }

        override suspend fun fetchTextFile(httpsUrl: String): String =
            textResponses[httpsUrl] ?: error("Unexpected text URL: $httpsUrl")

        override suspend fun downloadBinaryFile(
            httpsUrl: String,
            targetPartFile: File,
            onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
        ) {
            downloadBinaryCalls++
            targetPartFile.parentFile?.mkdirs()
            val total = binaryPayload.size.toLong()
            onProgress(0L, total)
            FileOutputStream(targetPartFile).use { out ->
                val chunkSize = 64 * 1024
                var offset = 0
                while (offset < binaryPayload.size) {
                    currentCoroutineContext().ensureActive()
                    val count = minOf(chunkSize, binaryPayload.size - offset)
                    out.write(binaryPayload, offset, count)
                    offset += count
                    onProgress(offset.toLong(), total)
                }
            }
        }
    }
}
