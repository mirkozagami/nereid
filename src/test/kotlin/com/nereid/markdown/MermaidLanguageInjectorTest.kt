package com.nereid.markdown

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Guards the reflective Markdown lookup in [MermaidLanguageInjector].
 *
 * `elementsToInjectIn()` resolves the fence class by name through `Class.forName`. If the
 * Markdown plugin ever renames or moves `MarkdownCodeFenceImpl`, the lookup returns an
 * empty list, the injector is registered against nothing, and Mermaid highlighting inside
 * ```mermaid fences stops working -- with no error the user can see.
 *
 * The Plugin Verifier cannot catch this: a name passed to `Class.forName` is a string
 * constant, not a bytecode reference, so there is no unresolved symbol for it to report.
 * That is precisely why it needs a test.
 */
class MermaidLanguageInjectorTest : BasePlatformTestCase() {

    fun testInjectorResolvesTheMarkdownFenceClass() {
        val elements = MermaidLanguageInjector().elementsToInjectIn()

        assertFalse(
            "MermaidLanguageInjector.elementsToInjectIn() is empty, so Mermaid is injected " +
                "into nothing and syntax support inside ```mermaid fences is silently dead. " +
                "The reflective lookup of MarkdownCodeFenceImpl failed -- the Markdown " +
                "plugin has most likely renamed or moved the class.",
            elements.isEmpty()
        )
    }

    /**
     * Pins the resolved class, not merely that something resolved. A lookup that started
     * returning some unrelated PSI class would still be non-empty but would inject in the
     * wrong place.
     */
    fun testResolvedClassIsTheMarkdownCodeFence() {
        val names = MermaidLanguageInjector().elementsToInjectIn().map { it.name }

        assertEquals(
            "The injector should target exactly the Markdown code fence element",
            listOf("org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl"),
            names
        )
    }
}
