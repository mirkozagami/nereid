package com.nereid.preview

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * `debounceDelayMs` offers a 0-2000 ms slider on the settings page and was read by
 * nothing but the diagnostics bundle -- `MermaidSplitEditor` constructed this listener
 * with a hardcoded `delayMs = 300` (#39).
 *
 * The listener also carried a `setDelay()` whose body was a comment saying the listener
 * would need recreating. It did not: `Alarm.addRequest` takes the delay per call, so
 * reading it at event time is enough. The delay is now supplied as a function, which
 * makes a settings change take effect on the next keystroke with no re-registration and
 * no listener for this panel to maintain.
 */
class DebouncedDocumentListenerTest : BasePlatformTestCase() {

    fun testDelayIsReadForEveryChangeRatherThanCapturedAtConstruction() {
        val asked = mutableListOf<Int>()
        var configured = 500

        val listener = DebouncedDocumentListener(
            delayMs = { asked.add(configured); configured },
            onUpdate = {},
            parentDisposable = testRootDisposable
        )

        val file = myFixture.configureByText("debounce.mmd", "flowchart LR\n")
        val document = myFixture.getDocument(file)
        document.addDocumentListener(listener, testRootDisposable)

        WriteCommandAction.runWriteCommandAction(project) { document.insertString(0, "A") }

        assertEquals(
            "The listener did not consult the delay when the document changed",
            listOf(500),
            asked
        )

        configured = 50
        WriteCommandAction.runWriteCommandAction(project) { document.insertString(0, "B") }

        assertEquals(
            "The delay was captured once instead of read per change, so changing the " +
                "slider in Settings cannot take effect until the editor is reopened (#39)",
            listOf(500, 50),
            asked
        )
    }
}
