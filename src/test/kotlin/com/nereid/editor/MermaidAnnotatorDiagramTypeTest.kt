package com.nereid.editor

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nereid.language.MermaidLexer

/**
 * The annotator must accept every diagram type the language knows about (#46).
 *
 * `MermaidAnnotator` kept its own hardcoded copy of the valid types and fell behind the
 * lexer, so five diagram types that render perfectly in the preview were underlined in
 * red on line 1 as "Invalid or missing diagram type". Completion would happily offer a
 * type the annotator then rejected.
 *
 * `architecture-beta` is the sharpest case: #4 was closed once the bundled Mermaid could
 * render it, but only the lexer and completion were updated, so the original reporter's
 * diagram rendered *and* was flagged as invalid at the same time.
 */
class MermaidAnnotatorDiagramTypeTest : BasePlatformTestCase() {

    private fun diagramTypeErrorsFor(type: String): List<String> {
        myFixture.configureByText("t_${type.replace('-', '_')}.mmd", "$type\n")
        return myFixture.doHighlighting()
            .filter { it.severity == HighlightSeverity.ERROR }
            .mapNotNull { it.description }
            .filter { it.contains("Invalid or missing diagram type") }
    }

    fun testEveryDiagramTypeTheLexerKnowsIsAccepted() {
        val rejected = MermaidLexer.DIAGRAM_TYPE_NAMES.filter { diagramTypeErrorsFor(it).isNotEmpty() }

        assertTrue(
            "The annotator rejects diagram types the rest of the plugin accepts. These " +
                "render correctly in the preview and are offered by completion, but are " +
                "underlined in red on line 1 (#46):\n" +
                rejected.joinToString("\n") { "  - $it" },
            rejected.isEmpty()
        )
    }

    /**
     * Named individually as well, because the set-driven test above would pass vacuously
     * if the canonical set were ever narrowed to match the annotator rather than the
     * other way round.
     */
    fun testTheFiveTypesReportedInIssue46AreAccepted() {
        listOf("architecture-beta", "zenuml", "packet-beta", "kanban", "journey").forEach { type ->
            assertEmpty(
                "'$type' is flagged as an invalid diagram type although it renders (#46)",
                diagramTypeErrorsFor(type)
            )
        }
    }

    fun testGibberishIsStillRejected() {
        assertFalse(
            "The annotator no longer flags an unknown diagram type at all, so the check " +
                "has been widened into uselessness rather than corrected",
            diagramTypeErrorsFor("notADiagramType").isEmpty()
        )
    }
}
