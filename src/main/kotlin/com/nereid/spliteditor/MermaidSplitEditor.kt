package com.nereid.spliteditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.nereid.diagnostics.ActionLogger
import com.nereid.diagnostics.DiagnosticCollector
import com.nereid.diagnostics.DiagnosticDialog
import com.nereid.diagnostics.DiagnosticNotifier
import com.nereid.preview.DebouncedDocumentListener
import com.nereid.preview.MermaidPreviewPanel
import java.awt.BorderLayout
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.beans.PropertyChangeListener
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane

class MermaidSplitEditor(
    private val textEditor: TextEditor,
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor {

    enum class ViewMode { CODE_ONLY, SPLIT, PREVIEW_ONLY }

    private val mainPanel: JPanel
    private val splitPane: JSplitPane
    private val previewPanel: MermaidPreviewPanel
    private val toolbar: MermaidEditorToolbar

    private var viewMode: ViewMode = ViewMode.SPLIT
    private var lastRenderError: String? = null
    private val diagnosticNotifier = DiagnosticNotifier()

    init {
        previewPanel = MermaidPreviewPanel(this)

        splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = textEditor.component
            rightComponent = previewPanel.component
            resizeWeight = 0.5
            dividerSize = 3
        }

        toolbar = MermaidEditorToolbar(
            onZoomIn = { previewPanel.zoomIn() },
            onZoomOut = { previewPanel.zoomOut() },
            onZoomReset = { previewPanel.resetView() },
            onFitToView = { previewPanel.fitToView() },
            onSettingsChanged = { previewPanel.applySettings() }
        )

        val notificationBar = diagnosticNotifier.createNotificationBar()

        val topPanel = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(notificationBar, BorderLayout.SOUTH)
        }

        mainPanel = JPanel(BorderLayout()).apply {
            add(topPanel, BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)
        }

        diagnosticNotifier.attachToPanel(mainPanel)

        setupDocumentListener()
        setupExportCallbacks()
        setupRenderErrorCallback()
        updatePreview()
    }

    private fun setupRenderErrorCallback() {
        previewPanel.onRenderError = { error ->
            lastRenderError = error
        }

        previewPanel.onReportIssue = {
            val collector = DiagnosticCollector()
            val bundle = collector.collect(
                lastRenderError = lastRenderError,
                diagramSource = textEditor.editor?.document?.text
            )
            DiagnosticDialog(project, bundle).show()
        }
    }

    private fun setupExportCallbacks() {
        previewPanel.onExportPng = { triggerExportPng() }
        previewPanel.onExportSvg = { triggerExportSvg() }
        previewPanel.onCopyPng = { triggerCopyPng() }
    }

    fun triggerExportPng() {
        ApplicationManager.getApplication().invokeLater {
            // NOTE: the varargs constructor is deprecated in favour of
            // FileSaverDescriptor(title, description).withExtensionFilter(...), but
            // withExtensionFilter() does not exist in 2023.3, our sinceBuild floor.
            // It is deprecated only (not scheduled for removal) and the sole downside is
            // a wrong label in the native Windows dialog, so it stays until we raise
            // sinceBuild. See build.gradle.kts.
            val descriptor = FileSaverDescriptor("Export as PNG", "Choose where to save the PNG file", "png")
            val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
            val result = dialog.save(file.parent, file.nameWithoutExtension + ".png")

            result?.let { wrapper ->
                previewPanel.exportAsPng { dataUrl ->
                    if (dataUrl.isNotEmpty() && dataUrl.startsWith("data:image/png;base64,")) {
                        ApplicationManager.getApplication().invokeLater {
                            try {
                                val base64Data = dataUrl.removePrefix("data:image/png;base64,")
                                val imageBytes = Base64.getDecoder().decode(base64Data)
                                wrapper.file.writeBytes(imageBytes)
                                ActionLogger.log("Exported PNG to ${wrapper.file.name}")
                            } catch (e: Exception) {
                                ActionLogger.log("PNG export failed: ${e.message}")
                                diagnosticNotifier.showError(
                                    project = project,
                                    message = "Failed to export PNG: ${e.message}",
                                    errorContext = "PNG Export",
                                    diagramSource = textEditor.editor?.document?.text
                                )
                            }
                        }
                    } else {
                        reportPngFailure("Failed to export PNG", "PNG Export", dataUrl)
                    }
                }
            }
        }
    }

    /**
     * Surfaces a failed PNG export. The preview reports problems as 'error:<reason>' and
     * used to send a bare empty string; either way the result was previously dropped on
     * the floor, so export failures were completely silent.
     */
    private fun reportPngFailure(message: String, errorContext: String, dataUrl: String) {
        val reason = when {
            dataUrl.startsWith("error:") -> dataUrl.removePrefix("error:")
            dataUrl.isEmpty() -> "the preview returned no image data"
            else -> "unexpected image data format"
        }
        ActionLogger.log("$errorContext failed: $reason")
        ApplicationManager.getApplication().invokeLater {
            diagnosticNotifier.showError(
                project = project,
                message = "$message: $reason",
                errorContext = errorContext,
                diagramSource = textEditor.editor?.document?.text
            )
        }
    }

    fun triggerExportSvg() {
        ApplicationManager.getApplication().invokeLater {
            // See the note in triggerExportPng() about this deprecated constructor.
            val descriptor = FileSaverDescriptor("Export as SVG", "Choose where to save the SVG file", "svg")
            val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
            val result = dialog.save(file.parent, file.nameWithoutExtension + ".svg")

            result?.let { wrapper ->
                previewPanel.exportAsSvg { svgContent ->
                    if (svgContent.isNotEmpty()) {
                        ApplicationManager.getApplication().invokeLater {
                            try {
                                wrapper.file.writeText(svgContent)
                                ActionLogger.log("Exported SVG to ${wrapper.file.name}")
                            } catch (e: Exception) {
                                ActionLogger.log("SVG export failed: ${e.message}")
                                diagnosticNotifier.showError(
                                    project = project,
                                    message = "Failed to export SVG: ${e.message}",
                                    errorContext = "SVG Export",
                                    diagramSource = textEditor.editor?.document?.text
                                )
                            }
                        }
                    } else {
                        ActionLogger.log("SVG Export failed: the preview returned no SVG content")
                        ApplicationManager.getApplication().invokeLater {
                            diagnosticNotifier.showError(
                                project = project,
                                message = "Failed to export SVG: the preview returned no SVG content",
                                errorContext = "SVG Export",
                                diagramSource = textEditor.editor?.document?.text
                            )
                        }
                    }
                }
            }
        }
    }

    fun triggerCopyPng() {
        previewPanel.exportAsPng { dataUrl ->
            if (dataUrl.isNotEmpty() && dataUrl.startsWith("data:image/png;base64,")) {
                ApplicationManager.getApplication().invokeLater {
                    try {
                        val base64Data = dataUrl.removePrefix("data:image/png;base64,")
                        val imageBytes = Base64.getDecoder().decode(base64Data)
                        val image = ImageIO.read(ByteArrayInputStream(imageBytes))
                        if (image != null) {
                            val transferable = ImageTransferable(image)
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
                            ActionLogger.log("Copied PNG to clipboard")
                        } else {
                            error("the exported PNG data could not be decoded")
                        }
                    } catch (e: Exception) {
                        ActionLogger.log("Copy to clipboard failed: ${e.message}")
                        diagnosticNotifier.showError(
                            project = project,
                            message = "Failed to copy to clipboard: ${e.message}",
                            errorContext = "Clipboard Copy",
                            diagramSource = textEditor.editor?.document?.text
                        )
                    }
                }
            } else {
                reportPngFailure("Failed to copy to clipboard", "Clipboard Copy", dataUrl)
            }
        }
    }

    private class ImageTransferable(private val image: BufferedImage) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = DataFlavor.imageFlavor.equals(flavor)
        override fun getTransferData(flavor: DataFlavor): Image {
            if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
            return image
        }
    }

    private fun setupDocumentListener() {
        val listener = DebouncedDocumentListener(
            delayMs = 300,
            onUpdate = { updatePreview() },
            parentDisposable = this
        )
        textEditor.editor.document.addDocumentListener(listener, this)
    }

    private fun updatePreview() {
        val source = textEditor.editor.document.text
        previewPanel.renderDiagram(source)
    }

    fun setViewMode(mode: ViewMode) {
        ActionLogger.log("Switched to view mode: ${mode.name}")
        viewMode = mode
        when (mode) {
            ViewMode.CODE_ONLY -> {
                splitPane.leftComponent = textEditor.component
                splitPane.rightComponent = null
                splitPane.dividerSize = 0
            }
            ViewMode.SPLIT -> {
                splitPane.leftComponent = textEditor.component
                splitPane.rightComponent = previewPanel.component
                splitPane.dividerSize = 3
                splitPane.resizeWeight = 0.5
            }
            ViewMode.PREVIEW_ONLY -> {
                splitPane.leftComponent = null
                splitPane.rightComponent = previewPanel.component
                splitPane.dividerSize = 0
            }
        }
    }

    override fun getTabActions(): ActionGroup {
        return DefaultActionGroup().apply {
            add(ViewModeAction("Editor Only", ViewMode.CODE_ONLY, AllIcons.General.LayoutEditorOnly))
            add(ViewModeAction("Editor and Preview", ViewMode.SPLIT, AllIcons.General.LayoutEditorPreview))
            add(ViewModeAction("Preview Only", ViewMode.PREVIEW_ONLY, AllIcons.General.LayoutPreviewOnly))
        }
    }

    private inner class ViewModeAction(
        text: String,
        private val mode: ViewMode,
        icon: javax.swing.Icon
    ) : AnAction(text, "Switch to $text view", icon), Toggleable {

        override fun actionPerformed(e: AnActionEvent) {
            setViewMode(mode)
        }

        // EDT rather than BGT: update() only reads the presentation and a field on the
        // enclosing editor, never the data context. Declared explicitly so the platform
        // does not fall back to its deprecated default.
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            Toggleable.setSelected(e.presentation, viewMode == mode)
        }
    }

    override fun getComponent(): JComponent = mainPanel

    override fun getPreferredFocusedComponent(): JComponent? = textEditor.preferredFocusedComponent

    override fun getName(): String = "Mermaid Editor"

    override fun setState(state: FileEditorState) {
        if (state is MermaidEditorState) {
            setViewMode(state.viewMode)
        }
    }

    override fun getState(level: FileEditorStateLevel): FileEditorState {
        return MermaidEditorState(viewMode)
    }

    override fun isModified(): Boolean = textEditor.isModified

    override fun isValid(): Boolean = textEditor.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        textEditor.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        textEditor.removePropertyChangeListener(listener)
    }

    override fun getCurrentLocation(): FileEditorLocation? = textEditor.currentLocation

    fun getLastRenderError(): String? = lastRenderError

    fun getDocument(): com.intellij.openapi.editor.Document? {
        return textEditor.editor?.document
    }

    override fun dispose() {
        Disposer.dispose(previewPanel)
        Disposer.dispose(textEditor)
    }

    override fun getFile(): VirtualFile = file
}

data class MermaidEditorState(val viewMode: MermaidSplitEditor.ViewMode) : FileEditorState {
    override fun canBeMergedWith(otherState: FileEditorState, level: FileEditorStateLevel): Boolean {
        return otherState is MermaidEditorState
    }
}
