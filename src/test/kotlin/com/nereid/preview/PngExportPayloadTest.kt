package com.nereid.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * The PNG export payload arrives from page script, which under Loose is not necessarily
 * our page script (#52): a diagram could call `javaBridge.onPngExport(...)` with content
 * of its own and have it written to the file the user picked in the save dialog.
 *
 * The panel therefore checks the payload before handing it to the pending callback.
 * `triggerExportPng` and `triggerCopyPng` both already tested this prefix at the far end;
 * doing it at the boundary means a single place decides what a PNG export may contain,
 * and the check cannot be forgotten by a third caller.
 *
 * Rejected payloads become `error:` reports so the existing failure path surfaces them,
 * rather than being dropped -- silence is this codebase's recurring defect.
 */
class PngExportPayloadTest {

    private val validPng = MermaidPreviewPanel.PNG_DATA_URL_PREFIX +
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

    @Test
    fun testWellFormedPngDataUrlPassesThroughUnchanged() {
        assertEquals(
            "A genuine PNG export must reach the callback byte for byte",
            validPng,
            MermaidPreviewPanel.sanitizePngExportPayload(validPng)
        )
    }

    @Test
    fun testErrorReportPassesThroughUnchanged() {
        assertEquals(
            "The page reports its own failures as 'error:<reason>' and that has to survive, " +
                "or export failures go silent again",
            "error:no diagram is currently rendered",
            MermaidPreviewPanel.sanitizePngExportPayload("error:no diagram is currently rendered")
        )
    }

    @Test
    fun testEmptyPayloadPassesThroughUnchanged() {
        assertEquals(
            "exportAsPng() calls back with \"\" when the browser is not loaded, and " +
                "reportPngFailure turns that into its own message",
            "",
            MermaidPreviewPanel.sanitizePngExportPayload("")
        )
    }

    @Test
    fun testForeignPayloadIsRejected() {
        assertEquals(
            "Diagram-authored content reached the export callback. Under Loose a diagram " +
                "can call javaBridge.onPngExport itself, and this would be written to the " +
                "file the user chose (#52)",
            MermaidPreviewPanel.PNG_EXPORT_REJECTED,
            MermaidPreviewPanel.sanitizePngExportPayload("data:text/html,<script>alert(1)</script>")
        )
    }

    @Test
    fun testWrongImageTypeIsRejected() {
        assertEquals(
            "Only PNG is exported here; another image type means the payload did not come " +
                "from exportAsPng()",
            MermaidPreviewPanel.PNG_EXPORT_REJECTED,
            MermaidPreviewPanel.sanitizePngExportPayload("data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=")
        )
    }

    @Test
    fun testNonBase64BodyIsRejected() {
        assertEquals(
            "The prefix alone is not enough -- it is attacker-chosen too, so the body has " +
                "to actually be base64",
            MermaidPreviewPanel.PNG_EXPORT_REJECTED,
            MermaidPreviewPanel.sanitizePngExportPayload(
                MermaidPreviewPanel.PNG_DATA_URL_PREFIX + "<script>alert(1)</script>"
            )
        )
    }

    @Test
    fun testEmptyBase64BodyIsRejected() {
        assertEquals(
            "A prefix with nothing after it is not an image",
            MermaidPreviewPanel.PNG_EXPORT_REJECTED,
            MermaidPreviewPanel.sanitizePngExportPayload(MermaidPreviewPanel.PNG_DATA_URL_PREFIX)
        )
    }

    /**
     * The rejection is phrased as the message `reportPngFailure` would have produced for
     * an unrecognised payload anyway, so hardening the boundary does not change what a
     * user sees when an export genuinely goes wrong.
     */
    @Test
    fun testRejectionIsAnErrorReport() {
        assertEquals(
            "The rejection has to be an 'error:' report or reportPngFailure will not " +
                "recognise it and the user gets no message",
            "error:",
            MermaidPreviewPanel.PNG_EXPORT_REJECTED.take(6)
        )
    }

    /**
     * A validator nothing calls is this codebase's signature defect: it looks wired up
     * and changes nothing, and no other check we have would notice. The tests above pass
     * just as happily whether or not the export handler uses it, so the wiring itself
     * needs asserting.
     *
     * Scanned from source because `setupExportQueries` is private and needs a live JCEF
     * browser. Comments are stripped first -- the KDoc on `sanitizePngExportPayload`
     * names the function, and a search over raw text would match that prose and pass with
     * the call deleted.
     */
    @Test
    fun testThePngExportHandlerCallsTheValidator() {
        val source = withoutComments(File(projectRoot(), PANEL_SOURCE).readText())

        val handler = Regex("""pngExportQuery\.addHandler \{.*?\n {8}}""", RegexOption.DOT_MATCHES_ALL)
            .find(source)?.value

        assertNotNull("MermaidPreviewPanel no longer registers a PNG export handler", handler)
        assertTrue(
            "The PNG export handler passes the payload straight to the callback. Under " +
                "Loose a diagram can call javaBridge.onPngExport itself, so its content " +
                "would be written to the file the user chose (#52)",
            handler!!.contains("sanitizePngExportPayload")
        )
    }

    /** See the note on the test above: without this the search matches prose. */
    private fun withoutComments(source: String): String =
        source
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("""//[^\n]*"""), " ")

    private fun projectRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            if (File(dir, PANEL_SOURCE).isFile) return dir
            dir = dir.parentFile
        }
        fail("Could not locate $PANEL_SOURCE from ${File("").absolutePath}")
        error("unreachable")
    }

    private companion object {
        const val PANEL_SOURCE = "src/main/kotlin/com/nereid/preview/MermaidPreviewPanel.kt"
    }
}
