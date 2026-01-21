package com.nereid.preview

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.nereid.settings.MermaidSettings
import javax.swing.UIManager

class ThemeManager(private val parentDisposable: Disposable) {

    private val settings = MermaidSettings.getInstance()

    var onThemeChanged: ((isDark: Boolean, mermaidTheme: String, background: String) -> Unit)? = null

    init {
        val connection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
            notifyThemeChanged()
        })
    }

    fun isDarkTheme(): Boolean {
        return when (settings.themeMode) {
            MermaidSettings.ThemeMode.FOLLOW_IDE -> {
                val laf = LafManager.getInstance().currentLookAndFeel
                laf?.name?.lowercase()?.contains("dark") == true ||
                    laf?.name?.lowercase()?.contains("darcula") == true ||
                    UIManager.getBoolean("ui.dark")
            }
            MermaidSettings.ThemeMode.MERMAID_THEME -> {
                settings.mermaidTheme in listOf("dark")
            }
            MermaidSettings.ThemeMode.CUSTOM -> false
        }
    }

    fun getMermaidTheme(): String {
        return when (settings.themeMode) {
            MermaidSettings.ThemeMode.FOLLOW_IDE -> if (isDarkTheme()) "dark" else "default"
            MermaidSettings.ThemeMode.MERMAID_THEME -> settings.mermaidTheme
            MermaidSettings.ThemeMode.CUSTOM -> settings.mermaidTheme
        }
    }

    fun getBackgroundColor(): String {
        return when (settings.previewBackground) {
            MermaidSettings.PreviewBackground.TRANSPARENT -> "transparent"
            MermaidSettings.PreviewBackground.MATCH_IDE -> if (isDarkTheme()) "#1e1e1e" else "#ffffff"
            MermaidSettings.PreviewBackground.WHITE -> "#ffffff"
            MermaidSettings.PreviewBackground.DARK -> "#1e1e1e"
        }
    }

    private fun notifyThemeChanged() {
        onThemeChanged?.invoke(isDarkTheme(), getMermaidTheme(), getBackgroundColor())
    }

    fun applyCurrentSettings() {
        notifyThemeChanged()
    }

    fun getCurrentThemeConfig(): ThemeConfig {
        return ThemeConfig(
            isDark = isDarkTheme(),
            mermaidTheme = getMermaidTheme(),
            background = getBackgroundColor()
        )
    }

    data class ThemeConfig(
        val isDark: Boolean,
        val mermaidTheme: String,
        val background: String
    )
}
