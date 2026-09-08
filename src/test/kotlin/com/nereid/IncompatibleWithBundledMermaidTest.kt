package com.nereid

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * The descriptor must declare that Nereid cannot load beside JetBrains' Mermaid plugin.
 *
 * IntelliJ language IDs are one global registry. `MermaidLanguage` is `Language("Mermaid")`,
 * and JetBrains' own plugin (`com.intellij.mermaid`) registers a language with that exact
 * ID. Whichever loads second throws `ImplementationConflictException` out of
 * `Language.<init>`. Because `MermaidLanguage` is a Kotlin `object`, that happens in
 * `<clinit>`, so the class is poisoned for the rest of the session and everything
 * downstream of it -- `MermaidFileType`, `MermaidParserDefinition`, the editor provider --
 * fails with `NoClassDefFoundError`. The IDE also shows a modal plugin-conflict dialog at
 * startup, which is what hung JetBrains' `install-plugin-test` for ten minutes (#64).
 *
 * That plugin became *bundled* in build 262, across IDEA Community and Ultimate, WebStorm,
 * and PyCharm Community and Professional -- verified by reading `product-info.json` out of
 * each distribution. It is absent in 261 and below. Since Nereid's `sinceBuild` is 233 and
 * `untilBuild` is open, the Marketplace serves it to 2026.2 users, where it conflicts on
 * install. The same conflict has always been possible on older IDEs for anyone who
 * installed the JetBrains plugin by hand, when it was still an optional download.
 *
 * `<incompatible-with>` is the platform's way to say this: the IDE disables Nereid with an
 * explanation instead of loading it into a broken state. It is a stopgap. #65 tracks
 * reworking 262+ to layer on the bundled language rather than compete with it, at which
 * point this declaration goes away and this test with it.
 *
 * Guarded by a test because the failure it prevents is invisible to everything else in the
 * build: verification is static bytecode analysis, and a duplicate language ID is
 * perfectly valid bytecode. Nothing but a real IDE with both plugins present would notice
 * the line going missing.
 */
class IncompatibleWithBundledMermaidTest {

    private val pluginXmlPath = "src/main/resources/META-INF/plugin.xml"
    private val bundledMermaidId = "com.intellij.mermaid"

    @Test
    fun testDescriptorDeclaresIncompatibilityWithTheBundledMermaidPlugin() {
        val descriptor = File(projectRoot(), pluginXmlPath).readText()

        // Whitespace inside the element is legal XML, so do not demand an exact string.
        val declaration = Regex(
            """<incompatible-with>\s*${Regex.escape(bundledMermaidId)}\s*</incompatible-with>"""
        )

        assertTrue(
            "plugin.xml no longer declares <incompatible-with>$bundledMermaidId</incompatible-with>. " +
                "Without it, Nereid and the Mermaid plugin bundled since build 262 both register " +
                "Language(\"Mermaid\"), and whichever loads second throws " +
                "ImplementationConflictException, taking the plugin down and blocking IDE startup " +
                "behind a modal conflict dialog. See #64. If this was removed as part of #65, " +
                "delete this test deliberately rather than making it pass.",
            declaration.containsMatchIn(descriptor)
        )
    }

    /**
     * The ID above is only worth asserting while Nereid still declares the colliding
     * language itself. If #65 removes `MermaidLanguage`, the incompatibility is obsolete
     * and this test should go with it -- so pin the premise, not just the conclusion.
     */
    @Test
    fun testNereidStillRegistersItsOwnMermaidLanguage() {
        val language = File(projectRoot(), "src/main/kotlin/com/nereid/language/MermaidLanguage.kt")

        assertTrue(
            "MermaidLanguage.kt is gone. If Nereid no longer registers its own " +
                "Language(\"Mermaid\"), the <incompatible-with> declaration this test guards is " +
                "obsolete -- remove both together (#65).",
            language.isFile
        )
        assertTrue(
            "MermaidLanguage no longer declares Language(\"Mermaid\"), so it cannot collide with " +
                "the bundled plugin and <incompatible-with> is obsolete (#65).",
            language.readText().contains("""Language("Mermaid")""")
        )
    }

    private fun projectRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            if (File(dir, pluginXmlPath).isFile) return dir
            dir = dir.parentFile
        }
        fail("Could not locate $pluginXmlPath from ${File("").absolutePath}")
        error("unreachable")
    }
}
