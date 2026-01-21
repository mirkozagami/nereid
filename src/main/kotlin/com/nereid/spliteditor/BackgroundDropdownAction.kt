package com.nereid.spliteditor

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.nereid.settings.MermaidSettings
import javax.swing.JComponent

class BackgroundDropdownAction(
    private val onBackgroundChanged: () -> Unit
) : ComboBoxAction() {

    private val settings = MermaidSettings.getInstance()

    companion object {
        private val BACKGROUNDS = listOf(
            MermaidSettings.PreviewBackground.MATCH_IDE to "Match IDE",
            MermaidSettings.PreviewBackground.WHITE to "White",
            MermaidSettings.PreviewBackground.DARK to "Dark",
            MermaidSettings.PreviewBackground.TRANSPARENT to "Transparent"
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun createPopupActionGroup(button: JComponent, dataContext: com.intellij.openapi.actionSystem.DataContext): DefaultActionGroup {
        return DefaultActionGroup().apply {
            BACKGROUNDS.forEach { (value, label) ->
                add(BackgroundAction(value, label))
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val currentBg = settings.previewBackground
        val label = BACKGROUNDS.find { it.first == currentBg }?.second ?: "Match IDE"
        e.presentation.text = "Background: $label"
    }

    private inner class BackgroundAction(
        private val bgValue: MermaidSettings.PreviewBackground,
        label: String
    ) : ToggleAction(label) {

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean {
            return settings.previewBackground == bgValue
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (state) {
                settings.previewBackground = bgValue
                onBackgroundChanged()
            }
        }
    }
}
