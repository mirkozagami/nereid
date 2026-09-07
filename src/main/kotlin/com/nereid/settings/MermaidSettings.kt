package com.nereid.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persisted plugin settings.
 *
 * Every setting here must be read by something. Nine were removed in #39 because nothing
 * consumed them: they appeared in the settings UI, saved to the user's `mermaid.xml`, and
 * changed nothing. `MermaidSettingsAreConsumedTest` now guards that.
 *
 * Removing a property is safe on upgrade. `XmlSerializerUtil.copyBean` copies by matching
 * property, so entries left in an existing `mermaid.xml` with no counterpart here are
 * ignored rather than failing the load.
 *
 * The `name = "MermaidSettings"` state key is the storage key in users' `options/` files
 * and must not be renamed -- doing so silently resets everyone's saved settings.
 */
@State(
    name = "MermaidSettings",
    storages = [Storage("mermaid.xml")]
)
class MermaidSettings : PersistentStateComponent<MermaidSettings> {

    // Editor settings
    var previewUpdateMode: PreviewUpdateMode = PreviewUpdateMode.LIVE
    var debounceDelayMs: Int = 300
    var defaultViewMode: ViewMode = ViewMode.SPLIT

    // Appearance settings
    var themeMode: ThemeMode = ThemeMode.FOLLOW_IDE
    var mermaidTheme: String = "default"
    var previewBackground: PreviewBackground = PreviewBackground.MATCH_IDE

    // Export settings
    var defaultExportFormat: ExportFormat = ExportFormat.PNG
    var pngScaleFactor: Int = 2
    var pngTransparentBackground: Boolean = true

    // Zoom settings
    var mouseWheelZoomEnabled: Boolean = true
    var defaultZoomLevel: ZoomLevel = ZoomLevel.FIT_ALL

    /**
     * The zoom a preview was last left at, for [ZoomLevel.LAST_USED].
     *
     * Implicit state rather than a preference: it has no control, and is written by the
     * preview's zoom callback rather than by the user. It exists because Last Used cannot
     * mean anything without somewhere to remember the last value.
     */
    var lastZoom: Double = 1.0

    // Advanced settings
    /**
     * Extra CSS injected into the preview document, for overriding SVG styles Mermaid's
     * themes do not expose (stroke widths, label fonts, specific colours).
     *
     * Deliberately absent from the toolbar's settings dialog: that dialog and the
     * settings page already duplicate every other setting, which is the duplication #12
     * exists to collapse, and a multi-line editor belongs on the settings page.
     */
    var customCss: String = ""
    var securityMode: SecurityMode = SecurityMode.STRICT

    enum class PreviewUpdateMode { LIVE, ON_SAVE, MANUAL }
    enum class ViewMode { CODE_ONLY, SPLIT, PREVIEW_ONLY }
    enum class ThemeMode { FOLLOW_IDE, MERMAID_THEME, CUSTOM }
    enum class PreviewBackground { TRANSPARENT, MATCH_IDE, WHITE, DARK }
    enum class ExportFormat { PNG, SVG }
    enum class ZoomLevel { FIT_ALL, ACTUAL_SIZE, LAST_USED }
    /**
     * Mirrors Mermaid's `securityLevel` config values.
     *
     * [mermaidValue] is the wire format handed to `mermaid.initialize`, kept next to the
     * enum rather than derived with `name.lowercase()` at each call site so both render
     * paths cannot spell it differently -- which is how they came to disagree (#44).
     *
     * `loose` permits HTML in node labels and `click ... call` handlers, so it lets
     * diagram *source* execute script in the preview document. That is the point of the
     * setting, and the reason it defaults to STRICT.
     */
    enum class SecurityMode(val mermaidValue: String) {
        STRICT("strict"),
        LOOSE("loose"),
    }

    override fun getState(): MermaidSettings = this

    override fun loadState(state: MermaidSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): MermaidSettings {
            return ApplicationManager.getApplication().getService(MermaidSettings::class.java)
        }
    }
}
