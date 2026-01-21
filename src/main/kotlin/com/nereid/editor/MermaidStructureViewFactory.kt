package com.nereid.editor

import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.nereid.language.psi.MermaidFile
import javax.swing.Icon

class MermaidStructureViewFactory : PsiStructureViewFactory {

    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is MermaidFile) return null

        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return MermaidStructureViewModel(psiFile)
            }
        }
    }
}

class MermaidStructureViewModel(file: MermaidFile) :
    StructureViewModelBase(file, MermaidStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false
}

class MermaidStructureViewElement(private val element: PsiFile) : StructureViewTreeElement {

    override fun getValue(): Any = element

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = element.name
        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean): Icon? = null
    }

    override fun getChildren(): Array<TreeElement> {
        val children = mutableListOf<TreeElement>()
        val text = element.text

        // Parse nodes and subgraphs from text
        val nodeRegex = Regex("""(\w+)\s*[\[\(\{]""")
        val subgraphRegex = Regex("""subgraph\s+(\w+)""")

        subgraphRegex.findAll(text).forEach { match ->
            children.add(SimpleTreeElement(match.groupValues[1], "subgraph"))
        }

        nodeRegex.findAll(text).take(50).forEach { match ->
            val name = match.groupValues[1]
            if (name !in setOf("subgraph", "end", "graph", "flowchart")) {
                children.add(SimpleTreeElement(name, "node"))
            }
        }

        return children.toTypedArray()
    }

    override fun navigate(requestFocus: Boolean) {}
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
}

class SimpleTreeElement(private val name: String, private val type: String) : TreeElement {
    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = name
        override fun getLocationString(): String = type
        override fun getIcon(unused: Boolean): Icon? = null
    }

    override fun getChildren(): Array<TreeElement> = emptyArray()
}
