package com.nereid.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.*
import java.awt.Dimension
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
    private var defaultZoomLevel = settings.defaultZoomLevel
    private var securityMode = settings.securityMode

    // Retained so doOKAction() can apply() them. The bindXxx() bindings below only copy
    // the UI values into the fields above when DialogPanel.apply() is called; without
    // that, doOKAction() writes the untouched initial values straight back and nothing
    // the user changed is saved.
    //
    // MUST be declared above the init block: DialogWrapper.init() synchronously calls
    // createCenterPanel(), and Kotlin initialises properties in declaration order, so
    // declaring this afterwards leaves it null when createCenterPanel() runs.
    private val tabPanels = mutableListOf<DialogPanel>()

    init {
        title = "Nereid Settings"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val tabbedPane = JBTabbedPane()

        val general = createGeneralTab()
        val export = createExportTab()
        val advanced = createAdvancedTab()
        tabPanels.clear()
        tabPanels.addAll(listOf(general, export, advanced))

        tabbedPane.addTab("General", general)
        tabbedPane.addTab("Export", export)
        tabbedPane.addTab("Advanced", advanced)

        tabbedPane.preferredSize = Dimension(450, 350)
        return tabbedPane
    }

    private fun createGeneralTab(): DialogPanel {
        return panel {
            group("Editor Behavior") {
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

            group("Zoom & Navigation") {
                row {
                    checkBox("Enable mouse wheel zoom")
                        .bindSelected({ mouseWheelZoomEnabled }, { mouseWheelZoomEnabled = it })
                }
                row("Default zoom:") {
                    comboBox(MermaidSettings.ZoomLevel.entries)
                        .bindItem({ defaultZoomLevel }, { defaultZoomLevel = it ?: MermaidSettings.ZoomLevel.FIT_ALL })
                }
            }
        }
    }

    private fun createExportTab(): DialogPanel {
        return panel {
            group("Export Settings") {
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
        }
    }

    private fun createAdvancedTab(): DialogPanel {
        return panel {
            group("Theme & Appearance") {
                row("Theme mode:") {
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


            group("Security") {
                row("Security level:") {
                    comboBox(MermaidSettings.SecurityMode.entries)
                        .bindItem({ securityMode }, { securityMode = it ?: MermaidSettings.SecurityMode.STRICT })
                }
            }
        }
    }

    override fun doOKAction() {
        // Push the UI values into the backing fields first. Without this the
        // assignments below just write the untouched initial values back, so nothing
        // the user changed in this dialog was ever saved.
        tabPanels.forEach { it.apply() }

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
        settings.defaultZoomLevel = defaultZoomLevel
        settings.securityMode = securityMode

        super.doOKAction()
    }
}
