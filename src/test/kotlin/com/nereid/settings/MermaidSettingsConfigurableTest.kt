package com.nereid.settings

import com.intellij.openapi.ui.DialogPanel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Container
import javax.swing.JCheckBox
import javax.swing.JTextArea

/**
 * Guards the defect fixed in 116c6b3, where `apply()` on the settings page was empty, so
 * none of the fifteen settings was ever saved, and `isModified()` returned `panel != null`,
 * leaving Apply permanently enabled while doing nothing.
 *
 * The Kotlin UI DSL's `bindXxx()` calls do not write back on their own -- only
 * `DialogPanel.apply()` does -- which is exactly the kind of unwired binding that static
 * analysis cannot see. The 11-shard verification matrix was fully green throughout.
 */
class MermaidSettingsConfigurableTest : BasePlatformTestCase() {

    private val settings get() = MermaidSettings.getInstance()
    private lateinit var saved: MermaidSettings

    // MermaidSettings is an application service, so it is shared by every test in the
    // run. Snapshot and restore it, or whichever test runs next inherits our edits.
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

    fun testApplyPersistsACheckboxChange() {
        val configurable = MermaidSettingsConfigurable()
        try {
            val panel = configurable.createComponent() as DialogPanel
            val before = settings.pngTransparentBackground

            val checkbox = panel.findCheckBox("Transparent PNG background")
            checkbox.isSelected = !before
            configurable.apply()

            assertEquals(
                "Toggling 'Transparent PNG background' and calling apply() did not reach " +
                    "MermaidSettings. The Kotlin UI DSL only writes bound properties back " +
                    "when DialogPanel.apply() is called.",
                !before,
                settings.pngTransparentBackground
            )
        } finally {
            configurable.disposeUIResources()
        }
    }

    fun testIsModifiedTracksThePanelRatherThanItsExistence() {
        val configurable = MermaidSettingsConfigurable()
        try {
            val panel = configurable.createComponent() as DialogPanel

            assertFalse(
                "A freshly created panel reports itself modified. isModified() previously " +
                    "returned 'panel != null', which left Apply permanently enabled.",
                configurable.isModified
            )

            panel.findCheckBox("Transparent PNG background").let { it.isSelected = !it.isSelected }

            assertTrue(
                "Changing a control did not mark the page modified, so Apply would stay " +
                    "greyed out and the change could not be saved",
                configurable.isModified
            )
        } finally {
            configurable.disposeUIResources()
        }
    }

    fun testApplyPersistsCustomCss() {
        val configurable = MermaidSettingsConfigurable()
        try {
            val panel = configurable.createComponent() as DialogPanel
            val css = "#diagram svg .node rect { stroke-width: 2px; }"

            panel.findTextArea().text = css
            configurable.apply()

            assertEquals(
                "Custom CSS typed on the settings page did not reach MermaidSettings",
                css,
                settings.customCss
            )
        } finally {
            configurable.disposeUIResources()
        }
    }

    /**
     * Clearing the field has to clear the setting, so a user can remove CSS and not only
     * add it.
     *
     * This covers the settings half only. The preview half -- `applyCustomCss` being
     * called even when the value is blank, rather than behind an `isNotBlank()` guard --
     * runs through JCEF and cannot be exercised headlessly.
     */
    fun testApplyPersistsClearingCustomCss() {
        settings.customCss = "#diagram svg { opacity: 0.5; }"

        val configurable = MermaidSettingsConfigurable()
        try {
            val panel = configurable.createComponent() as DialogPanel
            panel.findTextArea().text = ""
            configurable.apply()

            assertEquals("Clearing the custom CSS field did not clear the setting", "", settings.customCss)
        } finally {
            configurable.disposeUIResources()
        }
    }

    private fun Container.findTextArea(): JTextArea =
        textAreas().firstOrNull()
            ?: throw AssertionError("No text area on the settings page")

    private fun Container.textAreas(): List<JTextArea> =
        components.flatMap { child ->
            when (child) {
                is JTextArea -> listOf(child)
                is Container -> child.textAreas()
                else -> emptyList()
            }
        }

    /**
     * The DSL nests controls several panels deep, so the checkbox is found by walking the
     * tree rather than by index -- an index would silently drift as rows are added.
     */
    private fun Container.findCheckBox(text: String): JCheckBox =
        checkBoxes().firstOrNull { it.text == text }
            ?: throw AssertionError(
                "No checkbox labelled '$text' on the settings page. Found: " +
                    checkBoxes().map { it.text }
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
