package com.nereid.language

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

class MermaidLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var bufferEnd: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null
    private var bracketDepth: Int = 0
    private var parenDepth: Int = 0
    private var braceDepth: Int = 0

    companion object {
        private val DIAGRAM_KEYWORDS = mapOf(
            "graph" to MermaidTokenTypes.GRAPH,
            "flowchart" to MermaidTokenTypes.FLOWCHART,
            "sequenceDiagram" to MermaidTokenTypes.SEQUENCE_DIAGRAM,
            "classDiagram" to MermaidTokenTypes.CLASS_DIAGRAM,
            "stateDiagram" to MermaidTokenTypes.STATE_DIAGRAM,
            "stateDiagram-v2" to MermaidTokenTypes.STATE_DIAGRAM,
            "erDiagram" to MermaidTokenTypes.ER_DIAGRAM,
            "gantt" to MermaidTokenTypes.GANTT,
            "pie" to MermaidTokenTypes.PIE,
            "gitGraph" to MermaidTokenTypes.GITGRAPH,
            "mindmap" to MermaidTokenTypes.MINDMAP,
            "timeline" to MermaidTokenTypes.TIMELINE,
            "quadrantChart" to MermaidTokenTypes.QUADRANT,
            "requirementDiagram" to MermaidTokenTypes.REQUIREMENT,
            "C4Context" to MermaidTokenTypes.C4CONTEXT,
            "sankey-beta" to MermaidTokenTypes.SANKEY,
            "xychart-beta" to MermaidTokenTypes.XY_CHART,
            "block-beta" to MermaidTokenTypes.BLOCK,
            // Mermaid v11 diagram types
            "zenuml" to MermaidTokenTypes.ZENUML,
            "packet-beta" to MermaidTokenTypes.PACKET,
            "kanban" to MermaidTokenTypes.KANBAN,
            "architecture-beta" to MermaidTokenTypes.ARCHITECTURE,
            "journey" to MermaidTokenTypes.JOURNEY
        )

        private val DIRECTIONS = setOf("TB", "TD", "BT", "RL", "LR")

        private val KEYWORDS = mapOf(
            "subgraph" to MermaidTokenTypes.SUBGRAPH,
            "end" to MermaidTokenTypes.END,
            "participant" to MermaidTokenTypes.KEYWORD,
            "actor" to MermaidTokenTypes.KEYWORD,
            "note" to MermaidTokenTypes.KEYWORD,
            "loop" to MermaidTokenTypes.KEYWORD,
            "alt" to MermaidTokenTypes.KEYWORD,
            "else" to MermaidTokenTypes.KEYWORD,
            "opt" to MermaidTokenTypes.KEYWORD,
            "par" to MermaidTokenTypes.KEYWORD,
            "rect" to MermaidTokenTypes.KEYWORD,
            "class" to MermaidTokenTypes.KEYWORD,
            "section" to MermaidTokenTypes.KEYWORD,
            "title" to MermaidTokenTypes.KEYWORD,
            // Mermaid v11 participant types
            "boundary" to MermaidTokenTypes.KEYWORD,
            "control" to MermaidTokenTypes.KEYWORD,
            "entity" to MermaidTokenTypes.KEYWORD,
            "database" to MermaidTokenTypes.KEYWORD,
            "collections" to MermaidTokenTypes.KEYWORD,
            "queue" to MermaidTokenTypes.KEYWORD
        )

        private val ARROWS = listOf(
            "-->", "---", "-..->", "-..-", "==>", "===",
            "--o", "--x", "o--o", "x--x", "<-->", "<-.->",
            "->", "--", "-)", "(-", "-))", "((-"
        )
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.bracketDepth = 0
        this.parenDepth = 0
        this.braceDepth = 0
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= bufferEnd) {
            tokenType = null
            return
        }

        val c = buffer[tokenStart]

        // Handle content inside brackets specially
        if (bracketDepth > 0 && c != ']') {
            lexBracketString()
            return
        }

        when {
            c == '%' && lookAhead(1) == '%' -> lexCommentOrDirective()
            c == '\n' || c == '\r' -> lexNewline()
            c.isWhitespace() -> lexWhitespace()
            c == '"' || c == '\'' -> lexQuotedString()
            c == '[' -> lexBracketContent()
            c == '(' -> lexParenContent()
            c == '{' -> lexBraceContent()
            c == ']' -> {
                singleChar(MermaidTokenTypes.RBRACKET)
                if (bracketDepth > 0) bracketDepth--
            }
            c == ')' -> singleChar(MermaidTokenTypes.RPAREN)
            c == '}' -> singleChar(MermaidTokenTypes.RBRACE)
            c == '|' -> singleChar(MermaidTokenTypes.PIPE)
            c == ':' -> singleChar(MermaidTokenTypes.COLON)
            c == ';' -> singleChar(MermaidTokenTypes.SEMICOLON)
            isArrowStart(c) -> lexArrow()
            c.isLetter() || c == '_' -> lexIdentifier()
            c.isDigit() -> lexNumber()
            else -> singleChar(MermaidTokenTypes.BAD_CHARACTER)
        }
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd

    private fun lookAhead(offset: Int): Char? {
        val pos = tokenStart + offset
        return if (pos < bufferEnd) buffer[pos] else null
    }

    private fun singleChar(type: IElementType) {
        tokenType = type
        tokenEnd = tokenStart + 1
    }

    private fun lexCommentOrDirective() {
        if (lookAhead(2) == '{') {
            // Directive: %%{...}%%
            var pos = tokenStart + 3
            while (pos < bufferEnd - 2) {
                if (buffer[pos] == '}' && buffer[pos + 1] == '%' && buffer[pos + 2] == '%') {
                    tokenEnd = pos + 3
                    tokenType = MermaidTokenTypes.DIRECTIVE
                    return
                }
                pos++
            }
            tokenEnd = bufferEnd
            tokenType = MermaidTokenTypes.DIRECTIVE
        } else {
            // Comment: %% until end of line
            var pos = tokenStart + 2
            while (pos < bufferEnd && buffer[pos] != '\n' && buffer[pos] != '\r') {
                pos++
            }
            tokenEnd = pos
            tokenType = MermaidTokenTypes.COMMENT
        }
    }

    private fun lexNewline() {
        var pos = tokenStart
        while (pos < bufferEnd && (buffer[pos] == '\n' || buffer[pos] == '\r')) {
            pos++
        }
        tokenEnd = pos
        tokenType = MermaidTokenTypes.NEWLINE
    }

    private fun lexWhitespace() {
        var pos = tokenStart
        while (pos < bufferEnd && buffer[pos].isWhitespace() && buffer[pos] != '\n' && buffer[pos] != '\r') {
            pos++
        }
        tokenEnd = pos
        tokenType = MermaidTokenTypes.WHITE_SPACE
    }

    private fun lexQuotedString() {
        val quote = buffer[tokenStart]
        var pos = tokenStart + 1
        while (pos < bufferEnd) {
            val c = buffer[pos]
            if (c == quote) {
                tokenEnd = pos + 1
                tokenType = MermaidTokenTypes.STRING
                return
            }
            if (c == '\\' && pos + 1 < bufferEnd) {
                pos += 2
            } else {
                pos++
            }
        }
        tokenEnd = bufferEnd
        tokenType = MermaidTokenTypes.STRING
    }

    private fun lexBracketContent() {
        tokenType = MermaidTokenTypes.LBRACKET
        tokenEnd = tokenStart + 1
        bracketDepth++
    }

    private fun lexBracketString() {
        // Read content until ']' as STRING
        var pos = tokenStart
        while (pos < bufferEnd && buffer[pos] != ']') {
            pos++
        }
        if (pos > tokenStart) {
            tokenEnd = pos
            tokenType = MermaidTokenTypes.STRING
        } else {
            // Empty or immediately hit ']'
            tokenEnd = tokenStart + 1
            tokenType = MermaidTokenTypes.RBRACKET
            bracketDepth--
        }
    }

    private fun lexParenContent() {
        tokenType = MermaidTokenTypes.LPAREN
        tokenEnd = tokenStart + 1
    }

    private fun lexBraceContent() {
        tokenType = MermaidTokenTypes.LBRACE
        tokenEnd = tokenStart + 1
    }

    private fun isArrowStart(c: Char): Boolean {
        return c == '-' || c == '=' || c == '.' || c == '<' || c == 'o' || c == 'x'
    }

    private fun lexArrow() {
        for (arrow in ARROWS.sortedByDescending { it.length }) {
            if (matches(arrow)) {
                tokenEnd = tokenStart + arrow.length
                tokenType = MermaidTokenTypes.ARROW
                return
            }
        }
        // Not an arrow, treat as identifier or bad char
        if (buffer[tokenStart].isLetter()) {
            lexIdentifier()
        } else {
            singleChar(MermaidTokenTypes.BAD_CHARACTER)
        }
    }

    private fun matches(s: String): Boolean {
        if (tokenStart + s.length > bufferEnd) return false
        for (i in s.indices) {
            if (buffer[tokenStart + i] != s[i]) return false
        }
        return true
    }

    private fun lexIdentifier() {
        var pos = tokenStart
        while (pos < bufferEnd) {
            val c = buffer[pos]
            if (c.isLetterOrDigit() || c == '_' || c == '-') {
                pos++
            } else {
                break
            }
        }
        tokenEnd = pos
        val text = buffer.substring(tokenStart, tokenEnd)

        tokenType = when {
            DIAGRAM_KEYWORDS.containsKey(text) -> DIAGRAM_KEYWORDS[text]
            DIRECTIONS.contains(text) -> MermaidTokenTypes.DIRECTION
            KEYWORDS.containsKey(text) -> KEYWORDS[text]
            else -> MermaidTokenTypes.IDENTIFIER
        }
    }

    private fun lexNumber() {
        var pos = tokenStart
        while (pos < bufferEnd && (buffer[pos].isDigit() || buffer[pos] == '.')) {
            pos++
        }
        tokenEnd = pos
        tokenType = MermaidTokenTypes.NUMBER
    }
}
