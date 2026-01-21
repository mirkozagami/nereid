package com.nereid.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class MermaidFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val text = document.text

        // Fold subgraph blocks
        val subgraphRegex = Regex("""subgraph\s+.*?\n([\s\S]*?)\n\s*end""", RegexOption.MULTILINE)
        subgraphRegex.findAll(text).forEach { match ->
            val range = TextRange(match.range.first, match.range.last + 1)
            if (range.length > 0) {
                descriptors.add(FoldingDescriptor(root.node, range))
            }
        }

        // Fold directive blocks
        val directiveRegex = Regex("""%%\{[\s\S]*?}%%""")
        directiveRegex.findAll(text).forEach { match ->
            val range = TextRange(match.range.first, match.range.last + 1)
            if (range.length > 10) {
                descriptors.add(FoldingDescriptor(root.node, range))
            }
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        val text = node.text
        return when {
            text.startsWith("subgraph") -> "subgraph..."
            text.startsWith("%%{") -> "%%{...}%%"
            else -> "..."
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
