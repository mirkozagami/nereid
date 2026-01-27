package com.nereid.spliteditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
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

        mainPanel = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)
        }

        setupDocumentListener()
        setupExportCallbacks()
        setupRenderErrorCallback()
        updatePreview()
    }

    private fun setupRenderErrorCallback() {
        previewPanel.onRenderError = { error ->
            lastRenderError = error
        }
    }

    private fun setupExportCallbacks() {
        previewPanel.onExportPng = { triggerExportPng() }
        previewPanel.onExportSvg = { triggerExportSvg() }
        previewPanel.onCopyPng = { triggerCopyPng() }
    }

    fun triggerExportPng() {
        ApplicationManager.getApplication().invokeLater {
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
                            } catch (e: Exception) {
                                // Silently fail - could show notification in the future
                            }
                        }
                    }
                }
            }
        }
    }

    fun triggerExportSvg() {
        ApplicationManager.getApplication().invokeLater {
            val descriptor = FileSaverDescriptor("Export as SVG", "Choose where to save the SVG file", "svg")
            val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
            val result = dialog.save(file.parent, file.nameWithoutExtension + ".svg")

            result?.let { wrapper ->
                previewPanel.exportAsSvg { svgContent ->
                    if (svgContent.isNotEmpty()) {
                        ApplicationManager.getApplication().invokeLater {
                            try {
                                wrapper.file.writeText(svgContent)
                            } catch (e: Exception) {
                                // Silently fail - could show notification in the future
                            }
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
                        }
                    } catch (e: Exception) {
                        // Silently fail - could show notification in the future
                    }
                }
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
