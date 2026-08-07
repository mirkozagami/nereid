package com.nereid.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

/**
 * The single settings UI for the plugin, reached from **Settings > Tools > Nereid** and
 * from the editor toolbar's gear button.
 *
 * There used to be a second one, `MermaidSettingsDialog`, bound to the same settings by a
 * different mechanism. Both independently shipped the identical "bindings never applied"
 * bug (#10, #11), and every new setting had to be added twice. It was deleted in #12.
 */
class MermaidSettingsConfigurable : Configurable {

    // Must stay DialogPanel, not JComponent: the bindXxx() bindings only write back
    // when DialogPanel.apply() is called, and typing this as JComponent hides the
    // apply()/reset()/isModified() methods that make the panel work at all.
    private var panel: DialogPanel? = null
    private val settings = MermaidSettings.getInstance()

    // "Nereid" is the product; Mermaid is the diagram language it supports. User-facing
    // labels use the product name, matching displayName in plugin.xml. Note the internal
    // @State(name = "MermaidSettings") key must NOT be renamed -- it is the storage key
    // in users' options/ files, and changing it would silently reset saved settings.
    override fun getDisplayName(): String = "Nereid"

    override fun createComponent(): JComponent {
        // Grouped by what the user is trying to do, rather than by which part of the
        // codebase reads the value.
        panel = panel {
            group("Preview") {
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
                row("Default zoom:") {
                    comboBox(MermaidSettings.ZoomLevel.entries)
                        .bindItem(settings::defaultZoomLevel.toNullableProperty())
                }
                row {
                    checkBox("Enable mouse wheel zoom")
                        .bindSelected(settings::mouseWheelZoomEnabled)
                }
            }

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

            group("Export") {
                row("Default format:") {
                    comboBox(MermaidSettings.ExportFormat.entries)
                        .bindItem(settings::defaultExportFormat.toNullableProperty())
                }
                // 1..8 to match the coerceIn(1, 8) the exporter actually applies. The
                // dropdown used to stop at 3, so the top of the supported range was
                // unreachable from the UI.
                row("PNG scale:") {
                    comboBox((1..8).toList())
                        .bindItem(settings::pngScaleFactor.toNullableProperty())
                    label("x")
                }
                row {
                    checkBox("Transparent PNG background")
                        .bindSelected(settings::pngTransparentBackground)
                }
            }

            group("Advanced") {
                row("Security:") {
                    comboBox(MermaidSettings.SecurityMode.entries)
                        .bindItem(settings::securityMode.toNullableProperty())
                }
                row {
                    textArea()
                        .bindText(settings::customCss)
                        .rows(6)
                        .align(AlignX.FILL)
                        .label("Custom CSS:", LabelPosition.TOP)
                        .comment(
                            "Injected into the diagram preview, after the built-in styles. " +
                                "Example: <code>#diagram svg .node rect { stroke-width: 2px; }</code>"
                        )
                }.resizableRow()
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
        panel?.apply() ?: return
        // Open previews have no other way to learn about this. Without the broadcast the
        // page saves correctly and nothing on screen changes until the file is reopened.
        MermaidSettingsListener.notifyChanged()
    }

    override fun reset() {
        panel?.reset()
    }

    override fun disposeUIResources() {
        panel = null
    }
}
