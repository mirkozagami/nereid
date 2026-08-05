package com.nereid.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

class MermaidSettingsConfigurable : Configurable {

    // Must stay DialogPanel, not JComponent: the bindXxx() bindings only write back
    // when DialogPanel.apply() is called, and typing this as JComponent hides the
    // apply()/reset()/isModified() methods that make the panel work at all.
    private var panel: DialogPanel? = null
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

    // The Kotlin UI DSL does NOT write bound properties back on its own -- these three
    // must delegate to the DialogPanel. Previously apply() was empty, so nothing in this
    // page was ever saved, and isModified() returned `panel != null`, leaving Apply
    // permanently enabled while doing nothing.
    override fun isModified(): Boolean = panel?.isModified() ?: false

    override fun apply() {
        panel?.apply()
    }

    override fun reset() {
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}
