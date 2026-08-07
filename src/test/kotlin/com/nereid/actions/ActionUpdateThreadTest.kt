package com.nereid.actions

import com.intellij.openapi.actionSystem.AnAction
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile

/**
 * Guards the defect from #14, where the export actions read `CommonDataKeys.VIRTUAL_FILE`
 * in `update()` without declaring `getActionUpdateThread()`. The platform reported the
 * violation and disabled the action, greying out the entire Tools > Nereid group.
 *
 * The Plugin Verifier cannot see this -- an absent override is not a bytecode
 * incompatibility -- so it stayed green while the menu was dead.
 *
 * This scans the plugin's own compiled output rather than a hardcoded list, so it covers
 * actions added in future without anyone remembering to update this test.
 */
class ActionUpdateThreadTest {

    @Test
    fun testEveryActionOverridingUpdateDeclaresItsUpdateThread() {
        val actions = pluginActionClasses()

        // A scan that silently found nothing would pass forever while testing nothing.
        assertTrue(
            "Found no AnAction subclasses in the plugin output -- the classpath scan is " +
                "broken, not the plugin",
            actions.isNotEmpty()
        )

        val offenders = actions
            .filter { it.declaresMethod("update", com.intellij.openapi.actionSystem.AnActionEvent::class.java) }
            .filterNot { it.declaresUpdateThread() }
            .map { it.name }
            .sorted()

        assertTrue(
            "These actions override update() but do not declare getActionUpdateThread(). " +
                "The platform then reports a violation and disables the action, greying out " +
                "the menu group it belongs to (see #14):\n" +
                offenders.joinToString("\n") { "  - $it" },
            offenders.isEmpty()
        )
    }

    /** True when the class itself declares the method, rather than inheriting it. */
    private fun Class<*>.declaresMethod(name: String, vararg params: Class<*>): Boolean =
        try {
            getDeclaredMethod(name, *params); true
        } catch (e: NoSuchMethodException) {
            false
        }

    /**
     * `AnAction` supplies a concrete `getActionUpdateThread()`, so resolution always
     * succeeds; what matters is whether anything below `AnAction` overrode it. A platform
     * base class such as `ComboBoxAction` overriding it counts -- the point is that the
     * action is not silently riding on the deprecated default.
     */
    private fun Class<*>.declaresUpdateThread(): Boolean =
        getMethod("getActionUpdateThread").declaringClass != AnAction::class.java

    private fun pluginActionClasses(): List<Class<*>> {
        val loader = ActionUpdateThreadTest::class.java.classLoader
        return pluginClassNames()
            .mapNotNull {
                try {
                    // initialize = false: loading an action must not run static
                    // initialisers, which may reach services no test application provides.
                    Class.forName(it, false, loader)
                } catch (e: Throwable) {
                    null
                }
            }
            .filter { AnAction::class.java.isAssignableFrom(it) && !Modifier.isAbstract(it.modifiers) }
    }

    /**
     * Enumerates every com.nereid class from the plugin's own compiled output, located by
     * resolving a known plugin class as a resource so the scan follows the build layout
     * instead of guessing it. Handles both a classes directory and a jar, since the
     * IntelliJ Gradle plugin may put either on the test classpath.
     *
     * Resource lookup rather than `protectionDomain.codeSource`: the sandbox loads plugin
     * classes through a loader that leaves the code source null.
     */
    private fun pluginClassNames(): List<String> {
        val anchor = "com/nereid/language/MermaidLanguage.class"
        val url = ActionUpdateThreadTest::class.java.classLoader.getResource(anchor)
            ?: throw AssertionError("Could not resolve $anchor on the test classpath")

        val root = when (url.protocol) {
            "jar" -> {
                val path = url.path.substringBefore("!/")
                File(java.net.URI(path))
            }
            else -> File(java.net.URI(url.toString().removeSuffix("/$anchor")))
        }

        return when {
            root.isDirectory -> root.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .map {
                    it.relativeTo(root).path
                        .removeSuffix(".class")
                        .replace(File.separatorChar, '.')
                }
                .filter { it.startsWith("com.nereid") }
                .toList()

            else -> JarFile(root).use { jar ->
                jar.entries().toList()
                    .map { it.name }
                    .filter { it.endsWith(".class") }
                    .map { it.removeSuffix(".class").replace('/', '.') }
                    .filter { it.startsWith("com.nereid") }
            }
        }
    }
}
