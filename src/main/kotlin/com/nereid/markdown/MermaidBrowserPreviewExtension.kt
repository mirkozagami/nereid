package com.nereid.markdown

import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.ResourceProvider
import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension

/**
 * Registers Mermaid.js scripts and styles with the markdown preview panel.
 * This extension injects the necessary resources to render mermaid diagrams.
 */
class MermaidBrowserPreviewExtension(private val panel: MarkdownHtmlPanel) : MarkdownBrowserPreviewExtension {

    override val scripts: List<String>
        get() = listOf(
            MermaidMarkdownResourceProvider.RESOURCE_PREFIX + "mermaid.min.js",
            MermaidMarkdownResourceProvider.RESOURCE_PREFIX + "markdown-init.js"
        )

    override val styles: List<String>
        get() = listOf(
            MermaidMarkdownResourceProvider.RESOURCE_PREFIX + "markdown-preview.css"
        )

    override val resourceProvider: ResourceProvider
        get() = MermaidMarkdownResourceProvider()

    override fun dispose() {}

    /**
     * Provider that creates MermaidBrowserPreviewExtension instances for markdown panels.
     */
    class Provider : MarkdownBrowserPreviewExtension.Provider {
        override fun createBrowserExtension(panel: MarkdownHtmlPanel): MarkdownBrowserPreviewExtension {
            return MermaidBrowserPreviewExtension(panel)
        }
    }
}
