package com.laralbir.japantraveltools.feature.updater.domain

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preReleaseIdentifiers: List<String> = emptyList(),
    val buildMetadata: String? = null
) : Comparable<SemanticVersion> {

    val isPreRelease: Boolean
        get() = preReleaseIdentifiers.isNotEmpty()

    val cleanString: String
        get() = buildString {
            append(major).append('.').append(minor).append('.').append(patch)
            if (preReleaseIdentifiers.isNotEmpty()) {
                append('-')
                append(preReleaseIdentifiers.joinToString("."))
            }
        }

    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)

        // SemVer 2.0.0 §11: normal version has higher precedence than a pre-release version
        if (preReleaseIdentifiers.isEmpty() && other.preReleaseIdentifiers.isEmpty()) return 0
        if (preReleaseIdentifiers.isEmpty()) return 1
        if (other.preReleaseIdentifiers.isEmpty()) return -1

        val maxCommon = minOf(preReleaseIdentifiers.size, other.preReleaseIdentifiers.size)
        for (i in 0 until maxCommon) {
            val a = preReleaseIdentifiers[i]
            val b = other.preReleaseIdentifiers[i]
            if (a == b) continue

            val aNumeric = a.isAsciiDigitsOnly()
            val bNumeric = b.isAsciiDigitsOnly()
            val diff = when {
                aNumeric && bNumeric -> compareUnsignedDecimalStrings(a, b)
                aNumeric -> -1 // Numeric identifiers always have lower precedence than non-numeric
                bNumeric -> 1
                else -> a.compareTo(b)
            }
            if (diff != 0) return diff
        }
        return preReleaseIdentifiers.size.compareTo(other.preReleaseIdentifiers.size)
    }

    companion object {
        private val SEMVER_REGEX = Regex(
            """^[vV]?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$"""
        )

        fun parseOrNull(raw: String): SemanticVersion? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val match = SEMVER_REGEX.matchEntire(trimmed) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            val patch = match.groupValues[3].toIntOrNull() ?: return null
            val preRelease = match.groupValues[4]
                .takeIf { it.isNotEmpty() }
                ?.split('.')
                ?: emptyList()
            val build = match.groupValues[5].takeIf { it.isNotEmpty() }
            return SemanticVersion(
                major = major,
                minor = minor,
                patch = patch,
                preReleaseIdentifiers = preRelease,
                buildMetadata = build
            )
        }

        private fun String.isAsciiDigitsOnly(): Boolean =
            isNotEmpty() && all { it in '0'..'9' }

        private fun compareUnsignedDecimalStrings(left: String, right: String): Int {
            val normalizedLeft = left.trimStart('0')
            val normalizedRight = right.trimStart('0')
            if (normalizedLeft.length != normalizedRight.length) {
                return normalizedLeft.length.compareTo(normalizedRight.length)
            }
            return normalizedLeft.compareTo(normalizedRight)
        }
    }
}

object SemanticVersionComparator : Comparator<String> {
    override fun compare(leftVersion: String, rightVersion: String): Int {
        val left = SemanticVersion.parseOrNull(leftVersion)
        val right = SemanticVersion.parseOrNull(rightVersion)
        return when {
            left != null && right != null -> left.compareTo(right)
            left != null -> 1
            right != null -> -1
            else -> 0
        }
    }

    fun isRemoteNewer(installedVersion: String, remoteTag: String): Boolean {
        val installed = SemanticVersion.parseOrNull(installedVersion) ?: return false
        val remote = SemanticVersion.parseOrNull(remoteTag) ?: return false
        return remote > installed
    }
}
