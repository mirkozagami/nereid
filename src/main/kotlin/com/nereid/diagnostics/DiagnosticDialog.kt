package com.nereid.diagnostics

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.datatransfer.StringSelection
import javax.swing.*

class DiagnosticDialog(
    project: Project?,
    private val bundle: DiagnosticBundle
) : DialogWrapper(project) {

    private val sectionCheckboxes = mutableMapOf<String, JBCheckBox>()
    private val sectionPanels = mutableMapOf<String, JPanel>()
    private val commentsField = JBTextArea(3, 50)

    private var includeEnvironment = true
    private var includeSettings = true
    private var includeRendering = true
    private var includeActions = true
    private var includeLogs = true
    private var includeDiagram = false

    init {
        title = "Mermaid Diagnostic Report"
        setOKButtonText("Copy to Clipboard")
        setCancelButtonText("Close")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(0, JBUI.scale(10)))
        mainPanel.preferredSize = Dimension(600, 500)

        // Header
        val headerLabel = JLabel("Review the information below before sharing.")
        headerLabel.border = JBUI.Borders.emptyBottom(10)
        mainPanel.add(headerLabel, BorderLayout.NORTH)

        // Sections panel
        val sectionsPanel = JPanel()
        sectionsPanel.layout = BoxLayout(sectionsPanel, BoxLayout.Y_AXIS)

        addCollapsibleSection(sectionsPanel, "Environment", bundle.environment.toReportString(), true) {
            includeEnvironment = it
        }

        val settingsContent = bundle.pluginSettings.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }
        addCollapsibleSection(sectionsPanel, "Plugin Settings", settingsContent, true) {
            includeSettings = it
        }

        addCollapsibleSection(sectionsPanel, "Mermaid Rendering Status", bundle.renderingStatus.toReportString(), true) {
            includeRendering = it
        }

        val actionsContent = bundle.recentActions.joinToString("\n") { it.toReportString() }
        addCollapsibleSection(sectionsPanel, "Recent Actions", actionsContent, true) {
            includeActions = it
        }

        val logsContent = bundle.ideLogs.joinToString("\n")
        addCollapsibleSection(sectionsPanel, "IDE Logs (Filtered)", logsContent, true) {
            includeLogs = it
        }

        addCollapsibleSection(
            sectionsPanel,
            "Diagram Source",
            bundle.diagramSource ?: "(No diagram source available)",
            false,
            warningText = "May contain sensitive information"
        ) {
            includeDiagram = it
        }

        val scrollPane = JBScrollPane(sectionsPanel)
        scrollPane.border = JBUI.Borders.empty()
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // Comments field
        val commentsPanel = JPanel(BorderLayout(0, JBUI.scale(5)))
        commentsPanel.add(JLabel("Additional comments (optional):"), BorderLayout.NORTH)
        commentsField.lineWrap = true
        commentsField.wrapStyleWord = true
        commentsPanel.add(JBScrollPane(commentsField), BorderLayout.CENTER)
        commentsPanel.border = JBUI.Borders.emptyTop(10)
        mainPanel.add(commentsPanel, BorderLayout.SOUTH)

        return mainPanel
    }

    private fun addCollapsibleSection(
        parent: JPanel,
        title: String,
        content: String,
        defaultChecked: Boolean,
        warningText: String? = null,
        onCheckChanged: (Boolean) -> Unit
    ) {
        val sectionPanel = JPanel(BorderLayout())
        sectionPanel.border = JBUI.Borders.emptyBottom(5)

        // Header with checkbox and collapse button
        val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))

        val collapseButton = JButton("\u25BC")
        collapseButton.preferredSize = Dimension(24, 24)
        collapseButton.isFocusable = false
        collapseButton.margin = JBUI.emptyInsets()

        val checkbox = JBCheckBox(title, defaultChecked)
        checkbox.addActionListener { onCheckChanged(checkbox.isSelected) }
        sectionCheckboxes[title] = checkbox

        headerPanel.add(collapseButton)
        headerPanel.add(checkbox)

        if (warningText != null) {
            val warningLabel = JLabel("\u26A0\uFE0F $warningText")
            warningLabel.foreground = JBColor.ORANGE
            warningLabel.font = warningLabel.font.deriveFont(Font.ITALIC, 11f)
            headerPanel.add(warningLabel)
        }

        sectionPanel.add(headerPanel, BorderLayout.NORTH)

        // Content area
        val contentArea = JBTextArea(content)
        contentArea.isEditable = false
        contentArea.lineWrap = true
        contentArea.wrapStyleWord = true
        contentArea.background = JBColor.background()
        contentArea.border = JBUI.Borders.empty(5)

        val contentPanel = JPanel(BorderLayout())
        contentPanel.add(JBScrollPane(contentArea), BorderLayout.CENTER)
        contentPanel.preferredSize = Dimension(550, 80)
        contentPanel.isVisible = true

        sectionPanel.add(contentPanel, BorderLayout.CENTER)
        sectionPanels[title] = contentPanel

        // Toggle collapse
        collapseButton.addActionListener {
            contentPanel.isVisible = !contentPanel.isVisible
            collapseButton.text = if (contentPanel.isVisible) "\u25BC" else "\u25B6"
            parent.revalidate()
        }

        parent.add(sectionPanel)
    }

    override fun createActions(): Array<Action> {
        val copyAction = object : DialogWrapperAction("Copy to Clipboard") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                copyToClipboard()
            }
        }

        val githubAction = object : DialogWrapperAction("Open GitHub Issue") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                openGitHubIssue()
            }
        }

        return arrayOf(copyAction, githubAction, cancelAction)
    }

    private fun getSelectedSections(): List<DiagnosticSection> {
        return bundle.getIncludedSections(
            includeEnvironment = includeEnvironment,
            includeSettings = includeSettings,
            includeRendering = includeRendering,
            includeActions = includeActions,
            includeLogs = includeLogs,
            includeDiagram = includeDiagram
        )
    }

    private fun getUserComments(): String? {
        val text = commentsField.text.trim()
        return if (text.isNotEmpty()) text else null
    }

    private fun copyToClipboard() {
        val sections = getSelectedSections()
        val body = GitHubIssueBuilder.buildReportBody(sections, getUserComments())

        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(body), null)

        close(OK_EXIT_CODE)
    }

    private fun openGitHubIssue() {
        val sections = getSelectedSections()
        val url = GitHubIssueBuilder.buildGitHubUrl(sections, getUserComments())

        BrowserUtil.browse(url)
        close(OK_EXIT_CODE)
    }
}
