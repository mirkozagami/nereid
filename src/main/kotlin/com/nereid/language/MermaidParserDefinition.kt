package com.nereid.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.nereid.language.psi.MermaidFile

class MermaidParserDefinition : ParserDefinition {

    companion object {
        val FILE by lazy { IFileElementType(MermaidLanguage) }
    }

    override fun createLexer(project: Project?): Lexer = MermaidLexer()

    override fun createParser(project: Project?): PsiParser {
        return PsiParser { root, builder ->
            val marker = builder.mark()
            while (!builder.eof()) {
                builder.advanceLexer()
            }
            marker.done(root)
            builder.treeBuilt
        }
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = MermaidTokenTypes.COMMENTS

    override fun getStringLiteralElements(): TokenSet = MermaidTokenTypes.STRINGS

    override fun createElement(node: ASTNode?): PsiElement {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = MermaidFile(viewProvider)
}
