package com.nereid.language

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MermaidFileType : LanguageFileType(MermaidLanguage.INSTANCE) {

    @JvmStatic
    val INSTANCE: MermaidFileType = this

    override fun getName(): String = "Mermaid"

    override fun getDescription(): String = "Mermaid diagram file"

    override fun getDefaultExtension(): String = "mmd"

    override fun getIcon(): Icon = IconLoader.getIcon("/icons/mermaid.svg", MermaidFileType::class.java)
}
