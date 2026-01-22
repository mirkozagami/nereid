package com.nereid.spliteditor

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.nereid.settings.MermaidSettings
import javax.swing.JComponent

class ThemeDropdownAction(
    private val onThemeChanged: () -> Unit
) : ComboBoxAction() {

    private val settings = MermaidSettings.getInstance()

    companion object {
        private val THEMES = listOf(
            "default" to "Default",
            "dark" to "Dark",
            "forest" to "Forest",
            "neutral" to "Neutral"
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun createPopupActionGroup(button: JComponent, dataContext: com.intellij.openapi.actionSystem.DataContext): DefaultActionGroup {
        return DefaultActionGroup().apply {
            THEMES.forEach { (value, label) ->
                add(ThemeAction(value, label))
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val currentTheme = settings.mermaidTheme
        val label = THEMES.find { it.first == currentTheme }?.second ?: "Default"
        e.presentation.text = "Theme: $label"
    }

    private inner class ThemeAction(
        private val themeValue: String,
        label: String
    ) : ToggleAction(label) {

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean {
            return settings.mermaidTheme == themeValue
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (state) {
                settings.mermaidTheme = themeValue
                // Set theme mode to use the selected Mermaid theme
                settings.themeMode = MermaidSettings.ThemeMode.MERMAID_THEME
                onThemeChanged()
            }
        }
    }
}
