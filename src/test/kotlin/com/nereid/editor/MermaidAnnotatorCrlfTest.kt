package com.nereid.editor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Locks the assumption that makes `MermaidAnnotator`'s offset arithmetic correct.
 *
 * The annotator reconstructs document offsets by hand:
 *
 * ```
 * offset += line.length + 1
 * ```
 *
 * with `+ 1` for the line separator. #47 argued this drifts by one character per line on
 * CRLF files, since `String.lines()` splits on `\r\n` and strips both characters while
 * the counter only advances by one -- putting highlights progressively further from the
 * character they describe.
 *
 * It does not, because the annotator never sees a `\r`. The IntelliJ platform normalises
 * every line separator to `\n` when it loads a file into a `Document`; the original
 * separator is kept as file metadata and reapplied on save. So `PsiElement.text` is
 * always LF-only regardless of what is on disk, and `+ 1` is always right.
 *
 * That normalisation is the load-bearing assumption, and nothing in the annotator states
 * it. These tests exist so that if it ever stops holding -- a platform change, or someone
 * feeding the annotator text read straight off disk -- the failure is a red test naming
 * the reason, rather than silently misplaced highlights that only reproduce on Windows.
 */
class MermaidAnnotatorCrlfTest : BasePlatformTestCase() {

    /** The exact reproduction from #47. */
    fun testUnclosedBracketHighlightLandsOnTheBracketInACrlfFile() {
        val source = "flowchart LR\r\n  A --> B\r\n  C --> D\r\n  E[unclosed --> F\r\n"

        val file = myFixture.configureByText("crlf.mmd", source)
        val expected = file.text.indexOf('[')
        assertTrue("Test fixture lost the bracket", expected > 0)

        val unclosed = myFixture.doHighlighting()
            .filter { it.severity == HighlightSeverity.ERROR }
            .filter { it.description?.contains("Unclosed") == true }

        assertEquals("Expected exactly one unclosed-bracket error", 1, unclosed.size)
        assertEquals(
            "The unclosed-bracket highlight is not on the '[' it describes. On a CRLF file " +
                "this is the #47 drift: one character per preceding line.",
            expected,
            unclosed.single().startOffset
        )
    }

    /**
     * The same diagram with LF endings must highlight at the same place. #47's claim was
     * that the two disagree, so pinning them together is the direct refutation -- and the
     * assertion that would fail first if normalisation ever stopped happening.
     */
    fun testCrlfAndLfProduceIdenticalHighlightOffsets() {
        val diagram = "flowchart LR\n  A --> B\n  C --> D\n  E[unclosed --> F\n"

        fun offsetsFor(name: String, source: String): List<Int> {
            myFixture.configureByText(name, source)
            return myFixture.doHighlighting()
                .filter { it.severity == HighlightSeverity.ERROR }
                .map(HighlightInfo::getStartOffset)
                .sorted()
        }

        val lf = offsetsFor("lf.mmd", diagram)
        val crlf = offsetsFor("crlf2.mmd", diagram.replace("\n", "\r\n"))

        assertFalse("Expected at least one error to compare", lf.isEmpty())
        assertEquals(
            "The same diagram highlights at different offsets depending on its line " +
                "endings. That is the #47 defect: the annotator's hand-rolled offset " +
                "counter assumes one character per separator.",
            lf,
            crlf
        )
    }

    /**
     * The narrower statement of the assumption, checked directly against the document
     * rather than through the annotator: whatever separators a file arrives with, the PSI
     * the annotator reads is LF-only and `line.length + 1` reproduces the document's own
     * line starts.
     */
    fun testPsiIsAlwaysLfNormalisedWhateverTheFileUsed() {
        mapOf(
            "crlf" to "a\r\nbb\r\nccc",
            "lone-cr" to "a\rbb\rccc",
            "mixed-crlf-lf" to "a\r\nbb\nccc",
            "mixed-cr-lf" to "a\rbb\nccc",
            "lf" to "a\nbb\nccc",
        ).forEach { (name, source) ->
            val file = myFixture.configureByText("$name.mmd", source)
            val document = myFixture.getDocument(file)

            assertFalse(
                "PSI for a '$name' file contains a carriage return. MermaidAnnotator's " +
                    "'offset += line.length + 1' assumes one character per separator and " +
                    "would now drift (#47).",
                file.text.contains('\r')
            )

            val handRolled = mutableListOf<Int>()
            var offset = 0
            file.text.lines().forEach { handRolled.add(offset); offset += it.length + 1 }

            val fromDocument = (0 until document.lineCount).map(document::getLineStartOffset)

            assertEquals(
                "MermaidAnnotator's offset arithmetic disagrees with the document's own " +
                    "line starts for a '$name' file",
                fromDocument,
                handRolled.take(fromDocument.size)
            )
        }
    }
}
