package com.nereid.diagnostics

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel

class DiagnosticNotifier {

    private var notificationBar: JPanel? = null
    private var parentPanel: JPanel? = null
    private var lastError: String? = null

    fun createNotificationBar(): JPanel {
        val bar = JPanel(BorderLayout())
        bar.background = JBColor(0xFFF3CD, 0x3D3520)
        bar.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor(0xFFECB5, 0x5A4A20), 0, 0, 1, 0),
            JBUI.Borders.empty(8, 12)
        )
        bar.isVisible = false

        notificationBar = bar
        return bar
    }

    fun attachToPanel(parent: JPanel) {
        parentPanel = parent
    }

    fun showError(
        project: Project?,
        message: String,
        errorContext: String? = null,
        diagramSource: String? = null
    ) {
        lastError = message

        val bar = notificationBar ?: return
        bar.removeAll()

        // Warning icon and message
        val messagePanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        messagePanel.isOpaque = false

        val iconLabel = JLabel("\u26A0\uFE0F")
        val messageLabel = JLabel(message)

        messagePanel.add(iconLabel)
        messagePanel.add(messageLabel)

        // Report link
        val reportLink = JLabel("Report Issue")
        reportLink.foreground = JBColor.BLUE
        reportLink.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        reportLink.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                openDiagnosticDialog(project, errorContext, diagramSource)
            }
        })

        // Close button
        val closeButton = JLabel("\u2715")
        closeButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        closeButton.border = JBUI.Borders.emptyLeft(10)
        closeButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                hide()
            }
        })

        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0))
        rightPanel.isOpaque = false
        rightPanel.add(reportLink)
        rightPanel.add(closeButton)

        bar.add(messagePanel, BorderLayout.WEST)
        bar.add(rightPanel, BorderLayout.EAST)

        bar.isVisible = true
        bar.revalidate()
        bar.repaint()
    }

    fun hide() {
        notificationBar?.isVisible = false
        lastError = null
    }

    fun getLastError(): String? = lastError

    private fun openDiagnosticDialog(
        project: Project?,
        errorContext: String?,
        diagramSource: String?
    ) {
        val collector = DiagnosticCollector()
        val bundle = collector.collect(
            lastRenderError = lastError,
            diagramSource = diagramSource,
            errorContext = errorContext
        )

        val dialog = DiagnosticDialog(project, bundle)
        dialog.show()
    }
}
