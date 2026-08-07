package com.nereid.spliteditor

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Guards the EDT contract on the "Report this issue" callback (issue #34).
 *
 * JCEF delivers JBCefJSQuery results on the native browser thread -- confirmed by
 * instrumentation to be "AppKit Thread" on macOS -- not the EDT. `DialogWrapper.show()`
 * calls `ensureEventDispatchThread()` and throws there, and the exception dies inside the
 * JCEF callback, so the regression is completely silent: the link just does nothing.
 *
 * Nothing else catches this. The Plugin Verifier cannot see threading contracts, and the
 * callback is only reachable through a live JCEF browser, which is unavailable in a
 * headless test. So the contract is asserted against the source itself, the same way
 * PreviewTemplateTest guards preview.html.
 */
class MermaidSplitEditorThreadingTest {

    private val relativePath = "src/main/kotlin/com/nereid/spliteditor/MermaidSplitEditor.kt"

    /**
     * Resolves the source file by walking up from the working directory, so the test does
     * not depend on which directory the runner starts in.
     */
    private fun source(): String {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relativePath)
            if (candidate.isFile) return candidate.readText()
            dir = dir.parentFile
        }
        fail("Could not locate $relativePath from ${File("").absolutePath}")
        return ""
    }

    /**
     * Returns the contents of the brace-delimited block that follows [marker].
     */
    private fun blockAfter(text: String, marker: String): String {
        val start = text.indexOf(marker)
        assertTrue("'$marker' not found in the source under test", start >= 0)

        val open = text.indexOf('{', start)
        assertTrue("No block follows '$marker'", open >= 0)

        var depth = 0
        for (i in open until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(open + 1, i)
                }
            }
        }
        fail("Unbalanced braces after '$marker'")
        return ""
    }

    @Test
    fun testReportIssueCallbackMarshalsToTheEdt() {
        val body = blockAfter(source(), "previewPanel.onReportIssue =")

        assertTrue(
            "onReportIssue must wrap its body in invokeLater. JCEF invokes it on the browser " +
                "thread, where DialogWrapper.show() throws and the swallowed exception leaves " +
                "'Report this issue' doing nothing at all (issue #34).",
            body.contains("ApplicationManager.getApplication().invokeLater")
        )
    }

    @Test
    fun testDialogAndDocumentReadHappenInsideTheEdtBlock() {
        val body = blockAfter(source(), "previewPanel.onReportIssue =")
        val onEdt = blockAfter(body, "invokeLater")

        assertTrue(
            "DiagnosticDialog.show() must run inside the invokeLater block, not merely " +
                "somewhere in the callback",
            onEdt.contains("DiagnosticDialog") && onEdt.contains(".show()")
        )
        assertTrue(
            "Reading the document text must run inside the invokeLater block too: Document " +
                "access requires a read action, which the EDT provides implicitly",
            onEdt.contains("document?.text")
        )
    }
}
