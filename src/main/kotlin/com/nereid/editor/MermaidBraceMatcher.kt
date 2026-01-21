package com.nereid.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.nereid.language.MermaidTokenTypes

class MermaidBraceMatcher : PairedBraceMatcher {

    companion object {
        private val PAIRS = arrayOf(
            BracePair(MermaidTokenTypes.LBRACKET, MermaidTokenTypes.RBRACKET, false),
            BracePair(MermaidTokenTypes.LPAREN, MermaidTokenTypes.RPAREN, false),
            BracePair(MermaidTokenTypes.LBRACE, MermaidTokenTypes.RBRACE, false)
        )
    }

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
