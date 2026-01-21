package com.nereid.language

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidLanguageTest : BasePlatformTestCase() {

    fun testLanguageInstance() {
        val language = MermaidLanguage.INSTANCE
        assertNotNull(language)
        assertEquals("Mermaid", language.id)
        assertEquals("Mermaid", language.displayName)
    }

    fun testLanguageIsCaseSensitive() {
        assertTrue(MermaidLanguage.INSTANCE.isCaseSensitive)
    }
}
