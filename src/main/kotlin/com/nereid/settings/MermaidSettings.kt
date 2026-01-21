package com.nereid.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "MermaidSettings",
    storages = [Storage("mermaid.xml")]
)
class MermaidSettings : PersistentStateComponent<MermaidSettings> {

    // Editor settings
    var previewUpdateMode: PreviewUpdateMode = PreviewUpdateMode.LIVE
    var debounceDelayMs: Int = 300
    var defaultViewMode: ViewMode = ViewMode.SPLIT
    var showLineNumbersInErrors: Boolean = true

    // Appearance settings
    var themeMode: ThemeMode = ThemeMode.FOLLOW_IDE
    var mermaidTheme: String = "default"
    var customThemeJson: String = ""
    var previewBackground: PreviewBackground = PreviewBackground.MATCH_IDE

    // Export settings
    var defaultExportFormat: ExportFormat = ExportFormat.PNG
    var pngScaleFactor: Int = 2
    var pngTransparentBackground: Boolean = true
    var svgEmbedFonts: Boolean = true
    var lastExportDirectory: String = ""

    // Zoom settings
    var mouseWheelZoomEnabled: Boolean = true
    var zoomModifierKey: ModifierKey = ModifierKey.CTRL
    var zoomSensitivity: Int = 5
    var defaultZoomLevel: ZoomLevel = ZoomLevel.FIT_ALL

    // Advanced settings
    var useCustomMermaidJs: Boolean = false
    var customMermaidJsUrl: String = ""
    var securityMode: SecurityMode = SecurityMode.STRICT
    var experimentalFeaturesEnabled: Boolean = false

    enum class PreviewUpdateMode { LIVE, ON_SAVE, MANUAL }
    enum class ViewMode { CODE_ONLY, SPLIT, PREVIEW_ONLY }
    enum class ThemeMode { FOLLOW_IDE, MERMAID_THEME, CUSTOM }
    enum class PreviewBackground { TRANSPARENT, MATCH_IDE, WHITE, DARK }
    enum class ExportFormat { PNG, SVG }
    enum class ModifierKey { CTRL, CMD, NONE }
    enum class ZoomLevel { FIT_ALL, ACTUAL_SIZE, LAST_USED }
    enum class SecurityMode { STRICT, LOOSE }

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
