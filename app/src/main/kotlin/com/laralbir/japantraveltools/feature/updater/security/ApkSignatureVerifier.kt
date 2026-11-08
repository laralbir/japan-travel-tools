package com.laralbir.japantraveltools.feature.updater.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.util.Locale

data class ArchiveSignatureMetadata(
    val packageName: String,
    val versionCode: Long,
    val certSha256Fingerprints: Set<String>,
    val hasMultipleSigners: Boolean = false
)

interface PackageSignatureInspector {
    fun getInstalledSignatureMetadata(packageName: String): ArchiveSignatureMetadata?
    fun getArchiveSignatureMetadata(apkFile: File): ArchiveSignatureMetadata?
}

sealed interface ApkVerificationResult {
    data class Verified(
        val sha256Hex: String,
        val certSha256Hex: String,
        val archiveVersionCode: Long
    ) : ApkVerificationResult

    data class ChecksumMismatch(
        val expectedHex: String?,
        val actualHex: String
    ) : ApkVerificationResult

    data class InvalidArchive(
        val reason: String
    ) : ApkVerificationResult

    data class PackageNameMismatch(
        val expectedPackage: String,
        val actualPackage: String
    ) : ApkVerificationResult

    data class VersionDowngradeBlocked(
        val installedVersionCode: Long,
        val archiveVersionCode: Long
    ) : ApkVerificationResult

    data class SignatureMismatch(
        val installedCertSha256: Set<String>,
        val archiveCertSha256: Set<String>
    ) : ApkVerificationResult
}

class AndroidPackageSignatureInspector(
    private val context: Context
) : PackageSignatureInspector {

    override fun getInstalledSignatureMetadata(packageName: String): ArchiveSignatureMetadata? {
        if (packageName.isBlank()) return null
        val pm = context.packageManager
        return try {
            val packageInfo = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    pm.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                }
                else -> {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                }
            }
            extractSignatureMetadata(packageInfo)
        } catch (_: Exception) {
            null
        }
    }

    override fun getArchiveSignatureMetadata(apkFile: File): ArchiveSignatureMetadata? {
        if (!apkFile.exists() || !apkFile.isFile || apkFile.length() <= 0L) return null
        val pm = context.packageManager
        val archivePath = apkFile.absolutePath
        return try {
            val packageInfo = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    pm.getPackageArchiveInfo(
                        archivePath,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNING_CERTIFICATES)
                }
                else -> {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNATURES)
                }
            } ?: return null
            extractSignatureMetadata(packageInfo)
        } catch (_: Exception) {
            null
        }
    }

    fun getInstalledShortFingerprint(): String {
        val primaryFingerprint = getInstalledSignatureMetadata(context.packageName)
            ?.certSha256Fingerprints
            ?.firstOrNull()
            ?: return UNAVAILABLE_FINGERPRINT
        return ApkSignatureVerifier.formatShortFingerprint(primaryFingerprint)
    }

    private fun extractSignatureMetadata(packageInfo: PackageInfo): ArchiveSignatureMetadata? {
        val pkgName = packageInfo.packageName.takeIf { it.isNotBlank() } ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return null
            val multipleSigners = signingInfo.hasMultipleSigners()
            val signatures: List<Signature> = if (multipleSigners) {
                signingInfo.apkContentsSigners?.filterNotNull().orEmpty()
            } else {
                val history = signingInfo.signingCertificateHistory?.filterNotNull().orEmpty()
                if (history.isNotEmpty()) {
                    history.asReversed()
                } else {
                    signingInfo.apkContentsSigners?.filterNotNull().orEmpty()
                }
            }
            if (signatures.isEmpty()) return null
            val fingerprints = signatures
                .map { sig -> ApkSignatureVerifier.sha256BytesToHex(sig.toByteArray()) }
                .filter { it.isNotEmpty() }
                .toSet()
            if (fingerprints.isEmpty()) return null
            ArchiveSignatureMetadata(
                packageName = pkgName,
                versionCode = packageInfo.longVersionCode,
                certSha256Fingerprints = fingerprints,
                hasMultipleSigners = multipleSigners
            )
        } else {
            @Suppress("DEPRECATION")
            val rawSignatures = packageInfo.signatures?.filterNotNull().orEmpty()
            if (rawSignatures.isEmpty()) return null
            val fingerprints = rawSignatures
                .map { sig -> ApkSignatureVerifier.sha256BytesToHex(sig.toByteArray()) }
                .filter { it.isNotEmpty() }
                .toSet()
            if (fingerprints.isEmpty()) return null
            @Suppress("DEPRECATION")
            val versionCode = packageInfo.versionCode.toLong()
            ArchiveSignatureMetadata(
                packageName = pkgName,
                versionCode = versionCode,
                certSha256Fingerprints = fingerprints,
                hasMultipleSigners = fingerprints.size > 1
            )
        }
    }

    private companion object {
        const val UNAVAILABLE_FINGERPRINT = "—"
    }
}

