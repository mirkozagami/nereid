package com.nereid.preview

import org.junit.Assert.*
import org.junit.Test

/**
 * Covers the preview page template and the substitution that injects the Mermaid
 * library into it.
 *
 * None of this is reachable by the Plugin Verifier — it is a resource and a string
 * replacement — and a failure here leaves users with a blank preview and no error.
 */
class PreviewTemplateTest {

    private fun template(): String =
        PreviewTemplateTest::class.java
            .getResourceAsStream(MermaidPreviewPanel.PREVIEW_TEMPLATE_RESOURCE)
            ?.bufferedReader()?.use { it.readText() } ?: ""

    @Test
    fun testTemplateResourceLoads() {
        assertTrue(
            "preview.html is missing from resources; the preview would be blank",
            template().length > 1000
        )
    }

    @Test
    fun testTemplateContainsThePlaceholder() {
        assertTrue(
            "preview.html has no ${MermaidPreviewPanel.MERMAID_LIBRARY_PLACEHOLDER} token, " +
                "so the Mermaid library would never be injected and nothing would render",
            template().contains(MermaidPreviewPanel.MERMAID_LIBRARY_PLACEHOLDER)
        )
    }

    @Test
    fun testSubstitutionInjectsTheLibraryAndLeavesNoPlaceholder() {
        val page = template().replace(
            MermaidPreviewPanel.MERMAID_LIBRARY_PLACEHOLDER,
            MermaidBundle.script
        )

        assertFalse(
            "Placeholder survived substitution",
            page.contains(MermaidPreviewPanel.MERMAID_LIBRARY_PLACEHOLDER)
        )
        assertTrue(
            "Substituted page does not contain the bundled Mermaid version",
            page.contains("\"${MermaidBundle.version}\"")
        )
        assertTrue("Substituted page is implausibly small", page.length > 3_000_000)
    }

    /**
     * The page is useless without the entry points the Kotlin side calls into and the
     * bridge callbacks it listens for. Guards against an edit dropping one silently.
     */
    @Test
    fun testTemplateKeepsTheJavaScriptContract() {
        val html = template()
        listOf(
            "window.renderDiagram",
            "window.exportAsPng",
            "window.exportAsSvg",
            "window.setZoom",
            "window.fitToView",
            "window.resetView",
            "javaBridge",
        ).forEach { symbol ->
            assertTrue("preview.html no longer defines or uses '$symbol'", html.contains(symbol))
        }
    }
}
