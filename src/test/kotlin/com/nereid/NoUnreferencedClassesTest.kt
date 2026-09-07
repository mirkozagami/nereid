package com.nereid

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Every top-level class must be referenced by something other than its own file.
 *
 * Plugin verification is static bytecode analysis: a class nobody constructs is perfectly
 * valid bytecode, so nothing in the build notices one. That let `PngExporter` and
 * `SvgExporter` sit in the tree looking like the export implementation while production
 * exports went through `MermaidPreviewPanel`'s JS bridge instead -- and both reported
 * success without writing anything, so anyone who wired them up would have got `true`
 * back, no file, and no error (#48). `ClipboardExporter` was dead alongside them.
 *
 * This is the same shape of guard as `MermaidSettingsAreConsumedTest`, and it inherits
 * that test's lesson: comments are stripped before searching, because prose naming a
 * class counts as a reference and would let a dead class stay green because a KDoc
 * mentioned it.
 *
 * Classes registered with the platform are excluded, and *every* plugin descriptor is
 * searched, not just plugin.xml. The platform instantiates these reflectively --
 * annotators, providers, actions, file types -- so our own code never names them and each
 * would otherwise look dead. `markdown-support.xml`, pulled in by the optional markdown
 * dependency, registers two on its own; reading only plugin.xml reported both as dead.
 *
 * "Referenced" means anywhere other than the declaration itself, including elsewhere in
 * the same file. Kotlin puts several top-level classes in one file quite legitimately --
 * `MermaidEditorState` and the structure-view helpers are only ever used beside their own
 * declaration -- and demanding a reference from a *different* file condemns all of them.
 */
class NoUnreferencedClassesTest {

    private val mainSources = "src/main/kotlin/com/nereid"
    private val descriptorDir = "src/main/resources/META-INF"
    private val pluginXmlPath = "$descriptorDir/plugin.xml"

    @Test
    fun testEveryClassIsReferencedSomewhereOtherThanItsOwnDeclaration() {
        val root = projectRoot()
        val sources = File(root, mainSources).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        assertTrue(
            "Found no Kotlin sources to scan -- this test is not testing anything",
            sources.size > 10
        )

        val descriptors = File(root, descriptorDir).walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .toList()
        assertTrue(
            "Found no plugin descriptors; every platform-registered class would look dead",
            descriptors.isNotEmpty()
        )
        val registered = descriptors.joinToString("\n") { it.readText() }

        val bodies = sources.associateWith { withoutComments(it.readText()) }

        val dead = bodies.keys.flatMap { file ->
            declaredClasses(bodies.getValue(file)).mapNotNull { name ->
                val mention = Regex("""\b${Regex.escape(name)}\b""")

                // Every occurrence across the codebase, minus the declarations themselves.
                val mentions = bodies.values.sumOf { mention.findAll(it).count() }
                val declarations = bodies.values.sumOf { declarationOf(name).findAll(it).count() }

                val used = mentions > declarations || mention.containsMatchIn(registered)

                if (used) null else "$name (${file.name})"
            }
        }

        assertTrue(
            "These classes are declared and then never mentioned again -- not by any other " +
                "code, not beside their own declaration, and not by any plugin descriptor. " +
                "Nothing constructs them, and plugin verification cannot see that because a " +
                "class nobody uses is still valid bytecode. Delete them, or wire them up " +
                "(see #48):\n" +
                dead.joinToString("\n") { "  - $it" },
            dead.isEmpty()
        )
    }

    /**
     * Top-level class and object declarations. Nested and inner ones are skipped: they
     * are reached through their outer class, so judging them in isolation would flag
     * every private helper.
     */
    private fun declaredClasses(source: String): List<String> =
        DECLARATION.findAll(source).map { it.groupValues[1] }.toList()

    /** The declaration of [name] specifically, so its own occurrences can be discounted. */
    private fun declarationOf(name: String): Regex =
        Regex(
            """^(?:internal )?(?:open |abstract |data |sealed )*(?:class|object) ${Regex.escape(name)}\b""",
            RegexOption.MULTILINE
        )

    /**
     * Strips comments so prose cannot count as a reference -- the false negative found by
     * mutation in #56, where a KDoc naming a setting kept an unwired setting green.
     */
    private fun withoutComments(source: String): String =
        source
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("""//[^\n]*"""), " ")

    private companion object {
        /**
         * Top-level class and object declarations. Nested and inner ones are skipped by
         * the line-start anchor: they are reached through their outer class, so judging
         * them alone would flag every private helper.
         */
        val DECLARATION =
            Regex(
                """^(?:internal )?(?:open |abstract |data |sealed )*(?:class|object) (\w+)""",
                RegexOption.MULTILINE
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
