package com.nereid.spliteditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.nereid.settings.MermaidSettingsDialog
import javax.swing.JComponent

class MermaidEditorToolbar(
    private val onZoomIn: () -> Unit,
    private val onZoomOut: () -> Unit,
    private val onZoomReset: () -> Unit,
    private val onFitToView: () -> Unit,
    private val onSettingsChanged: () -> Unit
) {

    private val toolbar: ActionToolbar

    init {
        val group = DefaultActionGroup().apply {
            // Theme and Background dropdowns
            add(ThemeDropdownAction { onSettingsChanged() })
            add(BackgroundDropdownAction { onSettingsChanged() })
            addSeparator()
            // Zoom controls
            add(ZoomInAction())
            add(ZoomOutAction())
            add(ZoomResetAction())
            add(FitToViewAction())
            addSeparator()
            // Settings
            add(SettingsAction())
        }

        toolbar = ActionManager.getInstance().createActionToolbar("MermaidEditor", group, true)
        toolbar.targetComponent = toolbar.component
    }

    val component: JComponent get() = toolbar.component

    private inner class ZoomInAction : AnAction("Zoom In", "Zoom in", AllIcons.General.Add) {
        override fun actionPerformed(e: AnActionEvent) = onZoomIn()
    }

    private inner class ZoomOutAction : AnAction("Zoom Out", "Zoom out", AllIcons.General.Remove) {
        override fun actionPerformed(e: AnActionEvent) = onZoomOut()
    }

    private inner class ZoomResetAction : AnAction("Reset Zoom", "Reset to 100%", AllIcons.General.ActualZoom) {
        override fun actionPerformed(e: AnActionEvent) = onZoomReset()
    }

    private inner class FitToViewAction : AnAction("Fit to View", "Fit diagram to view", AllIcons.General.FitContent) {
        override fun actionPerformed(e: AnActionEvent) = onFitToView()
    }

    private inner class SettingsAction : AnAction("Settings", "Open Mermaid settings", AllIcons.General.Settings) {
        override fun actionPerformed(e: AnActionEvent) {
            val dialog = MermaidSettingsDialog(e.project)
            if (dialog.showAndGet()) {
                onSettingsChanged()
            }
        }
    }
}
