package com.nereid.editor

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.nereid.language.MermaidLanguage

class MermaidCompletionContributor : CompletionContributor() {

    companion object {
        private val DIAGRAM_TYPES = listOf(
            "graph" to "Graph diagram",
            "flowchart" to "Flowchart diagram",
            "sequenceDiagram" to "Sequence diagram",
            "classDiagram" to "Class diagram",
            "stateDiagram-v2" to "State diagram",
            "erDiagram" to "Entity-Relationship diagram",
            "gantt" to "Gantt chart",
            "pie" to "Pie chart",
            "gitGraph" to "Git graph",
            "mindmap" to "Mind map",
            "timeline" to "Timeline",
            "quadrantChart" to "Quadrant chart",
            "sankey-beta" to "Sankey diagram",
            "xychart-beta" to "XY chart",
            "block-beta" to "Block diagram"
        )

        private val DIRECTIONS = listOf(
            "TB" to "Top to Bottom",
            "TD" to "Top Down (same as TB)",
            "BT" to "Bottom to Top",
            "RL" to "Right to Left",
            "LR" to "Left to Right"
        )

        private val KEYWORDS = listOf(
            "subgraph" to "Create a subgraph",
            "end" to "End subgraph block",
            "participant" to "Declare participant",
            "actor" to "Declare actor",
            "note" to "Add a note",
            "loop" to "Loop block",
            "alt" to "Alternative block",
            "else" to "Else branch",
            "opt" to "Optional block",
            "par" to "Parallel block",
            "rect" to "Rectangle highlight",
            "section" to "Gantt section",
            "title" to "Diagram title"
        )

        private val ARROWS = listOf(
            "-->" to "Solid arrow",
            "---" to "Solid line",
            "-.->" to "Dotted arrow",
            "-.-" to "Dotted line",
            "==>" to "Thick arrow",
            "===" to "Thick line",
            "--o" to "Circle end",
            "--x" to "Cross end",
            "<-->" to "Bidirectional arrow"
        )

        private val SHAPES = listOf(
            "[text]" to "Rectangle",
            "(text)" to "Rounded rectangle",
            "([text])" to "Stadium shape",
            "[[text]]" to "Subroutine",
            "[(text)]" to "Cylinder",
            "((text))" to "Circle",
            ">text]" to "Asymmetric",
            "{text}" to "Rhombus",
            "{{text}}" to "Hexagon",
            "[/text/]" to "Parallelogram",
            "[\\text\\]" to "Parallelogram alt"
        )
    }

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(MermaidLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val text = parameters.originalFile.text
                    val offset = parameters.offset

                    // Check if we're at the start of the file or after a newline
                    val isLineStart = offset == 0 || text.getOrNull(offset - 1) == '\n'
                    val lineStart = text.lastIndexOf('\n', offset - 1) + 1
                    val linePrefix = text.substring(lineStart, offset).trim()

                    when {
                        // Diagram types at start
                        isLineStart || linePrefix.isEmpty() -> {
                            DIAGRAM_TYPES.forEach { (type, desc) ->
                                result.addElement(
                                    LookupElementBuilder.create(type)
                                        .withTypeText(desc)
                                        .bold()
                                )
                            }
                            KEYWORDS.forEach { (kw, desc) ->
                                result.addElement(
                                    LookupElementBuilder.create(kw)
                                        .withTypeText(desc)
                                )
                            }
                        }

                        // Directions after graph/flowchart
                        linePrefix.matches(Regex("(graph|flowchart)\\s*")) -> {
                            DIRECTIONS.forEach { (dir, desc) ->
                                result.addElement(
                                    LookupElementBuilder.create(dir)
                                        .withTypeText(desc)
                                        .bold()
                                )
                            }
                        }

                        // Arrows after identifier
                        linePrefix.matches(Regex(".*\\w\\s*")) -> {
                            ARROWS.forEach { (arrow, desc) ->
                                result.addElement(
                                    LookupElementBuilder.create(arrow)
                                        .withTypeText(desc)
                                )
                            }
                        }
                    }

                    // Always offer shapes as they can be useful
                    if (linePrefix.matches(Regex(".*\\w$"))) {
                        SHAPES.forEach { (shape, desc) ->
                            result.addElement(
                                LookupElementBuilder.create(shape)
                                    .withTypeText(desc)
                                    .withPresentableText(desc)
                            )
                        }
                    }
                }
            }
        )
    }
}
