package com.nereid.spliteditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Toggleable
import com.nereid.settings.MermaidSettingsDialog
import javax.swing.JComponent

class MermaidEditorToolbar(
    private val onViewModeChanged: (MermaidSplitEditor.ViewMode) -> Unit,
    private val onZoomIn: () -> Unit,
    private val onZoomOut: () -> Unit,
    private val onZoomReset: () -> Unit,
    private val onFitToView: () -> Unit,
    private val onSettingsChanged: () -> Unit
) {

    private var currentMode: MermaidSplitEditor.ViewMode = MermaidSplitEditor.ViewMode.SPLIT

    private val codeOnlyAction = ViewModeAction("Code", MermaidSplitEditor.ViewMode.CODE_ONLY, AllIcons.Actions.EditSource)
    private val splitAction = ViewModeAction("Split", MermaidSplitEditor.ViewMode.SPLIT, AllIcons.Actions.PreviewDetails)
    private val previewOnlyAction = ViewModeAction("Preview", MermaidSplitEditor.ViewMode.PREVIEW_ONLY, AllIcons.Actions.Preview)

    private val toolbar: ActionToolbar

    init {
        val group = DefaultActionGroup().apply {
            add(codeOnlyAction)
            add(splitAction)
            add(previewOnlyAction)
            addSeparator()
            add(ZoomInAction())
            add(ZoomOutAction())
            add(ZoomResetAction())
            add(FitToViewAction())
            addSeparator()
            add(SettingsAction())
        }

        toolbar = ActionManager.getInstance().createActionToolbar("MermaidEditor", group, true)
        toolbar.targetComponent = toolbar.component

        updateToggleStates()
    }

    val component: JComponent get() = toolbar.component

    fun setViewMode(mode: MermaidSplitEditor.ViewMode) {
        currentMode = mode
        updateToggleStates()
    }

    private fun updateToggleStates() {
        codeOnlyAction.isSelected = currentMode == MermaidSplitEditor.ViewMode.CODE_ONLY
        splitAction.isSelected = currentMode == MermaidSplitEditor.ViewMode.SPLIT
        previewOnlyAction.isSelected = currentMode == MermaidSplitEditor.ViewMode.PREVIEW_ONLY
    }

    private inner class ViewModeAction(
        text: String,
        private val mode: MermaidSplitEditor.ViewMode,
        icon: javax.swing.Icon
    ) : AnAction(text, "Switch to $text view", icon), Toggleable {

        var isSelected: Boolean = false
            set(value) {
                field = value
            }

        override fun actionPerformed(e: AnActionEvent) {
            currentMode = mode
            onViewModeChanged(mode)
            updateToggleStates()
        }

        override fun update(e: AnActionEvent) {
            Toggleable.setSelected(e.presentation, isSelected)
        }
    }

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
