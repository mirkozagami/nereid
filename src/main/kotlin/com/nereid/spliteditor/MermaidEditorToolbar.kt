package com.nereid.spliteditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.options.ShowSettingsUtil
import com.nereid.settings.MermaidSettingsConfigurable
import javax.swing.JComponent

class MermaidEditorToolbar(
    private val onZoomIn: () -> Unit,
    private val onZoomOut: () -> Unit,
    private val onZoomReset: () -> Unit,
    private val onFitToView: () -> Unit
) {

    private val toolbar: ActionToolbar

    init {
        val group = DefaultActionGroup().apply {
            // Theme and Background dropdowns
            add(ThemeDropdownAction())
            add(BackgroundDropdownAction())
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

    /**
     * Opens the IDE settings page rather than a bespoke dialog. The plugin used to ship a
     * second settings UI reached from here; both it and the settings page independently
     * grew the same "bindings never applied" bug, so it was deleted in #12.
     *
     * No explicit refresh afterwards: applying the page publishes
     * MermaidSettingsListener.TOPIC, which every open preview is subscribed to.
     */
    private inner class SettingsAction : AnAction("Settings", "Open Nereid settings", AllIcons.General.Settings) {
        override fun actionPerformed(e: AnActionEvent) {
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(e.project, MermaidSettingsConfigurable::class.java)
        }
    }
}
