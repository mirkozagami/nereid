package com.nereid.markdown

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.nereid.language.MermaidLanguage

class MermaidLanguageInjector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (!isMermaidFencedCodeBlock(context)) return

        val host = context as? PsiLanguageInjectionHost ?: return
        val text = context.text

        // Find the content between the fences
        val startIndex = text.indexOf('\n') + 1
        val endIndex = text.lastIndexOf("```").takeIf { it > startIndex } ?: text.length

        if (startIndex < endIndex) {
            registrar.startInjecting(MermaidLanguage.INSTANCE)
            registrar.addPlace(null, null, host, TextRange(startIndex, endIndex))
            registrar.doneInjecting()
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return try {
            // Try to load Markdown plugin classes
            val fenceClass = Class.forName("org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl")
            @Suppress("UNCHECKED_CAST")
            listOf(fenceClass as Class<out PsiElement>)
        } catch (e: ClassNotFoundException) {
            emptyList()
        }
    }

    private fun isMermaidFencedCodeBlock(element: PsiElement): Boolean {
        val text = element.text
        return text.startsWith("```mermaid") || text.startsWith("```Mermaid")
    }
}
