package com.laralbir.japantraveltools.feature.updater.data

import com.laralbir.japantraveltools.feature.updater.domain.AppUpdateInfo
import com.laralbir.japantraveltools.feature.updater.domain.SemanticVersion
import com.laralbir.japantraveltools.feature.updater.domain.SemanticVersionComparator
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class GitHubAssetDto(
    val id: Long,
    val name: String,
    val contentType: String,
    val sizeBytes: Long,
    val browserDownloadUrl: String,
    val digest: String?
)

data class GitHubReleaseDto(
    val id: Long,
    val tagName: String,
    val name: String,
    val bodyMarkdown: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val publishedAtIso: String,
    val htmlUrl: String,
    val assets: List<GitHubAssetDto>
)

object GitHubReleaseJsonParser {

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private val SHA256_HEX_REGEX = Regex("^[0-9a-fA-F]{64}$")
    private val CHECKSUM_LINE_REGEX = Regex(
        """^(?:sha256:)?([0-9a-fA-F]{64})(?:[\s]+(?:\*)?(.+?))?$""",
        RegexOption.IGNORE_CASE
    )
    private val BSD_CHECKSUM_LINE_REGEX = Regex(
        """^SHA256\s*\((.+)\)\s*=\s*([0-9a-fA-F]{64})$""",
        RegexOption.IGNORE_CASE
    )

    fun parseSingleRelease(json: String): GitHubReleaseDto =
        parseReleaseObject(JSONObject(json))

