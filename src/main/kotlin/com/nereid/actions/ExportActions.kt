package com.nereid.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.nereid.language.MermaidFileType
import com.nereid.settings.MermaidSettings
import com.nereid.spliteditor.MermaidSplitEditor

// These actions read CommonDataKeys.VIRTUAL_FILE in update(), which the platform
// requires to happen off the EDT, hence ActionUpdateThread.BGT. Without it IntelliJ
// reports the violation and disables the action, greying out the whole Tools > Nereid
// group.
class ExportToPngAction : AnAction("Export as PNG", "Export diagram as PNG image", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = getActiveMermaidEditor(e) ?: return
        editor.triggerExportPng()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}

class ExportToSvgAction : AnAction("Export as SVG", "Export diagram as SVG", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = getActiveMermaidEditor(e) ?: return
        editor.triggerExportSvg()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}

/**
 * Exports in whichever format the user set as their default.
 *
 * This is what Ctrl+Shift+E runs. The shortcut previously pointed straight at
 * [ExportToPngAction], so the one export gesture that does not name a format was
 * hardcoded to PNG while `defaultExportFormat` sat in the settings UI doing nothing
 * (#39). The menu entries stay per-format: choosing "Export as SVG" names the format, and
 * a default has no business overriding that.
 */
class ExportDiagramAction : AnAction("Export Diagram", "Export diagram in the default format", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = getActiveMermaidEditor(e) ?: return
        performExport(
            format = MermaidSettings.getInstance().defaultExportFormat,
            onPng = editor::triggerExportPng,
            onSvg = editor::triggerExportSvg,
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}

/**
 * Dispatches an export to the handler for [format].
 *
 * Split out from the action so the choice can be tested without a live editor -- the
 * editor builds a JCEF panel and cannot be constructed headlessly.
 */
internal fun performExport(
    format: MermaidSettings.ExportFormat,
    onPng: () -> Unit,
    onSvg: () -> Unit,
) {
    when (format) {
        MermaidSettings.ExportFormat.PNG -> onPng()
        MermaidSettings.ExportFormat.SVG -> onSvg()
    }
}

class CopyAsPngAction : AnAction("Copy as PNG", "Copy diagram as PNG to clipboard", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = getActiveMermaidEditor(e) ?: return
        editor.triggerCopyPng()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}

private fun getActiveMermaidEditor(e: AnActionEvent): MermaidSplitEditor? {
    val project = e.project ?: return null
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
    val editors = FileEditorManager.getInstance(project).getEditors(file)
    return editors.filterIsInstance<MermaidSplitEditor>().firstOrNull()
}
