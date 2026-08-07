package com.nereid.settings

import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Container
import javax.swing.JCheckBox

/**
 * Guards the defect fixed in fcd0a55, where the toolbar's settings dialog discarded every
 * change. Its `bindXxx()` calls copy the UI values into backing fields only when
 * `DialogPanel.apply()` runs; without that call, `doOKAction()` wrote the untouched
 * initial values straight back, so pressing OK saved nothing.
 *
 * Construction alone also guards the initialisation-order NPE that shipped during
 * development: `DialogWrapper.init()` synchronously calls `createCenterPanel()`, and
 * Kotlin initialises properties in declaration order, so declaring `tabPanels` after the
 * init block leaves it null exactly when the panel is being built.
 */
class MermaidSettingsDialogTest : BasePlatformTestCase() {

    private val settings get() = MermaidSettings.getInstance()
    private lateinit var saved: MermaidSettings

    // Application-level service shared across the whole run -- snapshot and restore.
    override fun setUp() {
        super.setUp()
        saved = MermaidSettings().also { XmlSerializerUtil.copyBean(settings, it) }
    }

    override fun tearDown() {
        try {
            XmlSerializerUtil.copyBean(saved, settings)
        } finally {
            super.tearDown()
        }
    }

    fun testConstructionPopulatesTheTabPanels() {
        withDialog { dialog ->
            val panels = dialog.tabPanels()

            // The init-order NPE left this list null while createCenterPanel() was
            // running, so a populated list is the direct evidence that it is declared
            // before the init block and the tabs were actually built.
            assertEquals(
                "Expected the General, Export and Advanced tab panels to be retained for " +
                    "doOKAction() to apply",
                3,
                panels.size
            )

            val checkboxes = panels.flatMap { it.checkBoxes() }.map { it.text }
            assertTrue(
                "Expected controls from the General and Export tabs, found: $checkboxes",
                checkboxes.containsAll(
                    listOf("Enable mouse wheel zoom", "Transparent PNG background")
                )
            )
        }
    }

    fun testOkPersistsACheckboxChange() {
        val before = settings.pngTransparentBackground

        withDialog { dialog ->
            dialog.findCheckBox("Transparent PNG background").isSelected = !before
            dialog.performOKAction()
        }

        assertEquals(
            "Pressing OK did not save the toggled 'Transparent PNG background'. " +
                "doOKAction() must call DialogPanel.apply() on each tab before copying the " +
                "backing fields into MermaidSettings, or it writes back the untouched " +
                "initial values.",
            !before,
            settings.pngTransparentBackground
        )
    }

    fun testOkLeavesUntouchedSettingsAlone() {
        val before = settings.debounceDelayMs

        withDialog { dialog ->
            dialog.findCheckBox("Transparent PNG background").let {
                it.isSelected = !it.isSelected
            }
            dialog.performOKAction()
        }

        assertEquals(
            "Saving the dialog changed a setting the user never touched",
            before,
            settings.debounceDelayMs
        )
    }

    private fun withDialog(block: (MermaidSettingsDialog) -> Unit) {
        val dialog = MermaidSettingsDialog(project)
        try {
            block(dialog)
        } finally {
            Disposer.dispose(dialog.disposable)
        }
    }

    /**
     * Reads the dialog's retained tab panels reflectively.
     *
     * Headless, `DialogWrapper` never creates a peer, so `getContentPanel()` is null and
     * there is no public route to the components. These are the very panels
     * `doOKAction()` calls `apply()` on, so asserting against them tests the real
     * round-trip rather than a rebuilt copy.
     */
    @Suppress("UNCHECKED_CAST")
    private fun MermaidSettingsDialog.tabPanels(): List<DialogPanel> {
        val field = MermaidSettingsDialog::class.java.getDeclaredField("tabPanels")
        field.isAccessible = true
        return (field.get(this) as? List<DialogPanel>)
            ?: throw AssertionError(
                "MermaidSettingsDialog.tabPanels was null after construction -- the " +
                    "initialisation-order bug is back: DialogWrapper.init() calls " +
                    "createCenterPanel() synchronously, so tabPanels must be declared " +
                    "above the init block"
            )
    }

    private fun MermaidSettingsDialog.findCheckBox(text: String): JCheckBox =
        tabPanels().flatMap { it.checkBoxes() }.firstOrNull { it.text == text }
            ?: throw AssertionError(
                "No checkbox labelled '$text' in the dialog. Found: " +
                    tabPanels().flatMap { it.checkBoxes() }.map { it.text }
            )

    private fun Container.checkBoxes(): List<JCheckBox> =
        components.flatMap { child ->
            when (child) {
                is JCheckBox -> listOf(child)
                is Container -> child.checkBoxes()
                else -> emptyList()
            }
        }
}