    fun parseReleaseList(json: String): List<GitHubReleaseDto> {
        val array = JSONArray(json)
        val result = ArrayList<GitHubReleaseDto>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            result.add(parseReleaseObject(obj))
        }
        return result
    }

    fun selectBestRelease(
        releases: List<GitHubReleaseDto>,
        includePreReleases: Boolean
    ): GitHubReleaseDto? {
        return releases
            .asSequence()
            .filter { release -> toAppUpdateInfo(release, includePreReleases) != null }
            .maxWithOrNull { a, b -> SemanticVersionComparator.compare(a.tagName, b.tagName) }
    }

    fun toAppUpdateInfo(
        dto: GitHubReleaseDto,
        includePreReleases: Boolean = true
    ): AppUpdateInfo? {
        if (dto.draft) return null
        val parsedVersion = SemanticVersion.parseOrNull(dto.tagName) ?: return null
        val effectivePreRelease = dto.prerelease || parsedVersion.isPreRelease
        if (effectivePreRelease && !includePreReleases) return null

        val apkAsset = findApkAsset(dto.assets) ?: return null
        val cleanTag = dto.tagName.trim()

        return AppUpdateInfo(
            tagName = cleanTag,
            cleanVersionName = parsedVersion.cleanString,
            releaseTitle = dto.name.ifBlank { cleanTag },
            changelogMarkdown = dto.bodyMarkdown,
            publishedAtIso = dto.publishedAtIso,
            apkAssetName = apkAsset.name,
            apkDownloadUrl = apkAsset.browserDownloadUrl,
            apkSizeBytes = apkAsset.sizeBytes,
            expectedSha256Hex = extractSha256FromDigest(apkAsset.digest),
            checksumAssetUrl = findChecksumAssetUrl(dto.assets, apkAsset.name),
            isPreRelease = effectivePreRelease,
            releasePageUrl = dto.htmlUrl
        )
    }

    fun parseSha256ChecksumFile(
        content: String,
        expectedFileName: String? = null
    ): String? {
        if (content.isBlank()) return null

        data class ChecksumEntry(val sha256Hex: String, val fileName: String?)

        val entries = mutableListOf<ChecksumEntry>()
        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val standardMatch = CHECKSUM_LINE_REGEX.matchEntire(line)
            if (standardMatch != null) {
                val hex = standardMatch.groupValues[1].lowercase(Locale.ROOT)
                val rawFile = standardMatch.groupValues[2].trim().takeIf { it.isNotEmpty() }
                entries.add(ChecksumEntry(hex, rawFile?.let(::extractBaseName)))
                continue
            }

            val bsdMatch = BSD_CHECKSUM_LINE_REGEX.matchEntire(line)
            if (bsdMatch != null) {
                val rawFile = bsdMatch.groupValues[1].trim().takeIf { it.isNotEmpty() }
                val hex = bsdMatch.groupValues[2].lowercase(Locale.ROOT)
                entries.add(ChecksumEntry(hex, rawFile?.let(::extractBaseName)))
            }
        }

        if (entries.isEmpty()) return null

        val normalizedExpected = expectedFileName?.trim()?.takeIf { it.isNotEmpty() }?.let(::extractBaseName)
        if (normalizedExpected != null) {
            val exactMatch = entries.firstOrNull { entry ->
                entry.fileName != null && entry.fileName.equals(normalizedExpected, ignoreCase = true)
            }
            if (exactMatch != null) return exactMatch.sha256Hex

            // If the checksum file contains only bare hash(es) with no filenames, accept the first hash.
            if (entries.all { it.fileName == null }) {
                return entries.first().sha256Hex
            }
            return null
        }

        return entries.firstOrNull { it.fileName?.endsWith(".apk", ignoreCase = true) == true }?.sha256Hex
            ?: entries.first().sha256Hex
    }

    private fun parseReleaseObject(obj: JSONObject): GitHubReleaseDto {
        val assetsArray = obj.optJSONArray("assets")
        val assets = if (assetsArray != null) {
            ArrayList<GitHubAssetDto>(assetsArray.length()).apply {
                for (i in 0 until assetsArray.length()) {
                    val assetObj = assetsArray.optJSONObject(i) ?: continue
                    add(parseAssetObject(assetObj))
                }
            }
        } else {
            emptyList()
        }

        val publishedAt = obj.optNonNullString("published_at")
            .ifBlank { obj.optNonNullString("created_at") }
            .trim()

        return GitHubReleaseDto(
            id = obj.optLong("id", 0L),
            tagName = obj.optNonNullString("tag_name").trim(),
            name = obj.optNonNullString("name").trim(),
            bodyMarkdown = obj.optNonNullString("body"),
            draft = obj.optBoolean("draft", false),
            prerelease = obj.optBoolean("prerelease", false),
            publishedAtIso = publishedAt,
            htmlUrl = obj.optNonNullString("html_url").trim(),
            assets = assets
        )
    }

    private fun parseAssetObject(obj: JSONObject): GitHubAssetDto {
        return GitHubAssetDto(
            id = obj.optLong("id", 0L),
            name = obj.optNonNullString("name").trim(),
            contentType = obj.optNonNullString("content_type").trim(),
            sizeBytes = obj.optLong("size", 0L),
            browserDownloadUrl = obj.optNonNullString("browser_download_url").trim(),
            digest = obj.optNullableString("digest")?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    private fun findApkAsset(assets: List<GitHubAssetDto>): GitHubAssetDto? {
        val validApkAssets = assets.filter { asset ->
            asset.browserDownloadUrl.isNotBlank() &&
                asset.name.endsWith(".apk", ignoreCase = true)
        }
        return validApkAssets.firstOrNull { asset ->
            asset.contentType.equals(APK_MIME_TYPE, ignoreCase = true)
        } ?: validApkAssets.firstOrNull()
    }

    private fun findChecksumAssetUrl(
        assets: List<GitHubAssetDto>,
        apkAssetName: String
    ): String? {
        val apkBaseName = apkAssetName.substring(0, apkAssetName.length - ".apk".length)
        val candidates = assets.filter { it.browserDownloadUrl.isNotBlank() }

        val exactApkSha = candidates.firstOrNull { asset ->
            asset.name.equals("$apkAssetName.sha256", ignoreCase = true) ||
                asset.name.equals("$apkAssetName.sha256sum", ignoreCase = true)
        }
        if (exactApkSha != null) return exactApkSha.browserDownloadUrl

        val baseSha = candidates.firstOrNull { asset ->
            asset.name.equals("$apkBaseName.sha256", ignoreCase = true) ||
                asset.name.equals("$apkBaseName.sha256sum", ignoreCase = true)
        }
        if (baseSha != null) return baseSha.browserDownloadUrl

        val genericChecksums = candidates.firstOrNull { asset ->
            asset.name.equals("checksums.sha256", ignoreCase = true) ||
                asset.name.equals("checksum.sha256", ignoreCase = true) ||
                asset.name.equals("SHA256SUMS", ignoreCase = true) ||
                asset.name.equals("sha256sums.txt", ignoreCase = true)
        }
        if (genericChecksums != null) return genericChecksums.browserDownloadUrl

        return candidates.firstOrNull { asset ->
            asset.name.endsWith(".sha256", ignoreCase = true) ||
                asset.name.endsWith(".sha256sum", ignoreCase = true)
        }?.browserDownloadUrl
    }

    private fun extractSha256FromDigest(digest: String?): String? {
        if (digest.isNullOrBlank()) return null
        val trimmed = digest.trim()
        val hexCandidate = if (trimmed.startsWith("sha256:", ignoreCase = true)) {
            trimmed.substring("sha256:".length).trim()
        } else {
            trimmed
        }
        return if (SHA256_HEX_REGEX.matches(hexCandidate)) {
            hexCandidate.lowercase(Locale.ROOT)
        } else {
            null
        }
    }

    private fun extractBaseName(path: String): String =
        path.substringAfterLast('/').substringAfterLast('\\')

    private fun JSONObject.optNonNullString(key: String, default: String = ""): String =
        if (isNull(key)) default else optString(key, default)

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key, "")
}
