package com.nereid.language

import com.intellij.psi.tree.TokenSet

object MermaidTokenTypes {

    // Diagram types
    @JvmField val GRAPH = MermaidElementType("GRAPH")
    @JvmField val FLOWCHART = MermaidElementType("FLOWCHART")
    @JvmField val SEQUENCE_DIAGRAM = MermaidElementType("SEQUENCE_DIAGRAM")
    @JvmField val CLASS_DIAGRAM = MermaidElementType("CLASS_DIAGRAM")
    @JvmField val STATE_DIAGRAM = MermaidElementType("STATE_DIAGRAM")
    @JvmField val ER_DIAGRAM = MermaidElementType("ER_DIAGRAM")
    @JvmField val GANTT = MermaidElementType("GANTT")
    @JvmField val PIE = MermaidElementType("PIE")
    @JvmField val GITGRAPH = MermaidElementType("GITGRAPH")
    @JvmField val MINDMAP = MermaidElementType("MINDMAP")
    @JvmField val TIMELINE = MermaidElementType("TIMELINE")
    @JvmField val QUADRANT = MermaidElementType("QUADRANT")
    @JvmField val REQUIREMENT = MermaidElementType("REQUIREMENT")
    @JvmField val C4CONTEXT = MermaidElementType("C4CONTEXT")
    @JvmField val SANKEY = MermaidElementType("SANKEY")
    @JvmField val XY_CHART = MermaidElementType("XY_CHART")
    @JvmField val BLOCK = MermaidElementType("BLOCK")

    // Directions
    @JvmField val DIRECTION = MermaidElementType("DIRECTION")

    // Keywords
    @JvmField val KEYWORD = MermaidElementType("KEYWORD")
    @JvmField val SUBGRAPH = MermaidElementType("SUBGRAPH")
    @JvmField val END = MermaidElementType("END")

    // Identifiers and literals
    @JvmField val IDENTIFIER = MermaidElementType("IDENTIFIER")
    @JvmField val STRING = MermaidElementType("STRING")
    @JvmField val NUMBER = MermaidElementType("NUMBER")

    // Connectors/arrows
    @JvmField val ARROW = MermaidElementType("ARROW")
    @JvmField val LINE = MermaidElementType("LINE")

    // Brackets and delimiters
    @JvmField val LBRACKET = MermaidElementType("LBRACKET")
    @JvmField val RBRACKET = MermaidElementType("RBRACKET")
    @JvmField val LPAREN = MermaidElementType("LPAREN")
    @JvmField val RPAREN = MermaidElementType("RPAREN")
    @JvmField val LBRACE = MermaidElementType("LBRACE")
    @JvmField val RBRACE = MermaidElementType("RBRACE")
    @JvmField val PIPE = MermaidElementType("PIPE")
    @JvmField val COLON = MermaidElementType("COLON")
    @JvmField val SEMICOLON = MermaidElementType("SEMICOLON")

    // Comments and directives
    @JvmField val COMMENT = MermaidElementType("COMMENT")
    @JvmField val DIRECTIVE = MermaidElementType("DIRECTIVE")

    // Whitespace and misc
    @JvmField val WHITE_SPACE = MermaidElementType("WHITE_SPACE")
    @JvmField val NEWLINE = MermaidElementType("NEWLINE")
    @JvmField val BAD_CHARACTER = MermaidElementType("BAD_CHARACTER")

    // Token sets for syntax highlighter
    @JvmField val DIAGRAM_TYPES = TokenSet.create(
        GRAPH, FLOWCHART, SEQUENCE_DIAGRAM, CLASS_DIAGRAM, STATE_DIAGRAM,
        ER_DIAGRAM, GANTT, PIE, GITGRAPH, MINDMAP, TIMELINE, QUADRANT,
        REQUIREMENT, C4CONTEXT, SANKEY, XY_CHART, BLOCK
    )

    @JvmField val KEYWORDS = TokenSet.create(KEYWORD, SUBGRAPH, END, DIRECTION)

    @JvmField val STRINGS = TokenSet.create(STRING)

    @JvmField val COMMENTS = TokenSet.create(COMMENT)

    @JvmField val BRACKETS = TokenSet.create(
        LBRACKET, RBRACKET, LPAREN, RPAREN, LBRACE, RBRACE
    )
}
