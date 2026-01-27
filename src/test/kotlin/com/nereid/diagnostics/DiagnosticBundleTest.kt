package com.nereid.diagnostics

import org.junit.Assert.*
import org.junit.Test

class DiagnosticBundleTest {

    @Test
    fun testEnvironmentInfoToString() {
        val env = EnvironmentInfo(
            ideName = "IntelliJ IDEA",
            ideVersion = "2024.1.2",
            pluginVersion = "1.0.3",
            osName = "macOS",
            osVersion = "14.2",
            javaVersion = "17.0.9",
            jbCefAvailable = true,
            jbCefVersion = "122.0"
        )

        val output = env.toReportString()

        assertTrue(output.contains("IntelliJ IDEA"))
        assertTrue(output.contains("2024.1.2"))
        assertTrue(output.contains("1.0.3"))
        assertTrue(output.contains("macOS"))
        assertTrue(output.contains("17.0.9"))
    }

    @Test
    fun testDiagnosticBundleIncludedSections() {
        val bundle = DiagnosticBundle(
            environment = EnvironmentInfo(
                ideName = "IntelliJ IDEA",
                ideVersion = "2024.1",
                pluginVersion = "1.0.3",
                osName = "macOS",
                osVersion = "14.0",
                javaVersion = "17",
                jbCefAvailable = true,
                jbCefVersion = "122"
            ),
            pluginSettings = mapOf("theme" to "dark"),
            renderingStatus = RenderingStatus(null, "11.0.0", emptyList()),
            recentActions = listOf(ActionLogEntry(System.currentTimeMillis(), "Test action")),
            ideLogs = listOf("Log line 1"),
            diagramSource = null
        )

        val sections = bundle.getIncludedSections(
            includeEnvironment = true,
            includeSettings = false,
            includeRendering = true,
            includeActions = true,
            includeLogs = false,
            includeDiagram = false
        )

        assertEquals(3, sections.size)
        assertTrue(sections.any { it.title == "Environment" })
        assertTrue(sections.any { it.title == "Mermaid Rendering Status" })
        assertTrue(sections.any { it.title == "Recent Actions" })
        assertFalse(sections.any { it.title == "Plugin Settings" })
    }
}
