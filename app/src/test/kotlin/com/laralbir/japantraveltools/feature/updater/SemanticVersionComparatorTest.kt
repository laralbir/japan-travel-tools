package com.laralbir.japantraveltools.feature.updater

import com.laralbir.japantraveltools.feature.updater.domain.SemanticVersion
import com.laralbir.japantraveltools.feature.updater.domain.SemanticVersionComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticVersionComparatorTest {

    @Test
    fun `parseOrNull parses clean and prefixed versions and normalizes cleanString`() {
        val stable = SemanticVersion.parseOrNull("v1.3.0")
        assertNotNull(stable)
        assertEquals(1, stable!!.major)
        assertEquals(3, stable.minor)
        assertEquals(0, stable.patch)
        assertTrue(stable.preReleaseIdentifiers.isEmpty())
        assertNull(stable.buildMetadata)
        assertFalse(stable.isPreRelease)
        assertEquals("1.3.0", stable.cleanString)

        val upperPrefixWithBuild = SemanticVersion.parseOrNull("  V1.4.0-beta.2+20261108.sha5114f85  ")
        assertNotNull(upperPrefixWithBuild)
        assertEquals(1, upperPrefixWithBuild!!.major)
        assertEquals(4, upperPrefixWithBuild.minor)
        assertEquals(0, upperPrefixWithBuild.patch)
        assertEquals(listOf("beta", "2"), upperPrefixWithBuild.preReleaseIdentifiers)
        assertEquals("20261108.sha5114f85", upperPrefixWithBuild.buildMetadata)
        assertTrue(upperPrefixWithBuild.isPreRelease)
        assertEquals("1.4.0-beta.2", upperPrefixWithBuild.cleanString)
    }

    @Test
    fun `parseOrNull rejects malformed version strings`() {
        val malformedInputs = listOf(
            "",
            "   ",
            "v",
            "vv1.0.0",
            "1.2",
            "1.2.3.4",
            "01.2.3",
            "1.02.3",
            "1.2.03",
            "1.0.0-01",
            "1.0.0-",
            "1.0.0-beta..1",
            "1.0.0+",
            "1.0.0+build..1",
            "-1.0.0",
            "latest",
            "v1.3.x"
        )

        for (raw in malformedInputs) {
            assertNull("Expected null for malformed version '$raw'", SemanticVersion.parseOrNull(raw))
        }
    }

    @Test
    fun `compare evaluates multi digit major minor and patch numerically`() {
        assertTrue(SemanticVersionComparator.compare("1.10.0", "1.2.9") > 0)
        assertTrue(SemanticVersionComparator.compare("1.2.10", "1.2.2") > 0)
        assertTrue(SemanticVersionComparator.compare("2.0.0", "1.99.99") > 0)
        assertEquals(0, SemanticVersionComparator.compare("v1.3.0", "V1.3.0"))
        assertEquals(0, SemanticVersionComparator.compare("1.3.0", "v1.3.0"))
    }

    @Test
    fun `compare enforces strict SemVer 2_0_0 pre-release precedence rules`() {
        val betaChain = listOf(
            "v0.0.1-beta.1",
            "v0.0.1-beta.2",
            "v0.0.1-beta.10",
            "v0.0.1"
        )
        for (i in 0 until betaChain.lastIndex) {
            val lower = betaChain[i]
            val higher = betaChain[i + 1]
            assertTrue(
                "Expected $lower < $higher",
                SemanticVersionComparator.compare(lower, higher) < 0
            )
            assertTrue(
                "Expected $higher > $lower",
                SemanticVersionComparator.compare(higher, lower) > 0
            )
        }

        assertTrue(SemanticVersionComparator.compare("1.3.0", "1.4.0-beta.1") < 0)
        assertTrue(SemanticVersionComparator.compare("1.4.0-beta.1", "1.4.0") < 0)

        val canonicalChain = listOf(
            "1.0.0-alpha",
            "1.0.0-alpha.1",
            "1.0.0-alpha.beta",
            "1.0.0-beta",
            "1.0.0-beta.2",
            "1.0.0-beta.11",
            "1.0.0-rc.1",
            "1.0.0"
        )
        val shuffled = listOf(
            "1.0.0",
            "1.0.0-beta.11",
            "1.0.0-alpha.1",
            "1.0.0-rc.1",
            "1.0.0-alpha",
            "1.0.0-beta.2",
            "1.0.0-alpha.beta",
            "1.0.0-beta"
        )
        assertEquals(canonicalChain, shuffled.sortedWith(SemanticVersionComparator))

        // Hyphenated identifier "-1" is ASCII alphanumeric, not negative integer -1
        assertTrue(SemanticVersionComparator.compare("1.0.0-1", "1.0.0--1") < 0)
    }

    @Test
    fun `compare ignores build metadata per SemVer 2_0_0 section 10`() {
        assertEquals(0, SemanticVersionComparator.compare("1.4.0+20261107", "1.4.0+20261108"))
        assertEquals(0, SemanticVersionComparator.compare("v1.4.0+001", "1.4.0"))
        assertEquals(
            0,
            SemanticVersionComparator.compare("1.4.0-beta.1+exp.sha.5114f85", "v1.4.0-beta.1+other")
        )
        assertFalse(SemanticVersionComparator.isRemoteNewer("1.3.0+build.1", "v1.3.0+build.2"))
    }

    @Test
    fun `isRemoteNewer detects valid upgrades and rejects downgrades equals or malformed tags`() {
        assertTrue(SemanticVersionComparator.isRemoteNewer("1.3.0", "v1.3.1"))
        assertTrue(SemanticVersionComparator.isRemoteNewer("1.3.0", "v1.4.0-beta.1"))
        assertTrue(SemanticVersionComparator.isRemoteNewer("0.0.1-beta.4", "v0.0.1-beta.10"))
        assertTrue(SemanticVersionComparator.isRemoteNewer("0.0.1-beta.10", "v0.0.1"))

        assertFalse(SemanticVersionComparator.isRemoteNewer("1.3.0", "v1.3.0"))
        assertFalse(SemanticVersionComparator.isRemoteNewer("1.3.0", "v1.3.0-beta.9"))
        assertFalse(SemanticVersionComparator.isRemoteNewer("1.4.0", "v1.3.9"))
        assertFalse(SemanticVersionComparator.isRemoteNewer("1.3.0", "not-a-semver"))
        assertFalse(SemanticVersionComparator.isRemoteNewer("invalid-installed", "v1.4.0"))
        assertFalse(SemanticVersionComparator.isRemoteNewer("", ""))
    }

    @Test
    fun `compare orders valid versions above malformed strings deterministically`() {
        assertTrue(SemanticVersionComparator.compare("0.0.1-alpha", "malformed") > 0)
        assertTrue(SemanticVersionComparator.compare("malformed", "0.0.1-alpha") < 0)
        assertEquals(0, SemanticVersionComparator.compare("malformed-a", "malformed-b"))
    }
}
