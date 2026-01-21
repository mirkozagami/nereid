package com.nereid.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.nereid.language.MermaidTokenTypes

class MermaidSyntaxHighlighterTest : BasePlatformTestCase() {

    fun testHighlighterReturnsCorrectAttributesForDiagramType() {
        val highlighter = MermaidSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(MermaidTokenTypes.GRAPH)
        assertTrue(attributes.isNotEmpty())
        assertEquals(MermaidSyntaxHighlighter.DIAGRAM_TYPE, attributes[0])
    }

    fun testHighlighterReturnsCorrectAttributesForKeyword() {
        val highlighter = MermaidSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(MermaidTokenTypes.KEYWORD)
        assertTrue(attributes.isNotEmpty())
        assertEquals(MermaidSyntaxHighlighter.KEYWORD, attributes[0])
    }

    fun testHighlighterReturnsCorrectAttributesForComment() {
        val highlighter = MermaidSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(MermaidTokenTypes.COMMENT)
        assertTrue(attributes.isNotEmpty())
        assertEquals(MermaidSyntaxHighlighter.COMMENT, attributes[0])
    }
}
