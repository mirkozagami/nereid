package com.nereid.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory

class DebouncedDocumentListener(
    private val delayMs: Int = 300,
    private val onUpdate: () -> Unit,
    parentDisposable: Disposable
) : DocumentListener {

    private val alarm: Alarm = AlarmFactory.getInstance().create(Alarm.ThreadToUse.SWING_THREAD, parentDisposable)

    override fun documentChanged(event: DocumentEvent) {
        alarm.cancelAllRequests()
        alarm.addRequest({
            ApplicationManager.getApplication().invokeLater {
                onUpdate()
            }
        }, delayMs)
    }

    fun setDelay(delayMs: Int) {
        // Note: Would need to recreate listener for new delay
    }

    fun forceUpdate() {
        alarm.cancelAllRequests()
        onUpdate()
    }
}
