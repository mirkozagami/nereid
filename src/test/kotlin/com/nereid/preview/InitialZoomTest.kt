package com.nereid.preview

import com.nereid.settings.MermaidSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * `defaultZoomLevel` offers Fit All / Actual Size / Last Used on the settings page and
 * was read by nothing but the diagnostics bundle (#39). Every preview opened fitted to
 * view because that is what the page did on load, not because anyone chose it.
 *
 * The decision is a pure function so it can be tested; applying it needs a live JCEF
 * browser, so that part is asserted against the panel source, as elsewhere in this
 * codebase.
 */
class InitialZoomTest {

    @Test
    fun testFitAllFitsRatherThanPickingAFactor() {
        assertEquals(
            InitialZoom.FitToView,
            initialZoomFor(MermaidSettings.ZoomLevel.FIT_ALL, lastZoom = 3.5)
        )
    }

    @Test
    fun testActualSizeIsAlwaysOneRegardlessOfTheLastZoom() {
        assertEquals(
            InitialZoom.Factor(1.0),
            initialZoomFor(MermaidSettings.ZoomLevel.ACTUAL_SIZE, lastZoom = 3.5)
        )
    }

    @Test
    fun testLastUsedRestoresTheRecordedZoom() {
        assertEquals(
            InitialZoom.Factor(3.5),
            initialZoomFor(MermaidSettings.ZoomLevel.LAST_USED, lastZoom = 3.5)
        )
    }

    /**
     * A zero or negative stored zoom would render the diagram invisible. The stored value
     * comes from the preview's own zoom callback, but it also round-trips through the
     * user's mermaid.xml, where it can be edited or corrupted.
     */
    @Test
    fun testAnUnusableStoredZoomFallsBackRatherThanHidingTheDiagram() {
        listOf(0.0, -1.0).forEach { bad ->
            assertEquals(
                "A stored zoom of $bad must not be applied; the diagram would vanish",
                InitialZoom.Factor(1.0),
                initialZoomFor(MermaidSettings.ZoomLevel.LAST_USED, lastZoom = bad)
            )
        }
    }

    /**
     * Applied once per page load, not per render. Re-applying on every render would
     * refit the diagram on every keystroke and fight any zoom the user had just set by
     * hand.
     */
    @Test
    fun testTheInitialZoomIsAppliedOnceAndResetWhenThePageReloads() {
        val source = panelSource()

        assertTrue(
            "MermaidPreviewPanel never applies the configured initial zoom, so " +
                "defaultZoomLevel remains decoration (#39)",
            source.contains("applyInitialZoom")
        )
        assertTrue(
            "The initial zoom is not guarded by a one-shot flag. Without it the preview " +
                "refits on every render and overrides zoom the user set by hand.",
            source.contains("initialZoomApplied")
        )
    }

    /**
     * Last Used is only meaningful if something records the zoom. The panel already
     * receives every zoom change from the browser bridge.
     */
    @Test
    fun testTheZoomCallbackRecordsTheLastZoom() {
        assertTrue(
            "Nothing records lastZoom, so the Last Used option would always restore the " +
                "default rather than the zoom the user left the preview at",
            panelSource().contains("lastZoom =")
        )
    }

    private fun panelSource(): String {
        val relative = "src/main/kotlin/com/nereid/preview/MermaidPreviewPanel.kt"
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative)
            if (candidate.isFile) return candidate.readText()
            dir = dir.parentFile
        }
        fail("Could not locate $relative from ${File("").absolutePath}")
        return ""
    }
}
