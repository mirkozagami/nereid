package com.nereid.language

import com.nereid.editor.MermaidCompletionContributor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The lexer, the annotator and completion must agree on which diagram types exist (#46).
 *
 * Three hardcoded lists drifted apart in both directions. The annotator was missing the
 * five Mermaid v11 types, so valid diagrams were flagged as invalid. Completion was
 * separately missing `C4Context`, `requirementDiagram` and `stateDiagram`, so three valid
 * types were never suggested -- which the issue did not notice, because it only compared
 * the annotator against the others.
 *
 * The annotator now reads [MermaidLexer.DIAGRAM_TYPE_NAMES] directly, so it cannot drift.
 * Completion still owns its own descriptions -- UI text does not belong in the language
 * layer -- so this test is what keeps that list honest.
 */
class DiagramTypeConsistencyTest {

    @Test
    fun testCompletionOffersExactlyTheDiagramTypesTheLanguageKnows() {
        val canonical = MermaidLexer.DIAGRAM_TYPE_NAMES
        val offered = MermaidCompletionContributor.diagramTypeNames()

        assertEquals(
            "Completion does not offer these diagram types, although the lexer and the " +
                "annotator accept them, so a user is never suggested a type that is valid",
            emptySet<String>(),
            canonical - offered
        )
        assertEquals(
            "Completion offers these diagram types, but the language does not know them, " +
                "so the annotator will flag whatever completion just inserted",
            emptySet<String>(),
            offered - canonical
        )
    }

    @Test
    fun testCompletionOffersExactlyTheDirectionsTheLanguageKnows() {
        assertEquals(
            "Completion and the lexer disagree about flowchart directions",
            MermaidLexer.DIRECTION_NAMES,
            MermaidCompletionContributor.directionNames()
        )
    }

    /**
     * Guards against the canonical set being narrowed to match a stale consumer rather
     * than the consumers being brought up to date.
     */
    @Test
    fun testTheCanonicalSetStillCoversTheVersion11Types() {
        listOf("architecture-beta", "zenuml", "packet-beta", "kanban", "journey").forEach {
            assertEquals(
                "'$it' is no longer a known diagram type",
                true,
                MermaidLexer.DIAGRAM_TYPE_NAMES.contains(it)
            )
        }
    }
}
