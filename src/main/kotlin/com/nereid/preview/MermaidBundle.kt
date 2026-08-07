package com.nereid.preview

/**
 * Metadata about the Mermaid library bundled with the plugin.
 *
 * The version is read from a resource shipped next to `mermaid.min.js` rather than
 * hardcoded, so it cannot silently drift from the artifact — which is exactly what
 * happened previously, when the plugin reported "11" while bundling 10.9.5.
 *
 * It is not queried from the library at runtime because Mermaid does not expose one:
 * neither `mermaid.version`, `mermaidAPI.version` nor `mermaidAPI.getConfig().version`
 * is defined. Reading a resource also works for **Help > Collect Mermaid Diagnostic
 * Info**, which can run with no preview open and therefore no browser to query.
 *
 * Whoever updates `mermaid.min.js` must update `mermaid-version.txt` alongside it.
 */
object MermaidBundle {

    const val UNKNOWN = "Unknown"

    private const val VERSION_RESOURCE = "/mermaid/mermaid-version.txt"

    /** The bundled Mermaid version, e.g. `10.9.5`, or [UNKNOWN] if it cannot be read. */
    val version: String by lazy { readVersion() }

    private fun readVersion(): String =
        try {
            MermaidBundle::class.java.getResourceAsStream(VERSION_RESOURCE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: UNKNOWN
        } catch (e: Exception) {
            // Diagnostics must never fail because a metadata file is unreadable.
            UNKNOWN
        }
}
