package com.laralbir.japantraveltools.feature.updater

import com.laralbir.japantraveltools.feature.updater.security.ApkSignatureVerifier
import com.laralbir.japantraveltools.feature.updater.security.ApkVerificationResult
import com.laralbir.japantraveltools.feature.updater.security.ArchiveSignatureMetadata
import com.laralbir.japantraveltools.feature.updater.security.PackageSignatureInspector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.experimental.xor

class ApkSignatureVerifierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    companion object {
        private const val EXPECTED_PACKAGE = "com.laralbir.japantraveltools"
        private const val INSTALLED_VERSION_CODE = 36L
        private const val NEXT_VERSION_CODE = 37L

        private val PROD_CERT_A = ApkSignatureVerifier.sha256BytesToHex(
            "production-release-rsa4096-cert-primary".toByteArray(Charsets.UTF_8)
        )
        private val PROD_CERT_B = ApkSignatureVerifier.sha256BytesToHex(
            "production-release-rsa4096-cert-cosigner".toByteArray(Charsets.UTF_8)
        )
        private val MITM_OR_DEBUG_CERT = ApkSignatureVerifier.sha256BytesToHex(
            "debug-or-attacker-mitm-cert".toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun `computeFileSha256Hex matches NIST FIPS 180-4 vector and multi-chunk streaming payload`() {
        val verifier = ApkSignatureVerifier(FakePackageSignatureInspector())

        val abcFile = createApkFile("nist-abc.apk", "abc".toByteArray(Charsets.US_ASCII))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            verifier.computeFileSha256Hex(abcFile)
        )

        // 150 KB payload exercises multi-buffer streaming (> 64 KB chunks)
        val largePayload = ByteArray(150 * 1024) { index -> ((index * 31 + 7) and 0xFF).toByte() }
        val largeFile = createApkFile("large-streaming.apk", largePayload)
        assertEquals(
            ApkSignatureVerifier.sha256BytesToHex(largePayload),
            verifier.computeFileSha256Hex(largeFile)
        )
    }

    @Test
    fun `verifyApk returns ChecksumMismatch when a single byte is altered`() {
        val originalPayload = ByteArray(4096) { index -> (index and 0xFF).toByte() }
        val expectedSha256 = ApkSignatureVerifier.sha256BytesToHex(originalPayload)

        val tamperedPayload = originalPayload.copyOf()
        tamperedPayload[2048] = tamperedPayload[2048] xor 0x01
        val tamperedFile = createApkFile("tampered.apk", tamperedPayload)

        val inspector = FakePackageSignatureInspector(
            installed = defaultInstalledMetadata(),
            archive = defaultArchiveMetadata()
        )
        val verifier = ApkSignatureVerifier(inspector)

        val result = verifier.verifyApk(
            apkFile = tamperedFile,
            expectedSha256Hex = expectedSha256,
            installedVersionCode = INSTALLED_VERSION_CODE
        )

        assertTrue("Expected ChecksumMismatch but got $result", result is ApkVerificationResult.ChecksumMismatch)
        val mismatch = result as ApkVerificationResult.ChecksumMismatch
        assertEquals(expectedSha256, mismatch.expectedHex)
        assertEquals(ApkSignatureVerifier.sha256BytesToHex(tamperedPayload), mismatch.actualHex)
        assertEquals(0, inspector.archiveInspectionCalls)
    }

    @Test
    fun `verifyApk fails closed with ChecksumMismatch when expectedSha256Hex is null, blank or malformed`() {
        val payload = "signed-apk-binary-payload".toByteArray(Charsets.UTF_8)
        val apkFile = createApkFile("update.apk", payload)
        val actualSha256 = ApkSignatureVerifier.sha256BytesToHex(payload)

        val inspector = FakePackageSignatureInspector(
            installed = defaultInstalledMetadata(),
            archive = defaultArchiveMetadata()
        )
        val verifier = ApkSignatureVerifier(inspector)

        val nullResult = verifier.verifyApk(apkFile, expectedSha256Hex = null, installedVersionCode = INSTALLED_VERSION_CODE)
        assertEquals(
            ApkVerificationResult.ChecksumMismatch(expectedHex = null, actualHex = actualSha256),
            nullResult
        )

        val blankResult = verifier.verifyApk(apkFile, expectedSha256Hex = "   ", installedVersionCode = INSTALLED_VERSION_CODE)
        assertTrue(blankResult is ApkVerificationResult.ChecksumMismatch)

        val shortHexResult = verifier.verifyApk(apkFile, expectedSha256Hex = "deadbeef", installedVersionCode = INSTALLED_VERSION_CODE)
        assertTrue(shortHexResult is ApkVerificationResult.ChecksumMismatch)

        val nonHexResult = verifier.verifyApk(
            apkFile = apkFile,
            expectedSha256Hex = "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz",
            installedVersionCode = INSTALLED_VERSION_CODE
        )
        assertTrue(nonHexResult is ApkVerificationResult.ChecksumMismatch)
        assertEquals(0, inspector.archiveInspectionCalls)
    }