class ApkSignatureVerifier(
    private val signatureInspector: PackageSignatureInspector,
    private val expectedPackageName: String = "com.laralbir.japantraveltools"
) {

    fun computeFileSha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(STREAM_BUFFER_SIZE_BYTES).use { input ->
            val buffer = ByteArray(STREAM_BUFFER_SIZE_BYTES)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead < 0) break
                if (bytesRead > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
        }
        return encodeLowerHex(digest.digest())
    }

    fun verifyApk(
        apkFile: File,
        expectedSha256Hex: String?,
        installedVersionCode: Long
    ): ApkVerificationResult {
        if (!apkFile.exists() || !apkFile.isFile) {
            return ApkVerificationResult.InvalidArchive("APK file does not exist: ${apkFile.path}")
        }
        if (apkFile.length() <= 0L) {
            return ApkVerificationResult.InvalidArchive("APK file is empty: ${apkFile.path}")
        }

        val actualSha256Hex = try {
            computeFileSha256Hex(apkFile)
        } catch (e: Exception) {
            return ApkVerificationResult.InvalidArchive("Unable to read APK file: ${e.message}")
        }

        val normalizedExpectedSha256 = normalizeSha256HexOrNull(expectedSha256Hex)
            ?: return ApkVerificationResult.ChecksumMismatch(
                expectedHex = expectedSha256Hex,
                actualHex = actualSha256Hex
            )

        val checksumMatches = MessageDigest.isEqual(
            normalizedExpectedSha256.toByteArray(Charsets.US_ASCII),
            actualSha256Hex.toByteArray(Charsets.US_ASCII)
        )
        if (!checksumMatches) {
            return ApkVerificationResult.ChecksumMismatch(
                expectedHex = normalizedExpectedSha256,
                actualHex = actualSha256Hex
            )
        }

        val archiveMetadata = signatureInspector.getArchiveSignatureMetadata(apkFile)
            ?: return ApkVerificationResult.InvalidArchive("Unable to parse APK archive or signing certificates.")

        if (archiveMetadata.packageName != expectedPackageName) {
            return ApkVerificationResult.PackageNameMismatch(
                expectedPackage = expectedPackageName,
                actualPackage = archiveMetadata.packageName
            )
        }

        if (archiveMetadata.versionCode <= installedVersionCode) {
            return ApkVerificationResult.VersionDowngradeBlocked(
                installedVersionCode = installedVersionCode,
                archiveVersionCode = archiveMetadata.versionCode
            )
        }

        val installedMetadata = signatureInspector.getInstalledSignatureMetadata(expectedPackageName)
        val installedCerts = installedMetadata
            ?.certSha256Fingerprints
            ?.mapNotNull { normalizeSha256HexOrNull(it) }
            ?.toSet()
            .orEmpty()
        val archiveCerts = archiveMetadata
            .certSha256Fingerprints
            .mapNotNull { normalizeSha256HexOrNull(it) }
            .toSet()

        val signersMatch = installedMetadata != null &&
            installedCerts.isNotEmpty() &&
            archiveCerts.isNotEmpty() &&
            installedMetadata.hasMultipleSigners == archiveMetadata.hasMultipleSigners &&
            installedCerts == archiveCerts

        if (!signersMatch) {
            return ApkVerificationResult.SignatureMismatch(
                installedCertSha256 = installedCerts,
                archiveCertSha256 = archiveCerts
            )
        }

        return ApkVerificationResult.Verified(
            sha256Hex = actualSha256Hex,
            certSha256Hex = archiveCerts.first(),
            archiveVersionCode = archiveMetadata.versionCode
        )
    }

    companion object {
        private const val STREAM_BUFFER_SIZE_BYTES = 64 * 1024
        private const val UNAVAILABLE_FINGERPRINT = "—"
        private val HEX_CHARS = "0123456789abcdef".toCharArray()
        private val SHA256_HEX_REGEX = Regex("^[0-9a-f]{64}$")

        fun formatShortFingerprint(sha256Hex: String): String {
            val cleaned = sha256Hex
                .trim()
                .let { raw ->
                    if (raw.startsWith("sha256:", ignoreCase = true)) raw.substring(7).trim() else raw
                }
                .replace(":", "")
                .lowercase(Locale.US)

            if (!SHA256_HEX_REGEX.matches(cleaned)) {
                return UNAVAILABLE_FINGERPRINT
            }

            val upper = cleaned.uppercase(Locale.US)
            val firstFourBytes = (0 until 4).map { byteIndex ->
                upper.substring(byteIndex * 2, byteIndex * 2 + 2)
            }
            val lastTwoBytes = listOf(
                upper.substring(60, 62),
                upper.substring(62, 64)
            )
            return (firstFourBytes + "…" + lastTwoBytes).joinToString(":")
        }

        fun sha256BytesToHex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return encodeLowerHex(digest)
        }

        private fun normalizeSha256HexOrNull(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            val withoutPrefix = if (trimmed.startsWith("sha256:", ignoreCase = true)) {
                trimmed.substring(7).trim()
            } else {
                trimmed
            }
            val lower = withoutPrefix.lowercase(Locale.US)
            return lower.takeIf { SHA256_HEX_REGEX.matches(it) }
        }

        private fun encodeLowerHex(digest: ByteArray): String {
            val out = CharArray(digest.size * 2)
            for (i in digest.indices) {
                val value = digest[i].toInt() and 0xFF
                out[i * 2] = HEX_CHARS[value ushr 4]
                out[i * 2 + 1] = HEX_CHARS[value and 0x0F]
            }
            return String(out)
        }
    }
}
