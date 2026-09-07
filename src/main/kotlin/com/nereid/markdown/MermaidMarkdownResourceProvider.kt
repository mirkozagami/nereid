package com.nereid.markdown

import com.nereid.preview.MermaidPreviewPanel
import com.nereid.settings.MermaidSettings
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
        val raw = stream.use { it.readBytes() }

        val mimeType = when {
            actualPath.endsWith(".js") -> "application/javascript; charset=utf-8"
            actualPath.endsWith(".css") -> "text/css; charset=utf-8"
            else -> "application/octet-stream"
        }

        // Only the init script carries placeholders. Scoped by name rather than by ".js"
        // so the 2 MB Mermaid bundle is not decoded and re-encoded on every preview load.
        val content = if (actualPath == INIT_SCRIPT) substituteSecurityLevel(raw) else raw

        return ResourceProvider.Resource(content, mimeType)
    }

    /**
     * Substitutes the user's security mode into the init script as it is served.
     *
     * The Markdown preview is the platform's, and it takes this script from here at load
     * time -- there is no live channel into it the way there is for the dedicated
     * preview, so a change reaches an already-open Markdown preview only when it
     * reloads. Tracked in #51.
     */
    private fun substituteSecurityLevel(raw: ByteArray): ByteArray {
        val level = MermaidSettings.getInstance().securityMode.mermaidValue
        return String(raw, Charsets.UTF_8)
            .replace(MermaidPreviewPanel.SECURITY_LEVEL_PLACEHOLDER, level)
            .toByteArray(Charsets.UTF_8)
    }

    companion object {
        const val RESOURCE_PREFIX = "mermaid/"

        private const val INIT_SCRIPT = "markdown-init.js"
    }
}
