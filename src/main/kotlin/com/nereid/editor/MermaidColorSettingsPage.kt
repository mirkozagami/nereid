package com.nereid.editor

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.nereid.language.MermaidFileType
import javax.swing.Icon

class MermaidColorSettingsPage : ColorSettingsPage {

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Diagram type", MermaidSyntaxHighlighter.DIAGRAM_TYPE),
            AttributesDescriptor("Keyword", MermaidSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("Direction", MermaidSyntaxHighlighter.DIRECTION),
            AttributesDescriptor("Identifier", MermaidSyntaxHighlighter.IDENTIFIER),
            AttributesDescriptor("String", MermaidSyntaxHighlighter.STRING),
            AttributesDescriptor("Number", MermaidSyntaxHighlighter.NUMBER),
            AttributesDescriptor("Arrow", MermaidSyntaxHighlighter.ARROW),
            AttributesDescriptor("Brackets", MermaidSyntaxHighlighter.BRACKETS),
            AttributesDescriptor("Comment", MermaidSyntaxHighlighter.COMMENT),
            AttributesDescriptor("Directive", MermaidSyntaxHighlighter.DIRECTIVE),
            AttributesDescriptor("Bad character", MermaidSyntaxHighlighter.BAD_CHARACTER)
        )
    }

    override fun getIcon(): Icon = MermaidFileType.INSTANCE.icon

    override fun getHighlighter(): SyntaxHighlighter = MermaidSyntaxHighlighter()

    override fun getDemoText(): String = """
        %%{init: {'theme': 'dark'}}%%
        %% A sample flowchart
        flowchart LR
            A[Start] --> B{Decision}
            B -->|Yes| C[Process 1]
            B -->|No| D[Process 2]
            C --> E((End))
            D --> E

            subgraph sub1 [Subprocess]
                F["Step 1"] --> G["Step 2"]
            end
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Mermaid"
}
