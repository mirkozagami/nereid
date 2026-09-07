package com.nereid.preview

import com.nereid.settings.MermaidSettings

/**
 * What to do with the zoom when a preview first renders.
 *
 * Fitting is not expressible as a zoom factor -- it depends on the rendered diagram's
 * size, which only the page knows -- so it is a separate case rather than a magic number.
 */
internal sealed interface InitialZoom {
    data object FitToView : InitialZoom
    data class Factor(val value: Double) : InitialZoom
}

/**
 * Resolves `defaultZoomLevel` against the zoom the user last left a preview at.
 *
 * Pure, so the choice is testable without a JCEF browser; [MermaidPreviewPanel] owns the
 * applying. Previously nothing read the setting at all and every preview fitted to view
 * because that is what the page happened to do on load (#39).
 *
 * A non-positive [lastZoom] is treated as unusable and falls back to actual size. The
 * value round-trips through the user's mermaid.xml, so it can arrive corrupted or
 * hand-edited, and applying it would leave an invisible diagram and no clue why.
 */
internal fun initialZoomFor(level: MermaidSettings.ZoomLevel, lastZoom: Double): InitialZoom =
    when (level) {
        MermaidSettings.ZoomLevel.FIT_ALL -> InitialZoom.FitToView
        MermaidSettings.ZoomLevel.ACTUAL_SIZE -> InitialZoom.Factor(1.0)
        MermaidSettings.ZoomLevel.LAST_USED ->
            InitialZoom.Factor(if (lastZoom > 0.0) lastZoom else 1.0)
    }
