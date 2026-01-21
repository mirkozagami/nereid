package com.nereid.editor

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.nereid.language.MermaidLexer
import com.nereid.language.MermaidTokenTypes

class MermaidSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        @JvmField
        val DIAGRAM_TYPE = createTextAttributesKey(
            "MERMAID_DIAGRAM_TYPE",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        @JvmField
        val KEYWORD = createTextAttributesKey(
            "MERMAID_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        @JvmField
        val DIRECTION = createTextAttributesKey(
            "MERMAID_DIRECTION",
            DefaultLanguageHighlighterColors.CONSTANT
        )

        @JvmField
        val IDENTIFIER = createTextAttributesKey(
            "MERMAID_IDENTIFIER",
            DefaultLanguageHighlighterColors.IDENTIFIER
        )

        @JvmField
        val STRING = createTextAttributesKey(
            "MERMAID_STRING",
            DefaultLanguageHighlighterColors.STRING
        )

        @JvmField
        val NUMBER = createTextAttributesKey(
            "MERMAID_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER
        )

        @JvmField
        val ARROW = createTextAttributesKey(
            "MERMAID_ARROW",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )

        @JvmField
        val BRACKETS = createTextAttributesKey(
            "MERMAID_BRACKETS",
            DefaultLanguageHighlighterColors.BRACKETS
        )

        @JvmField
        val COMMENT = createTextAttributesKey(
            "MERMAID_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )

        @JvmField
        val DIRECTIVE = createTextAttributesKey(
            "MERMAID_DIRECTIVE",
            DefaultLanguageHighlighterColors.METADATA
        )

        @JvmField
        val BAD_CHARACTER = createTextAttributesKey(
            "MERMAID_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )

        private val DIAGRAM_TYPE_KEYS = arrayOf(DIAGRAM_TYPE)
        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val DIRECTION_KEYS = arrayOf(DIRECTION)
        private val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
        private val STRING_KEYS = arrayOf(STRING)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val ARROW_KEYS = arrayOf(ARROW)
        private val BRACKETS_KEYS = arrayOf(BRACKETS)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val DIRECTIVE_KEYS = arrayOf(DIRECTIVE)
        private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer = MermaidLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            in MermaidTokenTypes.DIAGRAM_TYPES.types -> DIAGRAM_TYPE_KEYS

            MermaidTokenTypes.KEYWORD,
            MermaidTokenTypes.SUBGRAPH,
            MermaidTokenTypes.END -> KEYWORD_KEYS

            MermaidTokenTypes.DIRECTION -> DIRECTION_KEYS

            MermaidTokenTypes.IDENTIFIER -> IDENTIFIER_KEYS

            MermaidTokenTypes.STRING -> STRING_KEYS

            MermaidTokenTypes.NUMBER -> NUMBER_KEYS

            MermaidTokenTypes.ARROW,
            MermaidTokenTypes.LINE -> ARROW_KEYS

            MermaidTokenTypes.LBRACKET,
            MermaidTokenTypes.RBRACKET,
            MermaidTokenTypes.LPAREN,
            MermaidTokenTypes.RPAREN,
            MermaidTokenTypes.LBRACE,
            MermaidTokenTypes.RBRACE -> BRACKETS_KEYS

            MermaidTokenTypes.COMMENT -> COMMENT_KEYS

            MermaidTokenTypes.DIRECTIVE -> DIRECTIVE_KEYS

            MermaidTokenTypes.BAD_CHARACTER -> BAD_CHARACTER_KEYS

            else -> EMPTY_KEYS
        }
    }
}
