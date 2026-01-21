package com.nereid.preview

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import javax.swing.UIManager

class ThemeManager(private val parentDisposable: Disposable) {

    var onThemeChanged: ((isDark: Boolean, mermaidTheme: String) -> Unit)? = null

    init {
        val connection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
            notifyThemeChanged()
        })
    }

    fun isDarkTheme(): Boolean {
        val laf = LafManager.getInstance().currentLookAndFeel
        return laf?.name?.lowercase()?.contains("dark") == true ||
               laf?.name?.lowercase()?.contains("darcula") == true ||
               UIManager.getBoolean("ui.dark")
    }

    fun getMermaidTheme(): String {
        return if (isDarkTheme()) "dark" else "default"
    }

    private fun notifyThemeChanged() {
        onThemeChanged?.invoke(isDarkTheme(), getMermaidTheme())
    }

    fun getCurrentThemeConfig(): ThemeConfig {
        return ThemeConfig(
            isDark = isDarkTheme(),
            mermaidTheme = getMermaidTheme()
        )
    }

    data class ThemeConfig(
        val isDark: Boolean,
        val mermaidTheme: String
    )
}
