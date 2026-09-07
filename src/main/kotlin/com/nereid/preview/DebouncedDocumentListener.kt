package com.nereid.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.util.Alarm

/**
 * Coalesces document changes into one preview render.
 *
 * [delayMs] is a function rather than a value because the delay is a user setting
 * (`debounceDelayMs`) that can change while an editor is open. `Alarm.addRequest` takes
 * the delay per call, so reading it here is enough -- the listener never needs
 * re-registering, which is what the previous no-op `setDelay()` wrongly assumed.
 */
class DebouncedDocumentListener(
    private val delayMs: () -> Int,
    private val onUpdate: () -> Unit,
    parentDisposable: Disposable
) : DocumentListener {

    // AlarmFactory is deprecated and scheduled for removal; the Alarm constructor is
    // the direct replacement and behaves identically.
    private val alarm: Alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, parentDisposable)

    override fun documentChanged(event: DocumentEvent) {
        alarm.cancelAllRequests()
        alarm.addRequest({
            ApplicationManager.getApplication().invokeLater {
                onUpdate()
            }
        }, delayMs())
    }

    fun forceUpdate() {
        alarm.cancelAllRequests()
        onUpdate()
    }
}
