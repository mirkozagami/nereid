package com.nereid.markdown

import org.intellij.markdown.ast.ASTNode
import org.intellij.plugins.markdown.extensions.CodeFenceGeneratingProvider

/**
 * Generates HTML for mermaid code fences in markdown preview.
 * Outputs `<pre class="mermaid">code</pre>` which will be rendered by Mermaid.js.
 */
class MermaidCodeFenceGeneratingProvider : CodeFenceGeneratingProvider {

    override fun isApplicable(language: String): Boolean {
        return language.equals("mermaid", ignoreCase = true)
    }

    override fun generateHtml(language: String, raw: String, node: ASTNode): String {
        val escapedContent = escapeHtml(raw.trim())
        return """<pre class="mermaid">$escapedContent</pre>"""
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
