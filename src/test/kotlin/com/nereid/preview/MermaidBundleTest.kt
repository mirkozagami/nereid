package com.nereid.preview

import org.junit.Assert.*
import org.junit.Test

class MermaidBundleTest {

    @Test
    fun testVersionResourceIsPresent() {
        assertNotEquals(
            "mermaid-version.txt is missing or empty, so diagnostics would report an unknown version",
            MermaidBundle.UNKNOWN,
            MermaidBundle.version
        )
    }

    @Test
    fun testVersionLooksLikeSemver() {
        assertTrue(
            "Expected a semantic version, got '${MermaidBundle.version}'",
            MermaidBundle.version.matches(Regex("""\d+\.\d+\.\d+"""))
        )
    }

    /**
     * Guards against the drift this whole change exists to prevent: if someone swaps
     * mermaid.min.js without updating mermaid-version.txt, the declared version will no
     * longer appear inside the bundle and this fails.
     *
     * Mermaid embeds its own version as a string literal — verified across the official
     * 10.9.4, 10.9.5, 10.9.6, 11.12.2 and 11.16.1 distributions.
     */
    @Test
    fun testDeclaredVersionMatchesTheBundle() {
        val bundle = MermaidBundle::class.java.getResourceAsStream("/mermaid/mermaid.min.js")
            ?.bufferedReader()?.use { it.readText() }
        assertNotNull("Bundled mermaid.min.js is missing from resources", bundle)

        assertTrue(
            "mermaid-version.txt declares ${MermaidBundle.version}, but that version string " +
                "does not appear in the bundled mermaid.min.js — the two have drifted",
            bundle!!.contains("\"${MermaidBundle.version}\"")
        )
    }
}
