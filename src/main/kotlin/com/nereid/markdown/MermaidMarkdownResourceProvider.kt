package com.nereid.markdown

import org.intellij.plugins.markdown.ui.preview.ResourceProvider

/**
 * Serves bundled Mermaid resources (JS, CSS) through the markdown preview's resource system.
 * This allows the resources to be loaded within the markdown preview's CSP security model.
 */
class MermaidMarkdownResourceProvider : ResourceProvider {

    override fun canProvide(resourceName: String): Boolean {
        return resourceName.startsWith(RESOURCE_PREFIX)
    }

    override fun loadResource(resourceName: String): ResourceProvider.Resource? {
        val actualPath = resourceName.removePrefix(RESOURCE_PREFIX)
        val resourcePath = "/mermaid/$actualPath"

        val stream = javaClass.getResourceAsStream(resourcePath) ?: return null
        val content = stream.use { it.readBytes() }

        val mimeType = when {
            actualPath.endsWith(".js") -> "application/javascript; charset=utf-8"
            actualPath.endsWith(".css") -> "text/css; charset=utf-8"
            else -> "application/octet-stream"
        }

        return ResourceProvider.Resource(content, mimeType)
    }

    companion object {
        const val RESOURCE_PREFIX = "mermaid/"
    }
}
