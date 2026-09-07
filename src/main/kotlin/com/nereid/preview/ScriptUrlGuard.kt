package com.nereid.preview

/**
 * The script-URL sweep both previews run over a rendered diagram.
 *
 * Mermaid's `click A href "..."` directive takes its URL straight from the diagram file,
 * and under `loose` it reaches the document as an `<a xlink:href>` around the node. A
 * `javascript:` URL there runs as script in the preview when the user clicks (#52), and
 * the Markdown preview renders the same way (#60).
 *
 * The JavaScript lives in one resource and is substituted into both pages rather than
 * copied into each. #44 was precisely that failure: two render paths holding their own
 * copy of a security decision, which drifted until the same diagram rendered with
 * different security in each preview. A guard duplicated across the two files would have
 * recreated it.
 *
 * Substituted verbatim, so both pages end up carrying byte-identical source and a test
 * can assert as much. That leaves the block at its own indentation inside each page,
 * which is the same trade `MermaidBundle.script` already makes.
 *
 * Empty if the resource cannot be read, which leaves `stripScriptUrls` undefined and
 * every render failing loudly rather than quietly rendering unguarded. Tests assert both
 * pages carry it, since a resource that vanished from the jar is not something the
 * running plugin can do anything about.
 */
object ScriptUrlGuard {

    /** Replaced with [source] in both previews before either is handed to a browser. */
    const val PLACEHOLDER = "__SCRIPT_URL_GUARD__"

    private const val RESOURCE = "/mermaid/script-urls.js"

    /** The shared guard source, read once and cached. */
    val source: String by lazy { read() }

    private fun read(): String =
        try {
            ScriptUrlGuard::class.java.getResourceAsStream(RESOURCE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: ""
        } catch (e: Exception) {
            ""
        }
}
