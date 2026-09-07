package com.nereid.editor

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.nereid.language.MermaidLexer
import com.nereid.language.MermaidTokenTypes
import com.nereid.language.psi.MermaidFile

/**
 * Reports invalid diagram types and unbalanced brackets.
 *
 * Both checks run off the lexer's tokens rather than scanning the text by hand. The
 * previous version did its own character walk and got three things wrong (#46):
 *
 * - it kept a private copy of the valid diagram types, which fell five behind the lexer
 * - it counted brackets inside quoted labels and trailing `%%` comments
 * - it rebuilt bracket state per line, so a `classDiagram` class body -- ordinary Mermaid
 *   -- reported an unclosed `{` on one line and an unmatched `}` on the next
 *
 * Working from tokens removes all three by construction: the types come from the lexer,
 * and a bracket inside a string or comment never becomes a bracket token in the first
 * place. It also removes the hand-rolled `offset += line.length + 1` counter, whose
 * correctness depended on a platform guarantee nothing here stated (#47).
 */
class MermaidAnnotator : Annotator {

    private data class Token(val type: IElementType, val start: Int, val end: Int)

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is MermaidFile) return

        val text = element.text
        val tokens = tokenize(text)

        annotateDiagramType(tokens, text, holder)
        annotateBrackets(tokens, text, holder)
    }

    private fun tokenize(text: String): List<Token> {
        val lexer = MermaidLexer()
        lexer.start(text, 0, text.length, 0)

        val tokens = mutableListOf<Token>()
        while (true) {
            val type = lexer.tokenType ?: break
            tokens.add(Token(type, lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        return tokens
    }

    /**
     * The first token that carries meaning -- skipping whitespace, newlines, comments and
     * `%%{init}%%` directives, any of which may legitimately precede the diagram type.
     */
    private fun firstMeaningfulToken(tokens: List<Token>): Token? =
        tokens.firstOrNull { it.type !in SKIPPED }

    private fun annotateDiagramType(tokens: List<Token>, text: String, holder: AnnotationHolder) {
        // An empty file is not a broken diagram, it is an unstarted one. Reporting on it
        // would put an error on every newly created .mmd file.
        val first = firstMeaningfulToken(tokens) ?: return
        val declared = text.substring(first.start, first.end)

        if (declared !in MermaidLexer.DIAGRAM_TYPE_NAMES) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Invalid or missing diagram type")
                .range(TextRange(first.start, first.end))
                .create()
            return
        }

        if (declared in DIRECTED_DIAGRAMS) {
            annotateDirection(text, first, holder)
        }
    }

    /**
     * Checks the direction that follows `graph` or `flowchart` on the same line.
     *
     * Read from the text after the diagram-type token rather than from the next token,
     * because an invalid direction is by definition not lexed as one, and the range has to
     * cover whatever the user actually typed.
     */
    private fun annotateDirection(text: String, diagramType: Token, holder: AnnotationHolder) {
        val lineEnd = text.indexOf('\n', diagramType.end).let { if (it < 0) text.length else it }
        val rest = text.substring(diagramType.end, lineEnd)

        val direction = rest.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        if (direction.isEmpty() || direction.startsWith("%%")) return
        if (direction in MermaidLexer.DIRECTION_NAMES) return

        val start = diagramType.end + rest.indexOf(direction)
        holder.newAnnotation(
            HighlightSeverity.WARNING,
            "Invalid direction: $direction. Expected: TB, TD, BT, RL, or LR"
        )
            .range(TextRange(start, start + direction.length))
            .create()
    }

    /**
     * Matches brackets across the whole file.
     *
     * One stack for the file, not one per line: a `classDiagram` class body opens its
     * brace on one line and closes it on another, and rebuilding state per line reported
     * both halves as errors.
     */
    private fun annotateBrackets(tokens: List<Token>, text: String, holder: AnnotationHolder) {
        val open = ArrayDeque<Token>()

        tokens.forEach { token ->
            when (token.type) {
                in OPENERS -> open.addLast(token)
                in CLOSERS -> {
                    val expected = CLOSER_FOR[open.lastOrNull()?.type]
                    if (open.isNotEmpty() && expected == token.type) {
                        open.removeLast()
                    } else {
                        val closer = text.substring(token.start, token.end)
                        holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched '$closer'")
                            .range(TextRange(token.start, token.end))
                            .create()
                    }
                }
            }
        }

        open.forEach { token ->
            val opener = text.substring(token.start, token.end)
            val expected = CLOSING_CHAR[token.type] ?: '?'
            holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed '$opener', expected '$expected'")
                .range(TextRange(token.start, token.end))
                .create()
        }
    }

    private companion object {
        /** May precede the diagram type without being it. */
        val SKIPPED = setOf(
            MermaidTokenTypes.WHITE_SPACE,
            MermaidTokenTypes.NEWLINE,
            MermaidTokenTypes.COMMENT,
            MermaidTokenTypes.DIRECTIVE,
        )

        val DIRECTED_DIAGRAMS = setOf("graph", "flowchart")

        val OPENERS = setOf(
            MermaidTokenTypes.LBRACKET,
            MermaidTokenTypes.LPAREN,
            MermaidTokenTypes.LBRACE,
        )

        val CLOSERS = setOf(
            MermaidTokenTypes.RBRACKET,
            MermaidTokenTypes.RPAREN,
            MermaidTokenTypes.RBRACE,
        )

        val CLOSER_FOR = mapOf(
            MermaidTokenTypes.LBRACKET to MermaidTokenTypes.RBRACKET,
            MermaidTokenTypes.LPAREN to MermaidTokenTypes.RPAREN,
            MermaidTokenTypes.LBRACE to MermaidTokenTypes.RBRACE,
        )

        val CLOSING_CHAR = mapOf(
            MermaidTokenTypes.LBRACKET to ']',
            MermaidTokenTypes.LPAREN to ')',
            MermaidTokenTypes.LBRACE to '}',
        )
    }
}
