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
 * `DiagnosticCollector` is deliberately excluded from the search. It reads nearly every
 * setting in order to print it into a bug bundle, which is a reference but not a use --
 * and treating it as one is exactly what let the Tier 2 settings in #39 look consumed
 * while changing nothing. A setting must be read by something that acts on it.
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
            .filterNot { it.name == DIAGNOSTICS_REPORTER }
            .map { withoutComments(it.readText()) }
            .toList()

        val unread = declared.filter { setting ->
            val reference = Regex("""\b${Regex.escape(setting)}\b""")
            consumers.none { reference.containsMatchIn(it) }
        }

        assertTrue(
            "These settings are persisted but nothing acts on them. Being printed into a " +
                "diagnostic bundle by $DIAGNOSTICS_REPORTER does not count -- that is what " +
                "made the Tier 2 settings in #39 look consumed while they changed nothing. " +
                "They show up in the UI, save to the user's mermaid.xml, and do nothing. " +
                "Either wire them up or delete them:\n" +
                unread.joinToString("\n") { "  - $it" },
            unread.isEmpty()
        )
    }

    private companion object {
        /**
         * Reports settings into bug bundles rather than acting on them, so a reference
         * from here says nothing about whether a setting works.
         */
        const val DIAGNOSTICS_REPORTER = "DiagnosticCollector.kt"
    }

    /**
     * Strips block and line comments.
     *
     * Without this the search matches prose. A KDoc explaining that some setting used to
     * be ignored counts as a reference to it, so a setting could be unwired and still
     * pass purely because a comment named it -- which is precisely the kind of
     * false negative this test exists to prevent. Verified by re-wiring a setting back to
     * a literal and confirming this test then fails.
     */
    private fun withoutComments(source: String): String =
        source
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("""//[^\n]*"""), " ")

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
