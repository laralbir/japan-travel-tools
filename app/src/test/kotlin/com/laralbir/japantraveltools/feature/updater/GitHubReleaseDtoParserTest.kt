package com.laralbir.japantraveltools.feature.updater

import com.laralbir.japantraveltools.feature.updater.data.GitHubAssetDto
import com.laralbir.japantraveltools.feature.updater.data.GitHubReleaseDto
import com.laralbir.japantraveltools.feature.updater.data.GitHubReleaseJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseDtoParserTest {

    private val validHashLower =
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    private val secondaryHashLower =
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test
    fun `parseSingleRelease and toAppUpdateInfo extract apk metadata and strip sha256 digest prefix`() {
        val latestJson = """
            {
              "id": 901234,
              "tag_name": "v1.4.0",
              "name": "Japan Travel Tools v1.4.0",
              "body": "## Added\n- GitHub Releases OTA updater\n- APK Signature Scheme v2/v3",
              "draft": false,
              "prerelease": false,
              "published_at": "2026-11-08T08:00:00Z",
              "html_url": "https://github.com/laralbir/japan-travel-tools/releases/tag/v1.4.0",
              "assets": [
                {
                  "id": 1001,
                  "name": "japan-travel-tools-v1.4.0.apk",
                  "content_type": "application/vnd.android.package-archive",
                  "size": 20971520,
                  "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/japan-travel-tools-v1.4.0.apk",
                  "digest": "SHA256:${validHashLower.uppercase()}"
                },
                {
                  "id": 1002,
                  "name": "japan-travel-tools-v1.4.0.apk.sha256",
                  "content_type": "application/octet-stream",
                  "size": 99,
                  "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/japan-travel-tools-v1.4.0.apk.sha256",
                  "digest": null
                }
              ]
            }
        """.trimIndent()

        val dto = GitHubReleaseJsonParser.parseSingleRelease(latestJson)
        assertEquals(901234L, dto.id)
        assertEquals("v1.4.0", dto.tagName)
        assertEquals("Japan Travel Tools v1.4.0", dto.name)
        assertTrue(dto.bodyMarkdown.contains("GitHub Releases OTA updater"))
        assertFalse(dto.draft)
        assertFalse(dto.prerelease)
        assertEquals("2026-11-08T08:00:00Z", dto.publishedAtIso)
        assertEquals(2, dto.assets.size)
        assertNull(dto.assets[1].digest)

        val updateInfo = GitHubReleaseJsonParser.toAppUpdateInfo(dto, includePreReleases = false)
        assertNotNull(updateInfo)
        assertEquals("v1.4.0", updateInfo!!.tagName)
        assertEquals("1.4.0", updateInfo.cleanVersionName)
        assertEquals("Japan Travel Tools v1.4.0", updateInfo.releaseTitle)
        assertEquals("japan-travel-tools-v1.4.0.apk", updateInfo.apkAssetName)
        assertEquals(
            "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/japan-travel-tools-v1.4.0.apk",
            updateInfo.apkDownloadUrl
        )
        assertEquals(20971520L, updateInfo.apkSizeBytes)
        assertEquals(validHashLower, updateInfo.expectedSha256Hex)
        assertEquals(
            "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/japan-travel-tools-v1.4.0.apk.sha256",
            updateInfo.checksumAssetUrl
        )
        assertFalse(updateInfo.isPreRelease)
        assertEquals(
            "https://github.com/laralbir/japan-travel-tools/releases/tag/v1.4.0",
            updateInfo.releasePageUrl
        )
    }

    @Test
    fun `toAppUpdateInfo falls back to sha256 or checksums asset when digest is null or invalid and handles null json fields`() {
        val jsonWithNullFields = """
            {
              "id": 901235,
              "tag_name": "v1.4.1",
              "name": null,
              "body": null,
              "draft": false,
              "prerelease": false,
              "published_at": "2026-11-08T09:30:00Z",
              "html_url": "https://github.com/laralbir/japan-travel-tools/releases/tag/v1.4.1",
              "assets": [
                {
                  "id": 2001,
                  "name": "checksums.sha256",
                  "content_type": "text/plain",
                  "size": 190,
                  "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.1/checksums.sha256",
                  "digest": null
                },
                {
                  "id": 2002,
                  "name": "japan-travel-tools-v1.4.1.apk",
                  "content_type": "application/octet-stream",
                  "size": 19500000,
                  "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.1/japan-travel-tools-v1.4.1.apk",
                  "digest": "sha256:not-a-valid-64-hex"
                }
              ]
            }
        """.trimIndent()

        val dto = GitHubReleaseJsonParser.parseSingleRelease(jsonWithNullFields)
        assertEquals("", dto.name)
        assertEquals("", dto.bodyMarkdown)

        val updateInfo = GitHubReleaseJsonParser.toAppUpdateInfo(dto)
        assertNotNull(updateInfo)
        assertEquals("v1.4.1", updateInfo!!.releaseTitle)
        assertEquals("", updateInfo.changelogMarkdown)
        assertNull(updateInfo.expectedSha256Hex)
        assertEquals(
            "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.1/checksums.sha256",
            updateInfo.checksumAssetUrl
        )
    }

    @Test
    fun `toAppUpdateInfo rejects drafts disallowed prereleases invalid semver and releases without apk`() {
        val apkAsset = GitHubAssetDto(
            id = 1L,
            name = "japan-travel-tools-v1.4.0.apk",
            contentType = "application/vnd.android.package-archive",
            sizeBytes = 10_000_000L,
            browserDownloadUrl = "https://github.com/laralbir/japan-travel-tools/releases/download/v1.4.0/app.apk",
            digest = "sha256:$validHashLower"
        )
        val baseRelease = GitHubReleaseDto(
            id = 10L,
            tagName = "v1.4.0",
            name = "v1.4.0",
            bodyMarkdown = "Notes",
            draft = false,
            prerelease = false,
            publishedAtIso = "2026-11-08T00:00:00Z",
            htmlUrl = "https://github.com/laralbir/japan-travel-tools/releases/tag/v1.4.0",
            assets = listOf(apkAsset)
        )

        assertNull(GitHubReleaseJsonParser.toAppUpdateInfo(baseRelease.copy(draft = true)))

        val preReleaseDto = baseRelease.copy(tagName = "v1.4.0-beta.1", prerelease = true)
        assertNull(GitHubReleaseJsonParser.toAppUpdateInfo(preReleaseDto, includePreReleases = false))

        val allowedPreRelease = GitHubReleaseJsonParser.toAppUpdateInfo(
            preReleaseDto,
            includePreReleases = true
        )
        assertNotNull(allowedPreRelease)
        assertEquals("1.4.0-beta.1", allowedPreRelease!!.cleanVersionName)
        assertTrue(allowedPreRelease.isPreRelease)

        assertNull(GitHubReleaseJsonParser.toAppUpdateInfo(baseRelease.copy(tagName = "nightly-build")))
        assertNull(
            GitHubReleaseJsonParser.toAppUpdateInfo(
                baseRelease.copy(
                    assets = listOf(apkAsset.copy(name = "japan-travel-tools-v1.4.0.apk.sha256"))
                )
            )
        )
    }

    @Test
    fun `parseReleaseList and selectBestRelease filter drafts and respect includePreReleases by SemVer precedence`() {
        val releasesJson = """
            [
              {
                "id": 1,
                "tag_name": "v2.0.0",
                "name": "Draft v2.0.0",
                "body": "Unpublished draft",
                "draft": true,
                "prerelease": false,
                "published_at": "2026-11-08T10:00:00Z",
                "html_url": "https://github.com/laralbir/japan-travel-tools/releases/tag/v2.0.0",
                "assets": [
                  {
                    "id": 11,
                    "name": "japan-travel-tools-v2.0.0.apk",
                    "content_type": "application/vnd.android.package-archive",
                    "size": 21000000,
                    "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/v2.0.0/app.apk",
                    "digest": "sha256:$validHashLower"
                  }
                ]
              },
              {
                "id": 2,
                "tag_name": "v1.11.0-beta.2",
                "name": "Beta 2",
                "body": "Beta 2 notes",
                "draft": false,
                "prerelease": true,
                "published_at": "2026-11-08T09:00:00Z",
                "html_url": "https://github.com/laralbir/japan-travel-tools/releases/tag/v1.11.0-beta.2",
                "assets": [
                  {
                    "id": 12,
                    "name": "japan-travel-tools-v1.11.0-beta.2.apk",
                    "content_type": "application/vnd.android.package-archive",
                    "size": 20500000,
                    "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/v1.11.0-beta.2/app.apk",
                    "digest": "sha256:$validHashLower"
                  }
                ]
              },
              {
                "id": 3,
                "tag_name": "v1.11.0-beta.10",
                "name": "Beta 10",
                "body": "Beta 10 notes",
                "draft": false,
                "prerelease": true,
                "published_at": "2026-11-08T09:30:00Z",
                "html_url": "https://github.com/laralbir/japan-travel-tools/releases/tag/v1.11.0-beta.10",
                "assets": [
                  {
                    "id": 13,
                    "name": "japan-travel-tools-v1.11.0-beta.10.apk",
                    "content_type": "application/vnd.android.package-archive",
                    "size": 20600000,
                    "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/v1.11.0-beta.10/app.apk",
                    "digest": "sha256:$validHashLower"
                  }
                ]
              },
              {
                "id": 4,
                "tag_name": "v1.2.9",
                "name": "v1.2.9",
                "body": "Older minor",
                "draft": false,
                "prerelease": false,
                "published_at": "2026-10-01T00:00:00Z",
                "html_url": "https://github.com/laralbir/japan-travel-tools/releases/tag/v1.2.9",
                "assets": [
                  {
                    "id": 14,
                    "name": "japan-travel-tools-v1.2.9.apk",
                    "content_type": "application/vnd.android.package-archive",
                    "size": 18000000,
                    "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/v1.2.9/app.apk",
                    "digest": "sha256:$validHashLower"
                  }
                ]
              },
              {
                "id": 5,
                "tag_name": "v1.10.0",
                "name": "v1.10.0",
                "body": "Highest stable",
                "draft": false,
                "prerelease": false,
                "published_at": "2026-11-01T00:00:00Z",
                "html_url": "https://github.com/laralbir/japan-travel-tools/releases/tag/v1.10.0",
                "assets": [
                  {
                    "id": 15,
                    "name": "japan-travel-tools-v1.10.0.apk",
                    "content_type": "application/vnd.android.package-archive",
                    "size": 19900000,
                    "browser_download_url": "https://github.com/laralbir/japan-travel-tools/releases/download/v1.10.0/app.apk",
                    "digest": "sha256:$validHashLower"
                  }
                ]
              },
              {
                "id": 6,
                "tag_name": "v1.12.0",
                "name": "Release without APK asset",
                "body": "Docs only",
                "draft": false,
                "prerelease": false,
                "published_at": "2026-11-08T11:00:00Z",
                "html_url": "https://github.com/laralbir/japan-travel-tools/releases/tag/v1.12.0",
                "assets": []
              }
            ]
        """.trimIndent()

        val releases = GitHubReleaseJsonParser.parseReleaseList(releasesJson)
        assertEquals(6, releases.size)

        val bestStable = GitHubReleaseJsonParser.selectBestRelease(
            releases = releases,
            includePreReleases = false
        )
        assertNotNull(bestStable)
        assertEquals("v1.10.0", bestStable!!.tagName)

        val bestWithPreReleases = GitHubReleaseJsonParser.selectBestRelease(
            releases = releases,
            includePreReleases = true
        )
        assertNotNull(bestWithPreReleases)
        assertEquals("v1.11.0-beta.10", bestWithPreReleases!!.tagName)
    }

    @Test
    fun `parseSha256ChecksumFile parses gnu coreutils binary mode bare hex and multi-file checksums`() {
        assertEquals(
            validHashLower,
            GitHubReleaseJsonParser.parseSha256ChecksumFile(
                "${validHashLower.uppercase()}  japan-travel-tools-v1.4.0.apk\n",
                expectedFileName = "japan-travel-tools-v1.4.0.apk"
            )
        )

        assertEquals(
            validHashLower,
            GitHubReleaseJsonParser.parseSha256ChecksumFile(
                "$validHashLower *japan-travel-tools-v1.4.0.apk\r\n",
                expectedFileName = "japan-travel-tools-v1.4.0.apk"
            )
        )

        assertEquals(
            validHashLower,
            GitHubReleaseJsonParser.parseSha256ChecksumFile(
                "\n  ${validHashLower.uppercase()}  \n",
                expectedFileName = "japan-travel-tools-v1.4.0.apk"
            )
        )

        val multiLineChecksums = """
            # Release artifacts checksums
            $secondaryHashLower  source-archive.tar.gz
            $validHashLower  ./dist/japan-travel-tools-v1.4.0.apk
        """.trimIndent()

        assertEquals(
            validHashLower,
            GitHubReleaseJsonParser.parseSha256ChecksumFile(
                multiLineChecksums,
                expectedFileName = "japan-travel-tools-v1.4.0.apk"
            )
        )

        assertNull(
            GitHubReleaseJsonParser.parseSha256ChecksumFile(
                multiLineChecksums,
                expectedFileName = "non-existent.apk"
            )
        )

        assertNull(GitHubReleaseJsonParser.parseSha256ChecksumFile(""))
        assertNull(GitHubReleaseJsonParser.parseSha256ChecksumFile("1234567890abcdef  app.apk"))
    }
}
