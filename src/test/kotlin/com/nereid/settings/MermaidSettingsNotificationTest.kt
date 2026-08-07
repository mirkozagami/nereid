package com.nereid.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.ui.DialogPanel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Container
import javax.swing.JCheckBox

/**
 * Applying the settings page must tell open previews, or the page saves correctly and
 * nothing on screen changes until the file is reopened.
 *
 * Before #12 there was no notification: `apply()` wrote the value and told nobody, and
 * the only thing that refreshed a preview was a callback the toolbar's own dialog invoked
 * after closing. Deleting that dialog without this broadcast would have moved every user
 * onto the path where settings appear to do nothing.
 */
class MermaidSettingsNotificationTest : BasePlatformTestCase() {

    private val settings get() = MermaidSettings.getInstance()
    private lateinit var saved: MermaidSettings

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

    fun testApplyBroadcastsToSubscribers() {
        var notified = 0
        subscribe { notified++ }

        val configurable = MermaidSettingsConfigurable()
        try {
            val panel = configurable.createComponent() as DialogPanel
            panel.findCheckBox("Transparent PNG background").let { it.isSelected = !it.isSelected }
            configurable.apply()
        } finally {
            configurable.disposeUIResources()
        }

        assertEquals(
            "Applying the settings page did not publish MermaidSettingsListener.TOPIC, so " +
                "open previews would keep rendering with the old settings",
            1,
            notified
        )
    }

    /**
     * Subscribers read the settings object directly, so the broadcast has to come after
     * the panel has written its values back -- not before.
     */
    fun testSubscribersSeeTheNewValueNotTheOld() {
        val before = settings.pngTransparentBackground
        var observed: Boolean? = null
        subscribe { observed = settings.pngTransparentBackground }

        val configurable = MermaidSettingsConfigurable()
        try {
            val panel = configurable.createComponent() as DialogPanel
            panel.findCheckBox("Transparent PNG background").isSelected = !before
            configurable.apply()
        } finally {
            configurable.disposeUIResources()
        }

        assertEquals(
            "The broadcast fired before the panel wrote its values back, so subscribers " +
                "read stale settings",
            !before,
            observed
        )
    }

    fun testNotifyChangedReachesSubscribers() {
        var notified = false
        subscribe { notified = true }

        MermaidSettingsListener.notifyChanged()

        assertTrue(
            "MermaidSettingsListener.notifyChanged() did not reach subscribers -- the " +
                "toolbar dropdowns rely on it",
            notified
        )
    }

    /** Subscription is tied to the test's disposable, so it never leaks into another test. */
    private fun subscribe(onChanged: () -> Unit) {
        val disposable = Disposable { }
        Disposer.register(testRootDisposable, disposable)
        ApplicationManager.getApplication().messageBus
            .connect(disposable)
            .subscribe(
                MermaidSettingsListener.TOPIC,
                object : MermaidSettingsListener {
                    override fun settingsChanged() = onChanged()
                }
            )
    }

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
