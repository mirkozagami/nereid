package com.nereid.diagnostics

import com.intellij.ide.plugins.cl.PluginAwareClassLoader
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.jcef.JBCefApp
import com.nereid.preview.MermaidBundle
import com.nereid.settings.MermaidSettings
import java.io.File

class DiagnosticCollector {

    fun collect(
        lastRenderError: String? = null,
        consoleMessages: List<String> = emptyList(),
        diagramSource: String? = null,
        errorContext: String? = null
    ): DiagnosticBundle {
        return DiagnosticBundle(
            environment = collectEnvironmentInfo(),
            pluginSettings = collectPluginSettings(),
            renderingStatus = RenderingStatus(
                lastError = lastRenderError,
                mermaidVersion = MermaidBundle.version,
                consoleMessages = consoleMessages
            ),
            recentActions = ActionLogger.getRecentActions(),
            ideLogs = collectIdeLogs(),
            diagramSource = diagramSource,
            errorContext = errorContext
        )
    }

    fun collectEnvironmentInfo(): EnvironmentInfo {
        val appInfo = ApplicationInfo.getInstance()
        val pluginVersion = resolvePluginVersion()

        val jbCefAvailable = try {
            JBCefApp.isSupported()
        } catch (e: Exception) {
            false
        }

        val jbCefVersion: String? = if (jbCefAvailable) {
            try {
                // Try to get version from system property, which may be set by JBR
                System.getProperty("jcef.version") ?: "Available"
            } catch (e: Exception) {
                "Available"
            }
        } else null

        return EnvironmentInfo(
            ideName = appInfo.fullApplicationName,
            ideVersion = appInfo.fullVersion,
            pluginVersion = pluginVersion,
            osName = SystemInfo.OS_NAME,
            osVersion = SystemInfo.OS_VERSION,
            javaVersion = SystemInfo.JAVA_VERSION,
            jbCefAvailable = jbCefAvailable,
            jbCefVersion = jbCefVersion
        )
    }

    /**
     * Resolves this plugin's own version.
     *
     * As of 2026.2 every `PluginManager` lookup (`PluginManagerCore.getPlugin`,
     * `findEnabledPlugin`, `getPluginByClass`) is `@ApiStatus.Internal` and fails plugin
     * verification. The public replacement, `PluginDetailsService`, only exists in
     * 2026.2+, so it is unusable while we still build against 2023.3. This classloader
     * route is the cross-version workaround JetBrains recommends in the interim.
     *
     * Returns "Unknown" under unit tests, which use a non-plugin classloader.
     */
    private fun resolvePluginVersion(): String {
        val classLoader = DiagnosticCollector::class.java.classLoader
        return (classLoader as? PluginAwareClassLoader)?.pluginDescriptor?.version ?: "Unknown"
    }

    fun collectPluginSettings(): Map<String, String> {
        return try {
            val settings = MermaidSettings.getInstance()
            mapOf(
                "debounceDelayMs" to settings.debounceDelayMs.toString(),
                "defaultViewMode" to settings.defaultViewMode.name,
                "previewUpdateMode" to settings.previewUpdateMode.name,
                "themeMode" to settings.themeMode.name,
                "mermaidTheme" to settings.mermaidTheme,
                "defaultExportFormat" to settings.defaultExportFormat.name,
                "pngScaleFactor" to settings.pngScaleFactor.toString(),
                "defaultZoomLevel" to settings.defaultZoomLevel.name,
                "mouseWheelZoomEnabled" to settings.mouseWheelZoomEnabled.toString(),
                "securityMode" to settings.securityMode.name
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun collectIdeLogs(): List<String> {
        return try {
            val logDir = System.getProperty("idea.log.path") ?: return emptyList()
            val logFile = File(logDir, "idea.log")
            if (!logFile.exists()) return emptyList()

            logFile.readLines()
                .filter { line ->
                    line.contains("nereid", ignoreCase = true) ||
                    line.contains("mermaid", ignoreCase = true)
                }
                .takeLast(50)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
