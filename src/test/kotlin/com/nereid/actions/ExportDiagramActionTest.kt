package com.nereid.actions

import com.nereid.settings.MermaidSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * `defaultExportFormat` offers PNG / SVG on the settings page and was read by nothing but
 * the diagnostics bundle (#39).
 *
 * It had no possible consumer as things stood: "Export as PNG" and "Export as SVG" name
 * their format, and the context menu offers both, so there was nothing for a *default* to
 * influence. The one place a format was being chosen on the user's behalf was the
 * Ctrl+Shift+E shortcut, which plugin.xml bound straight to `ExportToPngAction` -- a
 * single "export" gesture hardcoded to PNG. That is what the setting now governs.
 */
class ExportDiagramActionTest {

    @Test
    fun testPngFormatTriggersThePngExport() {
        var png = 0
        var svg = 0

        performExport(MermaidSettings.ExportFormat.PNG, onPng = { png++ }, onSvg = { svg++ })

        assertEquals("PNG export was not triggered", 1, png)
        assertEquals("SVG export was triggered for a PNG default", 0, svg)
    }

    @Test
    fun testSvgFormatTriggersTheSvgExport() {
        var png = 0
        var svg = 0

        performExport(MermaidSettings.ExportFormat.SVG, onPng = { png++ }, onSvg = { svg++ })

        assertEquals("SVG export was not triggered", 1, svg)
        assertEquals(
            "PNG export was triggered although the default format is SVG. This is the #39 " +
                "defect: the shortcut ignores defaultExportFormat and always exports PNG.",
            0,
            png
        )
    }

    /**
     * The user-visible half. The shortcut must go through the format-aware action, or the
     * dispatch above is correct code that nothing reaches.
     */
    @Test
    fun testTheExportShortcutIsBoundToTheFormatAwareAction() {
        val shortcut = Regex(
            """<action id="Mermaid\.ExportShortcut"[^>]*class="([^"]+)"""
        ).find(pluginXml())?.groupValues?.get(1)

        assertTrue(
            "plugin.xml has no Mermaid.ExportShortcut action. Ctrl+Shift+E must be bound " +
                "to the format-aware export action, not to ExportToPngAction (#39).",
            shortcut != null
        )
        assertEquals(
            "The Ctrl+Shift+E shortcut does not use ExportDiagramAction, so it exports PNG " +
                "whatever the user set as their default format",
            "com.nereid.actions.ExportDiagramAction",
            shortcut
        )
        assertTrue(
            "The shortcut action lost its Ctrl+Shift+E binding",
            pluginXml().contains("""first-keystroke="ctrl shift E"""")
        )
    }

    /**
     * The explicit per-format actions must stay exactly as they are. A user choosing
     * "Export as SVG" from the menu is naming the format, and a default has no business
     * overriding that.
     */
    @Test
    fun testTheExplicitPerFormatActionsAreUnchanged() {
        val xml = pluginXml()
        assertTrue(
            "Mermaid.ExportPng must still map to ExportToPngAction",
            xml.contains("""<action id="Mermaid.ExportPng" class="com.nereid.actions.ExportToPngAction"/>""")
        )
        assertTrue(
            "Mermaid.ExportSvg must still map to ExportToSvgAction",
            xml.contains("""<action id="Mermaid.ExportSvg" class="com.nereid.actions.ExportToSvgAction"/>""")
        )
        assertFalse(
            "The old PNG-only shortcut id is still present; Ctrl+Shift+E would be bound twice",
            xml.contains("Mermaid.ExportPngShortcut")
        )
    }

    private fun pluginXml(): String {
        val relative = "src/main/resources/META-INF/plugin.xml"
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative)
            if (candidate.isFile) return candidate.readText()
            dir = dir.parentFile
        }
        fail("Could not locate $relative from ${File("").absolutePath}")
        return ""
    }
}
