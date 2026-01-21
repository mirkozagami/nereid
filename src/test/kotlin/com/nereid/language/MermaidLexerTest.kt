package com.nereid.language

import com.intellij.testFramework.LexerTestCase

class MermaidLexerTest : LexerTestCase() {

    override fun createLexer() = MermaidLexer()

    override fun getDirPath() = "src/test/resources/lexer"

    fun testDiagramType() {
        doTest(
            "graph TD",
            """
            GRAPH ('graph')
            WHITE_SPACE (' ')
            DIRECTION ('TD')
            """.trimIndent()
        )
    }

    fun testFlowchartWithNodes() {
        doTest(
            "flowchart LR\n    A --> B",
            """
            FLOWCHART ('flowchart')
            WHITE_SPACE (' ')
            DIRECTION ('LR')
            NEWLINE ('\n')
            WHITE_SPACE ('    ')
            IDENTIFIER ('A')
            WHITE_SPACE (' ')
            ARROW ('-->')
            WHITE_SPACE (' ')
            IDENTIFIER ('B')
            """.trimIndent()
        )
    }

    fun testComment() {
        doTest(
            "%% This is a comment",
            """
            COMMENT ('%% This is a comment')
            """.trimIndent()
        )
    }

    fun testDirective() {
        doTest(
            "%%{init: {'theme': 'dark'}}%%",
            """
            DIRECTIVE ('%%{init: {'theme': 'dark'}}%%')
            """.trimIndent()
        )
    }

    fun testNodeWithLabel() {
        doTest(
            "A[Hello World]",
            """
            IDENTIFIER ('A')
            LBRACKET ('[')
            STRING ('Hello World')
            RBRACKET (']')
            """.trimIndent()
        )
    }
}
