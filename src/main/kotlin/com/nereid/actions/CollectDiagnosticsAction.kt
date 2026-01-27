package com.nereid.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.nereid.diagnostics.DiagnosticCollector
import com.nereid.diagnostics.DiagnosticDialog
import com.nereid.language.MermaidFileType
import com.nereid.spliteditor.MermaidSplitEditor

class CollectDiagnosticsAction : AnAction(
    "Collect Mermaid Diagnostic Info",
    "Collect diagnostic information for bug reports",
    null
), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        // Try to get current diagram source if a Mermaid file is open
        var diagramSource: String? = null
        var lastRenderError: String? = null

        if (project != null) {
            val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
            if (file?.fileType == MermaidFileType.INSTANCE) {
                val editors = FileEditorManager.getInstance(project).getEditors(file)
                val mermaidEditor = editors.filterIsInstance<MermaidSplitEditor>().firstOrNull()
                if (mermaidEditor != null) {
                    diagramSource = mermaidEditor.getDocument()?.text
                    lastRenderError = mermaidEditor.getLastRenderError()
                }
            }
        }

        val collector = DiagnosticCollector()
        val bundle = collector.collect(
            lastRenderError = lastRenderError,
            diagramSource = diagramSource
        )

        val dialog = DiagnosticDialog(project, bundle)
        dialog.show()
    }
}
