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

    @Test
    fun testScriptResourceLoads() {
        assertTrue(
            "Bundled mermaid.min.js failed to load; the preview would render nothing",
            MermaidBundle.script.length > 100_000
        )
    }

    @Test
    fun testScriptMatchesDeclaredVersion() {
        assertTrue(
            "Loaded script does not carry the declared version ${MermaidBundle.version}",
            MermaidBundle.script.contains("\"${MermaidBundle.version}\"")
        )
    }

    /**
     * The preview inlines the library inside a `<script>` element. A literal `</script>`
     * anywhere in the source would close that element early and break the whole preview,
     * so this guards the assumption that inlining is safe for the bundled file.
     */
    @Test
    fun testScriptIsSafeToInline() {
        assertFalse(
            "Bundled Mermaid contains a literal </script>, which would terminate the " +
                "inline <script> element early — it must be escaped before inlining",
            MermaidBundle.script.contains("</script", ignoreCase = true)
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
