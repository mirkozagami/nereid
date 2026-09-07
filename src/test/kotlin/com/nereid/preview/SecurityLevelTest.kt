package com.nereid.preview

import com.nereid.settings.MermaidSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    /**
     * The rendered SVG has to be swept for script URLs before anything can click it
     * (#52).
     *
     * `click A href "javascript:..."` puts the URL in the .mmd file. Under 'loose'
     * Mermaid passes it through to an `<a xlink:href>` around the node, and clicking runs
     * it as script in the preview -- which reaches `window.javaBridge` and, since
     * `JBCefJSQuery.inject()` emits a `window.<slot>({request: ...})` global, the Kotlin
     * message router behind it too.
     *
     * Verified in a browser against the real page; asserted here because none of it is
     * reachable from the test suite.
     */
    @Test
    fun testRenderedSvgIsSweptForScriptUrls() {
        val html = previewTemplate()

        val paint = html.indexOf("diagram.innerHTML = svg;")
        val sweep = html.indexOf("stripScriptUrls(diagram);")

        assertTrue("preview.html no longer writes the rendered SVG into #diagram", paint >= 0)
        assertTrue(
            "The rendered SVG is inserted without stripping script URLs from it. A diagram " +
                "using click ... href \"javascript:...\" then runs script in the preview (#52)",
            sweep >= 0
        )
        assertTrue(
            "stripScriptUrls() must run in the same task as the innerHTML that inserted the " +
                "SVG, or the document yields with the attribute still live (#52)",
            sweep > paint
        )
    }

    /**
     * The sweep must not consult `securityLevel`. 'strict' already drops these URLs
     * inside Mermaid, so a guard that reads the setting is absent in exactly the case it
     * exists for -- and the setting being wrong is the whole premise of #52.
     */
    @Test
    fun testScriptUrlSweepIsNotConditionalOnTheSecurityLevel() {
        val html = previewTemplate()
        val body = Regex("""function stripScriptUrls\(root\) \{.*?\n        }""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.value

        assertNotNull("preview.html no longer defines stripScriptUrls()", body)
        assertFalse(
            "stripScriptUrls() reads securityLevel, so it stops guarding as soon as the " +
                "level says it need not -- which is the situation it is for (#52)",
            body!!.contains("securityLevel")
        )
    }

    /**
     * Schemes browsers execute, matched after whitespace and control characters are
     * removed. `jav\tascript:` resolves to `javascript:` in Chrome -- confirmed in the
     * browser -- and innerHTML decodes `&#9;` into that tab before the sweep sees it, so
     * matching the raw attribute value would leave the bypass open.
     */
    @Test
    fun testScriptUrlPatternsCoverTheExecutableSchemes() {
        val html = previewTemplate()

        listOf("javascript", "vbscript").forEach { scheme ->
            assertTrue(
                "preview.html no longer rejects '$scheme:' URLs in the rendered SVG (#52)",
                Regex("""SCRIPT_URL\s*=\s*/[^/]*\b$scheme\b""").containsMatchIn(html)
            )
        }
        assertTrue(
            "Anchors must reject 'data:' as well -- a data: URL under the user's click is a " +
                "navigation to attacker-authored content (#52)",
            Regex("""ANCHOR_URL\s*=\s*/[^/]*\bdata\b""").containsMatchIn(html)
        )
        assertTrue(
            "The scheme test no longer strips whitespace and control characters first, so " +
                "'jav<tab>ascript:' gets through while still executing (#52)",
            html.contains("value.replace(/[" + "\\u0000-\\u0020" + "]/g, '')")
        )
    }

    private companion object {
        const val MARKDOWN_INIT_RESOURCE = "/mermaid/markdown-init.js"
    }
}
