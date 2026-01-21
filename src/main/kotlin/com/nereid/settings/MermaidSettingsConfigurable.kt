package com.nereid.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class MermaidSettingsConfigurable : Configurable {

    private var panel: JComponent? = null
    private val settings = MermaidSettings.getInstance()

    override fun getDisplayName(): String = "Mermaid"

    override fun createComponent(): JComponent {
        panel = panel {
            group("Editor") {
                row("Preview update:") {
                    comboBox(MermaidSettings.PreviewUpdateMode.entries)
                        .bindItem(settings::previewUpdateMode.toNullableProperty())
                }
                row("Debounce delay:") {
                    slider(0, 2000, 100, 500)
                        .bindValue(settings::debounceDelayMs)
                    label("ms")
                }
                row("Default view:") {
                    comboBox(MermaidSettings.ViewMode.entries)
                        .bindItem(settings::defaultViewMode.toNullableProperty())
                }
            }

            group("Appearance") {
                row("Theme:") {
                    comboBox(MermaidSettings.ThemeMode.entries)
                        .bindItem(settings::themeMode.toNullableProperty())
                }
                row("Mermaid theme:") {
                    comboBox(listOf("default", "dark", "forest", "neutral"))
                        .bindItem(settings::mermaidTheme.toNullableProperty())
                }
                row("Background:") {
                    comboBox(MermaidSettings.PreviewBackground.entries)
                        .bindItem(settings::previewBackground.toNullableProperty())
                }
            }

            group("Export") {
                row("Default format:") {
                    comboBox(MermaidSettings.ExportFormat.entries)
                        .bindItem(settings::defaultExportFormat.toNullableProperty())
                }
                row("PNG scale:") {
                    comboBox(listOf(1, 2, 3))
                        .bindItem(settings::pngScaleFactor.toNullableProperty())
                    label("x")
                }
                row {
                    checkBox("Transparent PNG background")
                        .bindSelected(settings::pngTransparentBackground)
                }
            }

            group("Zoom & Navigation") {
                row {
                    checkBox("Enable mouse wheel zoom")
                        .bindSelected(settings::mouseWheelZoomEnabled)
                }
                row("Modifier key:") {
                    comboBox(MermaidSettings.ModifierKey.entries)
                        .bindItem(settings::zoomModifierKey.toNullableProperty())
                }
                row("Default zoom:") {
                    comboBox(MermaidSettings.ZoomLevel.entries)
                        .bindItem(settings::defaultZoomLevel.toNullableProperty())
                }
            }

            group("Advanced") {
                row {
                    checkBox("Use custom Mermaid.js")
                        .bindSelected(settings::useCustomMermaidJs)
                }
                row("Custom URL:") {
                    textField()
                        .bindText(settings::customMermaidJsUrl)
                        .enabled(settings.useCustomMermaidJs)
                }
                row("Security:") {
                    comboBox(MermaidSettings.SecurityMode.entries)
                        .bindItem(settings::securityMode.toNullableProperty())
                }
            }
        }
        return panel!!
    }

    override fun isModified(): Boolean = panel != null

    override fun apply() {
        // Settings are bound directly via bindXxx
    }

    override fun reset() {
        // Reset to current settings
    }
}
