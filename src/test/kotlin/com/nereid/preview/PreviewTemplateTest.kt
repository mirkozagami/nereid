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
     * Guards the pan-blur fix (#7).
     *
     * An unconditional `transition` on #diagram makes every pan mousemove start a fresh
     * 100ms interpolation, so the diagram lags the cursor and its text is resampled at
     * fractional offsets mid-animation. Reproduced in a browser: setting the transform
     * left a running CSSTransition with the computed position still at the old value.
     *
     * The transition must therefore live behind the .animated class, which only discrete
     * zoom actions opt into.
     */
    @Test
    fun testDiagramTransitionIsOptInRatherThanAlwaysOn() {
        val html = template()

        val diagramRule = Regex("""#diagram\s*\{[^}]*}""").find(html)?.value
        assertNotNull("preview.html no longer has a #diagram rule", diagramRule)
        assertFalse(
            "#diagram has an unconditional 'transition'. That reintroduces the pan blur: " +
                "every mousemove restarts a 100ms interpolation. Put it on #diagram.animated " +
                "instead, which only discrete zoom actions enable.",
            diagramRule!!.contains("transition")
        )

        assertTrue(
            "#diagram.animated no longer defines the transition, so the zoom buttons lose " +
                "their animation",
            Regex("""#diagram\.animated\s*\{[^}]*transition[^}]*}""").containsMatchIn(html)
        )
    }

    /**
     * Guards the drag-to-select fix (#7). Before it, a single pan gesture selected all
     * 133 characters of label text in the sample diagram.
     */
    @Test
    fun testPanningCannotSelectDiagramText() {
        val html = template()

        val bodyRule = Regex("""\bbody\s*\{[^}]*}""").find(html)?.value
        assertNotNull("preview.html no longer has a body rule", bodyRule)
        assertTrue(
            "body must set 'user-select: none', or dragging to pan highlights every label " +
                "in the diagram",
            bodyRule!!.contains("user-select: none")
        )

        // Scoped to the mousedown listener specifically: the wheel handler also calls
        // preventDefault(), so a bare search of the page would pass even with the
        // mousedown guard removed.
        val mousedownHandler = Regex(
            """addEventListener\('mousedown'.*?\n        }\);""",
            RegexOption.DOT_MATCHES_ALL
        ).find(html)?.value
        assertNotNull("preview.html no longer registers a mousedown listener", mousedownHandler)
        assertTrue(
            "The mousedown listener must call preventDefault() to suppress the browser's " +
                "native drag-to-select while panning",
            mousedownHandler!!.contains("preventDefault()")
        )
    }

    /**
     * The error overlay is the one place selection must survive: users need to read and
     * copy the message, and it hosts the "Report this issue" link fixed in #34. The
     * mousedown guard deliberately skips preventDefault() inside it.
     */
    @Test
    fun testErrorOverlayStaysSelectableAndClickable() {
        val html = template()

        assertTrue(
            "#error must opt back in to 'user-select: text'; body sets none for panning, " +
                "which would otherwise make the error message unselectable",
            Regex("""#error\s*\{[^}]*user-select:\s*text[^}]*}""").containsMatchIn(html)
        )
        assertTrue(
            "The mousedown handler must exempt #error from preventDefault(), or clicks in " +
                "the error overlay stop behaving normally",
            html.contains("closest('#error')")
        )
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
