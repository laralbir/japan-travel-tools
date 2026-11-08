package com.laralbir.japantraveltools.feature.updater.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

sealed interface GitHubApiReleaseResponse {
    data class Success(
        val jsonBody: String,
        val etag: String?
    ) : GitHubApiReleaseResponse

    data object NotModified : GitHubApiReleaseResponse
}

interface GitHubReleasesApiClient {
    suspend fun fetchReleasesJson(
        includePreReleases: Boolean,
        etag: String?
    ): GitHubApiReleaseResponse

    suspend fun fetchTextFile(httpsUrl: String): String

    suspend fun downloadBinaryFile(
        httpsUrl: String,
        targetPartFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    )
}

class HttpGitHubReleasesApiClient(
    private val repoOwner: String = "laralbir",
    private val repoName: String = "japan-travel-tools"
) : GitHubReleasesApiClient {

    override suspend fun fetchReleasesJson(
        includePreReleases: Boolean,
        etag: String?
    ): GitHubApiReleaseResponse = withContext(Dispatchers.IO) {
        val endpoint = if (includePreReleases) {
            "https://api.github.com/repos/$repoOwner/$repoName/releases?per_page=10"
        } else {
            "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        }

        var connection: HttpsURLConnection? = null
        try {
            currentCoroutineContext().ensureActive()
            connection = openValidatedHttpsConnection(
                initialUrl = endpoint,
                isApiRequest = true,
                etag = etag
            )
            when (val status = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> GitHubApiReleaseResponse.NotModified
                in 200..299 -> {
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val responseEtag = connection.getHeaderField("ETag")
                    GitHubApiReleaseResponse.Success(jsonBody = body, etag = responseEtag)
                }
                else -> throw IOException("GitHub Releases API returned HTTP $status")
            }
        } finally {
            connection?.disconnect()
        }
    }

    override suspend fun fetchTextFile(httpsUrl: String): String = withContext(Dispatchers.IO) {
        var connection: HttpsURLConnection? = null
        try {
            currentCoroutineContext().ensureActive()
            connection = openValidatedHttpsConnection(
                initialUrl = httpsUrl,
                isApiRequest = false,
                etag = null
            )
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("Failed to fetch text asset: HTTP $status")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection?.disconnect()
        }
    }

    override suspend fun downloadBinaryFile(
        httpsUrl: String,
        targetPartFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        var connection: HttpsURLConnection? = null
        try {
            currentCoroutineContext().ensureActive()
            requireAllowedHttpsUrl(httpsUrl)
            targetPartFile.parentFile?.mkdirs()

            connection = openValidatedHttpsConnection(
                initialUrl = httpsUrl,
                isApiRequest = false,
                etag = null
            )
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("Failed to download binary asset: HTTP $status")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: -1L
            var downloadedBytes = 0L
            onProgress(0L, totalBytes)

            connection.inputStream.use { input ->
                FileOutputStream(targetPartFile).use { output ->
                    val buffer = ByteArray(DOWNLOAD_CHUNK_SIZE_BYTES)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        onProgress(downloadedBytes, totalBytes)
                    }
                    output.fd.sync()
                }
            }
            currentCoroutineContext().ensureActive()
        } catch (t: Throwable) {
            runCatching {
                if (targetPartFile.exists()) {
                    targetPartFile.delete()
                }
            }
            throw t
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun openValidatedHttpsConnection(
        initialUrl: String,
        isApiRequest: Boolean,
        etag: String?
    ): HttpsURLConnection {
        requireAllowedHttpsUrl(initialUrl)
        var currentUrl = initialUrl

        for (hop in 0..MAX_REDIRECT_HOPS) {
            currentCoroutineContext().ensureActive()
            val urlObj = URL(currentUrl)
            val connection = (urlObj.openConnection() as? HttpsURLConnection)
                ?: throw SecurityException("Rejected non-HTTPS connection for URL: $currentUrl")

            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)

            val isGitHubApiHost = urlObj.host.equals("api.github.com", ignoreCase = true)
            if (isApiRequest || isGitHubApiHost) {
                connection.setRequestProperty("Accept", GITHUB_ACCEPT_HEADER)
                connection.setRequestProperty("X-GitHub-Api-Version", GITHUB_API_VERSION)
                if (hop == 0 && !etag.isNullOrBlank()) {
                    connection.setRequestProperty("If-None-Match", etag)
                }
            } else {
                connection.setRequestProperty("Accept", "application/octet-stream, text/plain, */*")
            }

            val responseCode = connection.responseCode
            if (responseCode in REDIRECT_STATUS_CODES) {
                if (hop == MAX_REDIRECT_HOPS) {
                    connection.disconnect()
                    throw IOException("Exceeded maximum redirect hops ($MAX_REDIRECT_HOPS) for $initialUrl")
                }
                val rawLocation = connection.getHeaderField("Location")?.trim()
                connection.disconnect()
                if (rawLocation.isNullOrEmpty()) {
                    throw IOException("Redirect HTTP $responseCode missing Location header")
                }
                val resolvedLocation = runCatching {
                    URI(currentUrl).resolve(rawLocation).toString()
                }.getOrElse { cause ->
                    throw SecurityException("Invalid redirect Location header: $rawLocation", cause)
                }
                requireAllowedHttpsUrl(resolvedLocation)
                currentUrl = resolvedLocation
                continue
            }

            return connection
        }

        throw IOException("Failed to establish HTTPS connection for $initialUrl")
    }

    private fun requireAllowedHttpsUrl(rawUrl: String) {
        if (!isAllowedHttpsDownloadUrl(rawUrl)) {
            throw SecurityException("Blocked disallowed or non-HTTPS update URL: $rawUrl")
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MILLIS = 12_000
        private const val READ_TIMEOUT_MILLIS = 20_000
        private const val MAX_REDIRECT_HOPS = 5
        private const val DOWNLOAD_CHUNK_SIZE_BYTES = 64 * 1024
        private const val GITHUB_ACCEPT_HEADER = "application/vnd.github+json"
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val USER_AGENT = "JapanTravelTools-Android-Updater"

        private val REDIRECT_STATUS_CODES = setOf(
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_SEE_OTHER,
            307,
            308
        )

        val ALLOWED_HOSTS: Set<String> = setOf(
            "api.github.com",
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
            "codeload.github.com"
        )

        fun isAllowedHttpsDownloadUrl(rawUrl: String): Boolean {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return false
            return runCatching {
                val normalized = trimmed.replace(" ", "%20")
                val uri = URI(normalized)
                if (!uri.scheme.equals("https", ignoreCase = true)) return false
                if (uri.userInfo != null) return false
                val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return false
                if (host !in ALLOWED_HOSTS) return false
                val url = URL(normalized)
                url.protocol.equals("https", ignoreCase = true) &&
                    url.host.lowercase(Locale.ROOT).trimEnd('.') in ALLOWED_HOSTS
            }.getOrDefault(false)
        }
    }
}
