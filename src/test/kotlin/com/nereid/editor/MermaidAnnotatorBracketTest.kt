package com.nereid.editor

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Bracket balance must be judged from the lexer's tokens, not a character scan (#46).
 *
 * The old check walked each line character by character with no notion of quoting or
 * comments and no state across lines, so it reported errors on valid diagrams.
 *
 * Worth recording precisely, because #46's own two examples do *not* misfire --
 * `A["array[0]"]` and `A --> B %% see step [3]` both happen to balance, so the character
 * scan gets them right by luck. The cases that actually broke are below, and the
 * `classDiagram` one is the serious one: a class body across lines is ordinary Mermaid,
 * straight from the documentation, and it produced two errors at once.
 *
 * Brackets inside strings and comments never become bracket tokens, so all of this is
 * fixed by construction rather than by special-casing.
 */
class MermaidAnnotatorBracketTest : BasePlatformTestCase() {

    private fun bracketErrorsIn(name: String, source: String): List<String> {
        myFixture.configureByText(name, source)
        return myFixture.doHighlighting()
            .filter { it.severity == HighlightSeverity.ERROR }
            .mapNotNull { it.description }
            .filter { it.contains("Unmatched") || it.contains("Unclosed") }
    }

    fun testAClassBodySpanningLinesIsNotReported() {
        assertEmpty(
            "A classDiagram class body reports both Unclosed '{' and Unmatched '}' because " +
                "bracket state was rebuilt per line. This is ordinary Mermaid syntax (#46).",
            bracketErrorsIn("cls.mmd", "classDiagram\nclass Animal {\n  +int age\n}\n")
        )
    }

    fun testAnUnbalancedBracketInsideAQuotedLabelIsNotReported() {
        assertEmpty(
            "A '[' inside a quoted label is text, not a bracket, and must not be matched",
            bracketErrorsIn("q1.mmd", "flowchart LR\n  A[\"array[0\"] --> B\n")
        )
    }

    fun testAClosingBracketInsideAQuotedLabelIsNotReported() {
        assertEmpty(
            "A ']' inside a quoted label was reported as Unmatched",
            bracketErrorsIn("q2.mmd", "flowchart LR\n  A[\"]\"] --> B\n")
        )
    }

    fun testABracketInATrailingCommentIsNotReported() {
        assertEmpty(
            "The comment skip only applied to lines starting with %%, so a trailing " +
                "comment was scanned in full",
            bracketErrorsIn("c1.mmd", "flowchart LR\n  A --> B  %% see step [3\n")
        )
    }

    /**
     * The check still has to earn its place. Widening it until nothing is ever reported
     * would "fix" this issue while removing the feature.
     */
    fun testAGenuinelyUnclosedBracketIsStillReported() {
        val errors = bracketErrorsIn("bad1.mmd", "flowchart LR\n  A[Start --> B\n")

        assertFalse(
            "A genuinely unclosed '[' is no longer reported, so the check has been " +
                "widened into uselessness rather than corrected",
            errors.isEmpty()
        )
        assertTrue("Expected an Unclosed report, got $errors", errors.any { it.contains("Unclosed") })
    }

    fun testAGenuinelyUnmatchedCloserIsStillReported() {
        val errors = bracketErrorsIn("bad2.mmd", "flowchart LR\n  A --> B)\n")

        assertTrue(
            "A stray ')' is no longer reported. Expected an Unmatched report, got $errors",
            errors.any { it.contains("Unmatched") }
        )
    }

    /**
     * The other common multi-line brace construct. Composite states nest a block the same
     * way a class body does, so it broke for the same reason.
     */
    fun testACompositeStateSpanningLinesIsNotReported() {
        assertEmpty(
            "A composite state's block reports unbalanced braces because bracket state " +
                "was rebuilt per line (#46)",
            bracketErrorsIn(
                "state.mmd",
                "stateDiagram-v2\n  [*] --> First\n  state First {\n    [*] --> second\n  }\n"
            )
        )
    }

    fun testAValidDiagramReportsNothing() {
        assertEmpty(
            "A plain valid diagram reports bracket errors",
            bracketErrorsIn("ok.mmd", "flowchart LR\n  A[Start] --> B[End]\n  C(Round) --> D{Diamond}\n")
        )
    }
}
