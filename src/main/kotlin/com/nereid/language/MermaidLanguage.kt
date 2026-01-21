package com.nereid.language

import com.intellij.lang.Language

object MermaidLanguage : Language("Mermaid") {

    @JvmStatic
    val INSTANCE: MermaidLanguage = this

    override fun getDisplayName(): String = "Mermaid"

    override fun isCaseSensitive(): Boolean = true
}
