package com.nereid.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

/**
 * Broadcast whenever Nereid settings change, so open previews can pick the change up
 * without being reopened.
 *
 * Before this existed there was no notification at all. The editor toolbar's gear button
 * faked one by invoking a callback after its own dialog closed, which meant two things:
 *
 *  - Changing a setting through **Settings > Tools > Nereid** did nothing visible until
 *    the file was reopened. `MermaidSettingsConfigurable.apply()` wrote the value and told
 *    nobody, and `ThemeManager` only listens for IDE look-and-feel changes.
 *  - The toolbar's theme and background dropdowns refreshed only their own editor's
 *    preview, so with two diagrams open the other one kept the old theme.
 *
 * An application-level topic fixes both: every open [com.nereid.preview.MermaidPreviewPanel]
 * subscribes, and every writer publishes, so there is one notification path rather than
 * one per caller.
 */
interface MermaidSettingsListener {

    fun settingsChanged()

    companion object {
        @JvmField
        val TOPIC: Topic<MermaidSettingsListener> =
            Topic.create("Nereid settings changed", MermaidSettingsListener::class.java)

        /**
         * Publishes to every subscriber. Call after the settings object has been updated,
         * never before -- subscribers read the new values synchronously.
         */
        fun notifyChanged() {
            ApplicationManager.getApplication()
                .messageBus
                .syncPublisher(TOPIC)
                .settingsChanged()
        }
    }
}
