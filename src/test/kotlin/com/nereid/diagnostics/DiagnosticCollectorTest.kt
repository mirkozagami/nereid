package com.nereid.diagnostics

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DiagnosticCollectorTest : BasePlatformTestCase() {

    fun testCollectEnvironmentInfo() {
        val collector = DiagnosticCollector()

        val env = collector.collectEnvironmentInfo()

        assertNotNull(env.ideName)
        assertNotNull(env.ideVersion)
        assertNotNull(env.pluginVersion)
        assertNotNull(env.osName)
        assertNotNull(env.javaVersion)
    }

    fun testCollectPluginSettings() {
        val collector = DiagnosticCollector()

        val settings = collector.collectPluginSettings()

        assertNotNull(settings)
        // Settings map should contain known keys
        assertTrue(settings.containsKey("debounceDelayMs") || settings.isEmpty())
    }

    fun testCollectFullBundle() {
        val collector = DiagnosticCollector()

        val bundle = collector.collect(
            lastRenderError = "Test error",
            diagramSource = null
        )

        assertNotNull(bundle)
        assertNotNull(bundle.environment)
        assertEquals("Test error", bundle.renderingStatus.lastError)
    }
}
