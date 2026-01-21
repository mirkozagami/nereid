package com.nereid.editor

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.nereid.language.psi.MermaidFile

class MermaidAnnotator : Annotator {

    companion object {
        private val VALID_DIAGRAM_TYPES = setOf(
            "graph", "flowchart", "sequenceDiagram", "classDiagram",
            "stateDiagram", "stateDiagram-v2", "erDiagram", "gantt",
            "pie", "gitGraph", "mindmap", "timeline", "quadrantChart",
            "requirementDiagram", "C4Context", "sankey-beta", "xychart-beta", "block-beta"
        )

        private val VALID_DIRECTIONS = setOf("TB", "TD", "BT", "RL", "LR")
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is MermaidFile) return

        val text = element.text
        val lines = text.lines()

        var offset = 0
        var hasDiagramType = false

        for ((lineNum, line) in lines.withIndex()) {
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) {
                offset += line.length + 1
                continue
            }

            // Check first non-comment line for diagram type
            if (!hasDiagramType) {
                val firstWord = trimmed.split(Regex("\\s+")).firstOrNull() ?: ""

                if (firstWord !in VALID_DIAGRAM_TYPES) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Invalid or missing diagram type")
                        .range(TextRange(offset, offset + line.length.coerceAtLeast(1)))
                        .create()
                } else {
                    hasDiagramType = true

                    // Check direction for graph/flowchart
                    if (firstWord in setOf("graph", "flowchart")) {
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size >= 2) {
                            val direction = parts[1]
                            if (direction !in VALID_DIRECTIONS && !direction.startsWith("%%")) {
                                val dirStart = offset + line.indexOf(direction)
                                holder.newAnnotation(HighlightSeverity.WARNING, "Invalid direction: $direction. Expected: TB, TD, BT, RL, or LR")
                                    .range(TextRange(dirStart, dirStart + direction.length))
                                    .create()
                            }
                        }
                    }
                }
            }

            // Check for unclosed brackets in the line
            val brackets = mutableListOf<Pair<Char, Int>>()
            for ((i, c) in line.withIndex()) {
                when (c) {
                    '[', '(', '{' -> brackets.add(c to i)
                    ']' -> {
                        if (brackets.lastOrNull()?.first == '[') brackets.removeLast()
                        else holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched ']'")
                            .range(TextRange(offset + i, offset + i + 1))
                            .create()
                    }
                    ')' -> {
                        if (brackets.lastOrNull()?.first == '(') brackets.removeLast()
                        else holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched ')'")
                            .range(TextRange(offset + i, offset + i + 1))
                            .create()
                    }
                    '}' -> {
                        if (brackets.lastOrNull()?.first == '{') brackets.removeLast()
                        else holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched '}'")
                            .range(TextRange(offset + i, offset + i + 1))
                            .create()
                    }
                }
            }

            // Report unclosed brackets
            for ((bracket, pos) in brackets) {
                val expected = when (bracket) {
                    '[' -> ']'
                    '(' -> ')'
                    '{' -> '}'
                    else -> '?'
                }
                holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed '$bracket', expected '$expected'")
                    .range(TextRange(offset + pos, offset + pos + 1))
                    .create()
            }

            offset += line.length + 1
        }
    }
}
