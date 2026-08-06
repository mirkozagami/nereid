package com.nereid.markdown

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.nereid.language.MermaidLanguage

class MermaidLanguageInjector : MultiHostInjector {

    private companion object {
        private val LOG = Logger.getInstance(MermaidLanguageInjector::class.java)
        private const val CODE_FENCE_CLASS =
            "org.intellij.plugins.markdown.lang.psi.impl.MarkdownCodeFenceImpl"
    }

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
            val fenceClass = Class.forName(CODE_FENCE_CLASS)
            @Suppress("UNCHECKED_CAST")
            listOf(fenceClass as Class<out PsiElement>)
        } catch (e: ClassNotFoundException) {
            // This injector is only registered via markdown-support.xml, which loads
            // solely when the Markdown plugin is present -- so reaching here means the
            // class was moved or renamed, not that Markdown is absent. The lookup is
            // reflective, so the Plugin Verifier cannot detect that breakage; without
            // this warning, Mermaid injection in ```mermaid fences would fail silently.
            LOG.warn("Markdown code fence class '$CODE_FENCE_CLASS' not found; Mermaid " +
                "language injection in Markdown fences is now disabled", e)
            emptyList()
        }
    }

    private fun isMermaidFencedCodeBlock(element: PsiElement): Boolean {
        val text = element.text
        return text.startsWith("```mermaid") || text.startsWith("```Mermaid")
    }
}