    @Test
    fun `verifyApk returns InvalidArchive when apkFile is missing, empty, or archive metadata is null`() {
        val verifierWithValidMeta = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = defaultInstalledMetadata(),
                archive = defaultArchiveMetadata()
            )
        )

        val missingFile = File(tempFolder.root, "non-existent-update.apk")
        val missingResult = verifierWithValidMeta.verifyApk(
            apkFile = missingFile,
            expectedSha256Hex = PROD_CERT_A,
            installedVersionCode = INSTALLED_VERSION_CODE
        )
        assertTrue("Expected InvalidArchive for missing file but got $missingResult", missingResult is ApkVerificationResult.InvalidArchive)

        val emptyFile = tempFolder.newFile("empty.apk")
        val emptySha256 = ApkSignatureVerifier.sha256BytesToHex(ByteArray(0))
        val emptyResult = verifierWithValidMeta.verifyApk(
            apkFile = emptyFile,
            expectedSha256Hex = emptySha256,
            installedVersionCode = INSTALLED_VERSION_CODE
        )
        assertTrue("Expected InvalidArchive for empty file but got $emptyResult", emptyResult is ApkVerificationResult.InvalidArchive)

        val corruptBytes = "corrupt-zip-not-an-apk".toByteArray(Charsets.UTF_8)
        val corruptFile = createApkFile("corrupt.apk", corruptBytes)
        val verifierWithNullArchive = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = defaultInstalledMetadata(),
                archive = null
            )
        )
        val nullArchiveResult = verifierWithNullArchive.verifyApk(
            apkFile = corruptFile,
            expectedSha256Hex = ApkSignatureVerifier.sha256BytesToHex(corruptBytes),
            installedVersionCode = INSTALLED_VERSION_CODE
        )
        assertTrue("Expected InvalidArchive when archive metadata is null but got $nullArchiveResult", nullArchiveResult is ApkVerificationResult.InvalidArchive)
    }

    @Test
    fun `verifyApk returns PackageNameMismatch when archive package differs from expected applicationId`() {
        val payload = "spoofed-package-apk".toByteArray(Charsets.UTF_8)
        val apkFile = createApkFile("spoofed.apk", payload)
        val verifier = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = defaultInstalledMetadata(),
                archive = ArchiveSignatureMetadata(
                    packageName = "com.malicious.fakejapantools",
                    versionCode = NEXT_VERSION_CODE,
                    certSha256Fingerprints = setOf(PROD_CERT_A)
                )
            )
        )

        val result = verifier.verifyApk(
            apkFile = apkFile,
            expectedSha256Hex = ApkSignatureVerifier.sha256BytesToHex(payload),
            installedVersionCode = INSTALLED_VERSION_CODE
        )

        assertEquals(
            ApkVerificationResult.PackageNameMismatch(
                expectedPackage = EXPECTED_PACKAGE,
                actualPackage = "com.malicious.fakejapantools"
            ),
            result
        )
    }

    @Test
    fun `verifyApk returns VersionDowngradeBlocked when archiveVersionCode is equal or lower than installedVersionCode`() {
        val payload = "downgrade-attempt-apk".toByteArray(Charsets.UTF_8)
        val apkFile = createApkFile("downgrade.apk", payload)
        val expectedSha256 = ApkSignatureVerifier.sha256BytesToHex(payload)

        val sameVersionVerifier = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = defaultInstalledMetadata(versionCode = INSTALLED_VERSION_CODE),
                archive = defaultArchiveMetadata(versionCode = INSTALLED_VERSION_CODE)
            )
        )
        assertEquals(
            ApkVerificationResult.VersionDowngradeBlocked(
                installedVersionCode = INSTALLED_VERSION_CODE,
                archiveVersionCode = INSTALLED_VERSION_CODE
            ),
            sameVersionVerifier.verifyApk(apkFile, expectedSha256, INSTALLED_VERSION_CODE)
        )

        val olderVersionVerifier = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = defaultInstalledMetadata(versionCode = INSTALLED_VERSION_CODE),
                archive = defaultArchiveMetadata(versionCode = INSTALLED_VERSION_CODE - 5L)
            )
        )
        assertEquals(
            ApkVerificationResult.VersionDowngradeBlocked(
                installedVersionCode = INSTALLED_VERSION_CODE,
                archiveVersionCode = INSTALLED_VERSION_CODE - 5L
            ),
            olderVersionVerifier.verifyApk(apkFile, expectedSha256, INSTALLED_VERSION_CODE)
        )
    }

    @Test
    fun `verifyApk returns SignatureMismatch on different certificate or multi-signer mismatch`() {
        val payload = "re-signed-mitm-apk".toByteArray(Charsets.UTF_8)
        val apkFile = createApkFile("mitm.apk", payload)
        val expectedSha256 = ApkSignatureVerifier.sha256BytesToHex(payload)

        // 1. Single signer mismatch (e.g. debug vs release or MITM certificate)
        val singleMismatchVerifier = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = defaultInstalledMetadata(certs = setOf(PROD_CERT_A)),
                archive = defaultArchiveMetadata(certs = setOf(MITM_OR_DEBUG_CERT))
            )
        )
        assertEquals(
            ApkVerificationResult.SignatureMismatch(
                installedCertSha256 = setOf(PROD_CERT_A),
                archiveCertSha256 = setOf(MITM_OR_DEBUG_CERT)
            ),
            singleMismatchVerifier.verifyApk(apkFile, expectedSha256, INSTALLED_VERSION_CODE)
        )

        // 2. Multi-signer downgrade: installed requires co-signers {A, B}, archive only has {A}
        val strippedCosignerVerifier = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = ArchiveSignatureMetadata(
                    packageName = EXPECTED_PACKAGE,
                    versionCode = INSTALLED_VERSION_CODE,
                    certSha256Fingerprints = setOf(PROD_CERT_A, PROD_CERT_B),
                    hasMultipleSigners = true
                ),
                archive = ArchiveSignatureMetadata(
                    packageName = EXPECTED_PACKAGE,
                    versionCode = NEXT_VERSION_CODE,
                    certSha256Fingerprints = setOf(PROD_CERT_A),
                    hasMultipleSigners = false
                )
            )
        )
        assertEquals(
            ApkVerificationResult.SignatureMismatch(
                installedCertSha256 = setOf(PROD_CERT_A, PROD_CERT_B),
                archiveCertSha256 = setOf(PROD_CERT_A)
            ),
            strippedCosignerVerifier.verifyApk(apkFile, expectedSha256, INSTALLED_VERSION_CODE)
        )

        // 3. Multi-signer injection: installed has {A}, archive adds extra signer {A, MITM}
        val extraSignerVerifier = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = defaultInstalledMetadata(certs = setOf(PROD_CERT_A)),
                archive = ArchiveSignatureMetadata(
                    packageName = EXPECTED_PACKAGE,
                    versionCode = NEXT_VERSION_CODE,
                    certSha256Fingerprints = setOf(PROD_CERT_A, MITM_OR_DEBUG_CERT),
                    hasMultipleSigners = true
                )
            )
        )
        assertEquals(
            ApkVerificationResult.SignatureMismatch(
                installedCertSha256 = setOf(PROD_CERT_A),
                archiveCertSha256 = setOf(PROD_CERT_A, MITM_OR_DEBUG_CERT)
            ),
            extraSignerVerifier.verifyApk(apkFile, expectedSha256, INSTALLED_VERSION_CODE)
        )

        // 4. Fail-closed when both certificate sets are empty
        val emptyCertsVerifier = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = defaultInstalledMetadata(certs = emptySet()),
                archive = defaultArchiveMetadata(certs = emptySet())
            )
        )
        assertEquals(
            ApkVerificationResult.SignatureMismatch(
                installedCertSha256 = emptySet(),
                archiveCertSha256 = emptySet()
            ),
            emptyCertsVerifier.verifyApk(apkFile, expectedSha256, INSTALLED_VERSION_CODE)
        )
    }

    @Test
    fun `verifyApk returns Verified when SHA-256, packageName, versionCode and X509 certificates match`() {
        val payload = "official-signed-release-v1.4.0-apk".toByteArray(Charsets.UTF_8)
        val apkFile = createApkFile("japan-travel-tools-v1.4.0.apk", payload)
        val expectedSha256 = ApkSignatureVerifier.sha256BytesToHex(payload)

        val singleSignerVerifier = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = defaultInstalledMetadata(),
                archive = defaultArchiveMetadata()
            )
        )

        val singleResult = singleSignerVerifier.verifyApk(
            apkFile = apkFile,
            expectedSha256Hex = expectedSha256.uppercase(),
            installedVersionCode = INSTALLED_VERSION_CODE
        )
        assertEquals(
            ApkVerificationResult.Verified(
                sha256Hex = expectedSha256,
                certSha256Hex = PROD_CERT_A,
                archiveVersionCode = NEXT_VERSION_CODE
            ),
            singleResult
        )

        // Multi-signer exact set match (order-independent) also succeeds
        val multiSignerVerifier = ApkSignatureVerifier(
            FakePackageSignatureInspector(
                installed = ArchiveSignatureMetadata(
                    packageName = EXPECTED_PACKAGE,
                    versionCode = INSTALLED_VERSION_CODE,
                    certSha256Fingerprints = setOf(PROD_CERT_A, PROD_CERT_B),
                    hasMultipleSigners = true
                ),
                archive = ArchiveSignatureMetadata(
                    packageName = EXPECTED_PACKAGE,
                    versionCode = NEXT_VERSION_CODE,
                    certSha256Fingerprints = setOf(PROD_CERT_B, PROD_CERT_A),
                    hasMultipleSigners = true
                )
            )
        )
        val multiResult = multiSignerVerifier.verifyApk(
            apkFile = apkFile,
            expectedSha256Hex = expectedSha256,
            installedVersionCode = INSTALLED_VERSION_CODE
        )
        assertTrue("Expected Verified for matching multi-signers but got $multiResult", multiResult is ApkVerificationResult.Verified)
    }

    @Test
    fun `formatShortFingerprint formats 64-char hex into uppercase colon-separated first 4 bytes ellipsis last 2 bytes`() {
        val rawHex = "abcd1234567890abcdef1234567890abcdef1234567890abcdef12345678ef90"
        assertEquals(
            "AB:CD:12:34:…:EF:90",
            ApkSignatureVerifier.formatShortFingerprint(rawHex)
        )
        assertEquals(
            "AB:CD:12:34:…:EF:90",
            ApkSignatureVerifier.formatShortFingerprint("  sha256:${rawHex.uppercase()}  ")
        )
        assertEquals("—", ApkSignatureVerifier.formatShortFingerprint(""))
        assertEquals("—", ApkSignatureVerifier.formatShortFingerprint("invalid-short-hex"))
    }

    private fun createApkFile(name: String, bytes: ByteArray): File {
        val file = tempFolder.newFile(name)
        file.writeBytes(bytes)
        return file
    }

    private fun defaultInstalledMetadata(
        versionCode: Long = INSTALLED_VERSION_CODE,
        certs: Set<String> = setOf(PROD_CERT_A)
    ) = ArchiveSignatureMetadata(
        packageName = EXPECTED_PACKAGE,
        versionCode = versionCode,
        certSha256Fingerprints = certs,
        hasMultipleSigners = false
    )

    private fun defaultArchiveMetadata(
        versionCode: Long = NEXT_VERSION_CODE,
        certs: Set<String> = setOf(PROD_CERT_A)
    ) = ArchiveSignatureMetadata(
        packageName = EXPECTED_PACKAGE,
        versionCode = versionCode,
        certSha256Fingerprints = certs,
        hasMultipleSigners = false
    )

    private class FakePackageSignatureInspector(
        private val installed: ArchiveSignatureMetadata? = null,
        private val archive: ArchiveSignatureMetadata? = null
    ) : PackageSignatureInspector {
        var archiveInspectionCalls: Int = 0
            private set

        override fun getInstalledSignatureMetadata(packageName: String): ArchiveSignatureMetadata? = installed

        override fun getArchiveSignatureMetadata(apkFile: File): ArchiveSignatureMetadata? {
            archiveInspectionCalls += 1
            return archive
        }
    }
}
