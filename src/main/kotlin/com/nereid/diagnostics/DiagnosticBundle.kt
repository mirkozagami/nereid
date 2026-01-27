package com.nereid.diagnostics

import java.text.SimpleDateFormat
import java.util.*

data class EnvironmentInfo(
    val ideName: String,
    val ideVersion: String,
    val pluginVersion: String,
    val osName: String,
    val osVersion: String,
    val javaVersion: String,
    val jbCefAvailable: Boolean,
    val jbCefVersion: String?
) {
    fun toReportString(): String = buildString {
        appendLine("- IDE: $ideName $ideVersion")
        appendLine("- Plugin: Nereid $pluginVersion")
        appendLine("- OS: $osName $osVersion")
        appendLine("- Java: $javaVersion")
        append("- JBCef: ${if (jbCefAvailable) "Available ($jbCefVersion)" else "Not available"}")
    }
}

data class RenderingStatus(
    val lastError: String?,
    val mermaidVersion: String?,
    val consoleMessages: List<String>
) {
    fun toReportString(): String = buildString {
        if (lastError != null) {
            appendLine("**Last Error:** $lastError")
            appendLine()
        }
        appendLine("- Mermaid.js version: ${mermaidVersion ?: "Unknown"}")
        if (consoleMessages.isNotEmpty()) {
            appendLine()
            appendLine("**Console Output:**")
            consoleMessages.forEach { appendLine("  $it") }
        }
    }
}

data class ActionLogEntry(
    val timestamp: Long,
    val action: String
) {
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun toReportString(): String = "[${dateFormat.format(Date(timestamp))}] $action"
}

data class DiagnosticSection(
    val title: String,
    val content: String
)

data class DiagnosticBundle(
    val environment: EnvironmentInfo,
    val pluginSettings: Map<String, String>,
    val renderingStatus: RenderingStatus,
    val recentActions: List<ActionLogEntry>,
    val ideLogs: List<String>,
    val diagramSource: String?,
    val errorContext: String? = null,
    val userComments: String? = null
) {
    fun getIncludedSections(
        includeEnvironment: Boolean = true,
        includeSettings: Boolean = true,
        includeRendering: Boolean = true,
        includeActions: Boolean = true,
        includeLogs: Boolean = true,
        includeDiagram: Boolean = false
    ): List<DiagnosticSection> {
        val sections = mutableListOf<DiagnosticSection>()

        if (includeEnvironment) {
            sections.add(DiagnosticSection("Environment", environment.toReportString()))
        }

        if (includeSettings && pluginSettings.isNotEmpty()) {
            val settingsContent = pluginSettings.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }
            sections.add(DiagnosticSection("Plugin Settings", settingsContent))
        }

        if (includeRendering) {
            sections.add(DiagnosticSection("Mermaid Rendering Status", renderingStatus.toReportString()))
        }

        if (includeActions && recentActions.isNotEmpty()) {
            val actionsContent = recentActions.joinToString("\n") { it.toReportString() }
            sections.add(DiagnosticSection("Recent Actions", actionsContent))
        }

        if (includeLogs && ideLogs.isNotEmpty()) {
            val logsContent = ideLogs.joinToString("\n")
            sections.add(DiagnosticSection("IDE Logs", logsContent))
        }

        if (includeDiagram && diagramSource != null) {
            sections.add(DiagnosticSection("Diagram Source", "```mermaid\n$diagramSource\n```"))
        }

        return sections
    }
}
