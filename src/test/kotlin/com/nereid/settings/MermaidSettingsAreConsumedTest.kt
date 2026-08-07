package com.nereid.settings

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Every persisted setting must be read by something outside the settings package.
 *
 * A setting that is only ever written is invisible to every other check we have. Plugin
 * verification is static bytecode analysis, and a write-only field is perfectly valid
 * bytecode. The persistence tests added in #21 confirm a setting survives `apply()`,
 * which an inert setting does just as well as a working one.
 *
 * That gap let nine settings ship as decoration -- shown in the UI, saved to the user's
 * mermaid.xml, changing nothing (#39).
 *
 * Known limit: `DiagnosticCollector` reads most settings purely to report them in bug
 * bundles, and that counts as a reference here. So this catches "nothing reads it at all",
 * not "read but never acted on". The seven settings in that second category are tracked
 * in #39 and need wiring up rather than deleting, which is a per-setting behaviour change.
 */
class MermaidSettingsAreConsumedTest {

    private val settingsSource = "src/main/kotlin/com/nereid/settings/MermaidSettings.kt"
    private val mainSources = "src/main/kotlin/com/nereid"

    @Test
    fun testEveryPersistedSettingIsReadOutsideTheSettingsPackage() {
        val root = projectRoot()
        val declared = declaredSettings(File(root, settingsSource).readText())

        assertTrue(
            "Parsed no settings out of MermaidSettings.kt -- this test is not testing " +
                "anything, fix the parsing rather than the plugin",
            declared.size > 5
        )

        val consumers = File(root, mainSources).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.parentFile.name == "settings" }
            .map { it.readText() }
            .toList()

        val unread = declared.filter { setting ->
            val reference = Regex("""\b${Regex.escape(setting)}\b""")
            consumers.none { reference.containsMatchIn(it) }
        }

        assertTrue(
            "These settings are persisted but nothing outside the settings package reads " +
                "them. They show up in the UI, save to the user's mermaid.xml, and change " +
                "nothing. Either wire them up or delete them (see #39):\n" +
                unread.joinToString("\n") { "  - $it" },
            unread.isEmpty()
        )
    }

    /** Property names declared as `var name: Type = ...` on MermaidSettings. */
    private fun declaredSettings(source: String): List<String> =
        Regex("""^ {4}var (\w+)""", RegexOption.MULTILINE)
            .findAll(source)
            .map { it.groupValues[1] }
            .toList()

    private fun projectRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            if (File(dir, settingsSource).isFile) return dir
            dir = dir.parentFile
        }
        fail("Could not locate $settingsSource from ${File("").absolutePath}")
        error("unreachable")
    }
}
