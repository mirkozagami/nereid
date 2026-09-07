package com.nereid.preview

import com.nereid.settings.MermaidSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the defect in #44: the two render paths hardcoded *different* Mermaid
 * `securityLevel` values -- `strict` in preview.html (twice, at the initial
 * `initialize()` and again on every theme change) and `loose` in markdown-init.js --
 * while `MermaidSettings.securityMode` was read by nothing but the diagnostics bundle.
 *
 * The user-visible result: the same diagram rendered with different security in the
 * dedicated preview and the Markdown preview, and neither honoured the setting. A user
 * who chose Strict still got `loose` in Markdown previews, where `loose` permits HTML in
 * node labels and `click ... call` handlers.
 *
 * None of this is reachable by the Plugin Verifier -- these are resources and a string
 * substitution. A hardcoded level is perfectly valid JavaScript.
 *
 * The regression these tests exist to catch is someone reintroducing a literal level in
 * either resource, which is what let the two paths drift apart in the first place.
 */
class SecurityLevelTest {

    private fun resource(path: String): String =
        SecurityLevelTest::class.java.getResourceAsStream(path)
            ?.bufferedReader()?.use { it.readText() } ?: ""

    private fun previewTemplate(): String = resource(MermaidPreviewPanel.PREVIEW_TEMPLATE_RESOURCE)

    private fun markdownInitScript(): String = resource(MARKDOWN_INIT_RESOURCE)

    /** A quoted level used as the value of `securityLevel`, e.g. `securityLevel: 'strict'`. */
    private val hardcodedLevel =
        Regex("""securityLevel\s*:\s*['"](strict|loose|antiscript|sandbox)['"]""")

    @Test
    fun testPreviewTemplateHasNoHardcodedSecurityLevel() {
        val found = hardcodedLevel.find(previewTemplate())
        assertTrue(
            "preview.html hardcodes a securityLevel (${found?.value}). It must come from " +
                "MermaidSettings.securityMode via ${MermaidPreviewPanel.SECURITY_LEVEL_PLACEHOLDER}, " +
                "or the setting is decoration again and the two render paths can drift (#44).",
            found == null
        )
    }

    @Test
    fun testMarkdownInitScriptHasNoHardcodedSecurityLevel() {
        val found = hardcodedLevel.find(markdownInitScript())
        assertTrue(
            "markdown-init.js hardcodes a securityLevel (${found?.value}). This is the path " +
                "that shipped as 'loose' regardless of the user's choice (#44).",
            found == null
        )
    }

    /**
     * One declaration, not one per call site. preview.html called `mermaid.initialize`
     * twice and re-hardcoded the level in the second call, so a fix applied only to the
     * first would have been silently undone on the next theme change.
     */
    @Test
    fun testPreviewTemplateDeclaresTheSecurityLevelPlaceholderExactlyOnce() {
        val occurrences = Regex(Regex.escape(MermaidPreviewPanel.SECURITY_LEVEL_PLACEHOLDER))
            .findAll(previewTemplate()).count()

        assertEquals(
            "preview.html must substitute the security level in exactly one place and have " +
                "both mermaid.initialize() calls read that single value. Found $occurrences " +
                "occurrences; more than one lets the call sites drift apart again (#44).",
            1,
            occurrences
        )
    }

    @Test
    fun testMarkdownInitScriptUsesThePlaceholder() {
        assertTrue(
            "markdown-init.js has no ${MermaidPreviewPanel.SECURITY_LEVEL_PLACEHOLDER} token, " +
                "so the Markdown preview cannot honour the setting",
            markdownInitScript().contains(MermaidPreviewPanel.SECURITY_LEVEL_PLACEHOLDER)
        )
    }

    @Test
    fun testSecurityModeMapsToTheMermaidValues() {
        assertEquals("strict", MermaidSettings.SecurityMode.STRICT.mermaidValue)
        assertEquals("loose", MermaidSettings.SecurityMode.LOOSE.mermaidValue)
    }

    /**
     * The level each path actually ends up configured with, rather than anywhere those
     * words happen to appear. Matching raw file text also matches the explanatory
     * comments, which is enough to make these tests pass for reasons unrelated to the
     * configuration they are meant to check.
     */
    private fun previewLevelIn(page: String): String? =
        Regex("""let securityLevel = '([^']*)'""").find(page)?.groupValues?.get(1)

    private fun markdownLevelIn(script: String): String? =
        Regex("""securityLevel\s*:\s*"([^"]*)"""").find(script)?.groupValues?.get(1)

    @Test
    fun testBuildingThePreviewSubstitutesTheLevelAndLeavesNoPlaceholder() {
        val page = MermaidPreviewPanel.buildPreviewHtml(previewTemplate(), "loose", true)

        assertFalse(
            "Security level placeholder survived substitution",
            page.contains(MermaidPreviewPanel.SECURITY_LEVEL_PLACEHOLDER)
        )
        assertEquals(
            "Substituted page is not configured with the requested security level",
            "loose",
            previewLevelIn(page)
        )
    }

    /**
     * The actual #44 defect, stated directly: given one configured mode, both render
     * paths must end up on the same level. This fails if either resource reverts to a
     * literal, whichever literal it is.
     */
    @Test
    fun testBothRenderPathsResolveToTheSameLevel() {
        MermaidSettings.SecurityMode.entries.forEach { mode ->
            val preview = MermaidPreviewPanel.buildPreviewHtml(previewTemplate(), mode.mermaidValue, true)
            val markdown = markdownInitScript()
                .replace(MermaidPreviewPanel.SECURITY_LEVEL_PLACEHOLDER, mode.mermaidValue)

            assertEquals(
                "Dedicated preview is not configured with '${mode.mermaidValue}' for mode $mode",
                mode.mermaidValue,
                previewLevelIn(preview)
            )
            assertEquals(
                "Markdown preview is not configured with '${mode.mermaidValue}' for mode $mode",
                mode.mermaidValue,
                markdownLevelIn(markdown)
            )
        }
    }

    private companion object {
        const val MARKDOWN_INIT_RESOURCE = "/mermaid/markdown-init.js"
    }
}
