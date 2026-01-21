package com.nereid.language

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidFileTypeTest : BasePlatformTestCase() {

    fun testFileTypeProperties() {
        val fileType = MermaidFileType.INSTANCE
        assertEquals("Mermaid", fileType.name)
        assertEquals("Mermaid diagram file", fileType.description)
        assertEquals("mmd", fileType.defaultExtension)
        assertNotNull(fileType.icon)
    }

    fun testFileTypeAssociation() {
        val file = myFixture.configureByText("test.mmd", "graph TD")
        assertEquals(MermaidFileType.INSTANCE, file.virtualFile.fileType)
    }
}
