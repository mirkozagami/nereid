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

    /**
     * `mouseWheelZoomEnabled` offers a checkbox in Settings, Nereid, Preview and was one
     * of the seven Tier 2 settings in #39 -- persisted, shown, reported in diagnostics,
     * and acted on by nothing. The wheel handler zoomed regardless.
     */
    @Test
    fun testTemplateContainsTheMouseWheelZoomPlaceholder() {
        assertEquals(
            "preview.html must substitute the mouse wheel zoom flag in exactly one place, " +
                "so the wheel handler and any later reader cannot disagree (#39)",
            1,
            Regex(Regex.escape(MermaidPreviewPanel.MOUSE_WHEEL_ZOOM_PLACEHOLDER))
                .findAll(template()).count()
        )
    }

    @Test
    fun testWheelHandlerIsGatedOnTheSetting() {
        val handler = Regex(
            """addEventListener\('wheel'.*?\n        \}""",
            RegexOption.DOT_MATCHES_ALL
        ).find(template())?.value

        assertNotNull("preview.html no longer registers a wheel listener", handler)
        assertTrue(
            "The wheel listener does not consult mouseWheelZoom, so the setting is " +
                "decoration and Ctrl+wheel zooms whatever the user chose (#39)",
            handler!!.contains("mouseWheelZoom")
        )
    }

    @Test
    fun testSubstitutionCarriesTheMouseWheelZoomSetting() {
        listOf(true, false).forEach { enabled ->
            val page = MermaidPreviewPanel.buildPreviewHtml(template(), "strict", enabled)

            assertFalse(
                "Mouse wheel zoom placeholder survived substitution",
                page.contains(MermaidPreviewPanel.MOUSE_WHEEL_ZOOM_PLACEHOLDER)
            )
            assertEquals(
                "Substituted page does not carry mouseWheelZoom=$enabled",
                enabled.toString(),
                Regex("""let mouseWheelZoom = (\w+)""").find(page)?.groupValues?.get(1)
            )
        }
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
     * The custom CSS from Settings > Nereid is written into a dedicated style element.
     * It must come last in the head so user rules win over the built-in ones at equal
     * specificity -- otherwise the setting appears to do nothing for most overrides.
     */
    @Test
    fun testCustomCssElementIsLastInTheHead() {
        val html = template()

        val customCssAt = html.indexOf("""<style id="custom-css">""")
        assertTrue("preview.html has no <style id=\"custom-css\"> element", customCssAt >= 0)

        val builtInStylesAt = html.indexOf("<style>")
        assertTrue(
            "The custom CSS element must come after the built-in <style> block, or user " +
                "overrides lose on specificity ties",
            customCssAt > builtInStylesAt
        )
        assertTrue(
            "The custom CSS element must be in the <head>",
            customCssAt < html.indexOf("<body>")
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
            "window.applyCustomCss",
            "window.setMouseWheelZoom",
            "javaBridge",
        ).forEach { symbol ->
            assertTrue("preview.html no longer defines or uses '$symbol'", html.contains(symbol))
        }
    }

    /**
     * Renders are fire-and-forget from Kotlin and Mermaid's render time swings by two
     * orders of magnitude with diagram size, so a render of an older buffer can still be
     * running when the next one is dispatched (#45). Both results have to be checked
     * against the newest request id before they touch the DOM or the bridge -- the
     * success path so a stale diagram cannot overwrite a newer one, and the error path so
     * a stale failure cannot hang an error banner over a preview that has since rendered
     * fine.
     *
     * Verified in a browser against the real page; asserted here because none of that is
     * reachable from the test suite.
     */
    @Test
    fun testSupersededRendersAreDiscardedOnBothPaths() {
        val html = template()

        assertTrue(
            "preview.html no longer tracks a newest-render id, so nothing can tell a " +
                "stale render from the current one (#45)",
            html.contains("latestRenderId")
        )

        val guard = "if (renderId !== latestRenderId) return;"
        val afterMermaid = html.indexOf("await mermaid.render(")
        val paint = html.indexOf("diagram.innerHTML = svg;")
        val catchBlock = html.indexOf("} catch (e) {", afterMermaid)
        val reportError = html.indexOf("onRenderError(errorMessage)", catchBlock)

        assertTrue("preview.html no longer awaits mermaid.render()", afterMermaid >= 0)
        assertTrue("preview.html no longer writes the rendered SVG into #diagram", paint >= 0)
        assertTrue("preview.html no longer catches render failures", catchBlock >= 0)
        assertTrue("preview.html no longer reports render errors to the bridge", reportError >= 0)

        // Each guard is located within its own path -- between the await and the paint,
        // and between the catch and the bridge call -- so dropping either one is caught.
        // Asserting only that a guard exists somewhere ahead of each is not enough: the
        // success guard sits ahead of the error path too and would cover for its absence.
        assertTrue(
            "The rendered SVG is written to #diagram without first checking that this is " +
                "still the newest request. A slow render of an older buffer will overwrite " +
                "a newer one and the preview will not match the editor (#45)",
            html.lastIndexOf(guard, paint) > afterMermaid
        )
        assertTrue(
            "onRenderError is called without first checking that this is still the newest " +
                "request. A stale failure will raise an error banner over a preview that " +
                "has already rendered successfully (#45)",
            html.lastIndexOf(guard, reportError) > catchBlock
        )
    }

    /**
     * `mermaid.render()` is passed a fixed element id and begins by deleting whatever
     * already holds it, so two overlapping calls tear out each other's scratch element
     * and both report success over an empty preview. Queueing the requests is what keeps
     * the fixed id safe, so the queue and the single call site have to stay together.
     */
    @Test
    fun testRenderRequestsAreQueued() {
        val html = template()

        // Matched as code, not as the word: the prose in preview.html names renderChain
        // too, and a `contains("renderChain")` check passes on a page that only talks
        // about the queue it no longer has.
        assertTrue(
            "window.renderDiagram no longer queues each request behind the previous one. " +
                "Overlapping mermaid.render() calls share one element id, tear out each " +
                "other's scratch element and blank the preview (#45)",
            html.contains("renderChain.then(")
        )
        // The declaration is deliberately excluded. Only a reassignment moves the queue
        // forward; with just `let renderChain = Promise.resolve()` every request chains
        // off the same already-settled promise and they all overlap as before.
        assertTrue(
            "Nothing reassigns renderChain, so it never advances past its initial value " +
                "and every request runs against an already-settled promise -- the queue " +
                "is decoration and the renders overlap exactly as before (#45)",
            Regex("""(?<!let )renderChain\s*=[^=]""").containsMatchIn(html)
        )
        assertEquals(
            "preview.html must call mermaid.render() from exactly one place. A second " +
                "call site is not covered by the render queue and can overlap the first, " +
                "which collides on the shared 'mermaid-diagram' element id (#45)",
            1,
            // The element id argument is what distinguishes a call site from the
            // several places the prose above mentions mermaid.render().
            Regex("""mermaid\.render\(\s*['"]""").findAll(html).count()
        )
    }
}
