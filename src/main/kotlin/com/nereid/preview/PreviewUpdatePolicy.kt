package com.nereid.preview

import com.nereid.settings.MermaidSettings

/** What prompted a possible preview render. */
internal enum class RenderTrigger { DOCUMENT_CHANGE, SAVE, MANUAL_REFRESH }

/**
 * Whether [trigger] should re-render the preview under [mode].
 *
 * `previewUpdateMode` was read by nothing but the diagnostics bundle: the preview
 * re-rendered on a debounce after every keystroke regardless, so On Save and Manual did
 * nothing (#39).
 *
 * A manual refresh renders in every mode -- it is a direct instruction, and Manual would
 * otherwise have no way to update at all. Live deliberately does *not* also render on
 * save: the document change already did, and rendering again would redo the work for an
 * already-current diagram.
 */
internal fun shouldRender(
    trigger: RenderTrigger,
    mode: MermaidSettings.PreviewUpdateMode,
): Boolean = when (trigger) {
    RenderTrigger.MANUAL_REFRESH -> true
    RenderTrigger.DOCUMENT_CHANGE -> mode == MermaidSettings.PreviewUpdateMode.LIVE
    RenderTrigger.SAVE -> mode == MermaidSettings.PreviewUpdateMode.ON_SAVE
}
