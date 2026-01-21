package com.nereid.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAware
import com.nereid.language.MermaidFileType

class ExportToPngAction : AnAction("Export as PNG", "Export diagram as PNG image", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val descriptor = FileSaverDescriptor("Export as PNG", "Choose where to save the PNG file", "png")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val result = dialog.save(file.parent, file.nameWithoutExtension + ".png")

        result?.let {
            // Trigger export via editor
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}

class ExportToSvgAction : AnAction("Export as SVG", "Export diagram as SVG", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val descriptor = FileSaverDescriptor("Export as SVG", "Choose where to save the SVG file", "svg")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val result = dialog.save(file.parent, file.nameWithoutExtension + ".svg")

        result?.let {
            // Trigger export via editor
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}

class CopyAsPngAction : AnAction("Copy as PNG", "Copy diagram as PNG to clipboard", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        // Trigger clipboard copy via editor
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == MermaidFileType.INSTANCE
    }
}
