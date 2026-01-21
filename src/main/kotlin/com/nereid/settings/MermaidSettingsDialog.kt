package com.nereid.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class MermaidSettingsDialog(project: Project?) : DialogWrapper(project) {

    private val settings = MermaidSettings.getInstance()

    private var previewUpdateMode = settings.previewUpdateMode
    private var debounceDelayMs = settings.debounceDelayMs
    private var defaultViewMode = settings.defaultViewMode
    private var themeMode = settings.themeMode
    private var mermaidTheme = settings.mermaidTheme
    private var previewBackground = settings.previewBackground
    private var defaultExportFormat = settings.defaultExportFormat
    private var pngScaleFactor = settings.pngScaleFactor
    private var pngTransparentBackground = settings.pngTransparentBackground
    private var mouseWheelZoomEnabled = settings.mouseWheelZoomEnabled
    private var zoomModifierKey = settings.zoomModifierKey
    private var defaultZoomLevel = settings.defaultZoomLevel
    private var useCustomMermaidJs = settings.useCustomMermaidJs
    private var customMermaidJsUrl = settings.customMermaidJsUrl
    private var securityMode = settings.securityMode

    init {
        title = "Mermaid Settings"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            group("Editor") {
                row("Preview update:") {
                    comboBox(MermaidSettings.PreviewUpdateMode.entries)
                        .bindItem({ previewUpdateMode }, { previewUpdateMode = it ?: MermaidSettings.PreviewUpdateMode.LIVE })
                }
                row("Debounce delay:") {
                    spinner(0..2000, 50)
                        .bindIntValue({ debounceDelayMs }, { debounceDelayMs = it })
                    label("ms")
                }
                row("Default view:") {
                    comboBox(MermaidSettings.ViewMode.entries)
                        .bindItem({ defaultViewMode }, { defaultViewMode = it ?: MermaidSettings.ViewMode.SPLIT })
                }
            }

            group("Appearance") {
                row("Theme:") {
                    comboBox(MermaidSettings.ThemeMode.entries)
                        .bindItem({ themeMode }, { themeMode = it ?: MermaidSettings.ThemeMode.FOLLOW_IDE })
                }
                row("Mermaid theme:") {
                    comboBox(listOf("default", "dark", "forest", "neutral"))
                        .bindItem({ mermaidTheme }, { mermaidTheme = it ?: "default" })
                }
                row("Background:") {
                    comboBox(MermaidSettings.PreviewBackground.entries)
                        .bindItem({ previewBackground }, { previewBackground = it ?: MermaidSettings.PreviewBackground.MATCH_IDE })
                }
            }

            group("Export") {
                row("Default format:") {
                    comboBox(MermaidSettings.ExportFormat.entries)
                        .bindItem({ defaultExportFormat }, { defaultExportFormat = it ?: MermaidSettings.ExportFormat.PNG })
                }
                row("PNG scale:") {
                    comboBox(listOf(1, 2, 3))
                        .bindItem({ pngScaleFactor }, { pngScaleFactor = it ?: 2 })
                    label("x")
                }
                row {
                    checkBox("Transparent PNG background")
                        .bindSelected({ pngTransparentBackground }, { pngTransparentBackground = it })
                }
            }

            group("Zoom & Navigation") {
                row {
                    checkBox("Enable mouse wheel zoom")
                        .bindSelected({ mouseWheelZoomEnabled }, { mouseWheelZoomEnabled = it })
                }
                row("Modifier key:") {
                    comboBox(MermaidSettings.ModifierKey.entries)
                        .bindItem({ zoomModifierKey }, { zoomModifierKey = it ?: MermaidSettings.ModifierKey.CTRL })
                }
                row("Default zoom:") {
                    comboBox(MermaidSettings.ZoomLevel.entries)
                        .bindItem({ defaultZoomLevel }, { defaultZoomLevel = it ?: MermaidSettings.ZoomLevel.FIT_ALL })
                }
            }

            group("Advanced") {
                row {
                    checkBox("Use custom Mermaid.js")
                        .bindSelected({ useCustomMermaidJs }, { useCustomMermaidJs = it })
                }
                row("Custom URL:") {
                    textField()
                        .bindText({ customMermaidJsUrl }, { customMermaidJsUrl = it })
                        .columns(COLUMNS_LARGE)
                }
                row("Security:") {
                    comboBox(MermaidSettings.SecurityMode.entries)
                        .bindItem({ securityMode }, { securityMode = it ?: MermaidSettings.SecurityMode.STRICT })
                }
            }
        }
    }

    override fun doOKAction() {
        settings.previewUpdateMode = previewUpdateMode
        settings.debounceDelayMs = debounceDelayMs
        settings.defaultViewMode = defaultViewMode
        settings.themeMode = themeMode
        settings.mermaidTheme = mermaidTheme
        settings.previewBackground = previewBackground
        settings.defaultExportFormat = defaultExportFormat
        settings.pngScaleFactor = pngScaleFactor
        settings.pngTransparentBackground = pngTransparentBackground
        settings.mouseWheelZoomEnabled = mouseWheelZoomEnabled
        settings.zoomModifierKey = zoomModifierKey
        settings.defaultZoomLevel = defaultZoomLevel
        settings.useCustomMermaidJs = useCustomMermaidJs
        settings.customMermaidJsUrl = customMermaidJsUrl
        settings.securityMode = securityMode

        super.doOKAction()
    }
}
