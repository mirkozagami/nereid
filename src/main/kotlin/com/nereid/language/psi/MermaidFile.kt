package com.nereid.language.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.nereid.language.MermaidFileType
import com.nereid.language.MermaidLanguage

class MermaidFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MermaidLanguage.INSTANCE) {

    override fun getFileType(): FileType = MermaidFileType.INSTANCE

    override fun toString(): String = "Mermaid File"
}
