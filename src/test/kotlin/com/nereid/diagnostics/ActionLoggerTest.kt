package com.nereid.diagnostics

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ActionLoggerTest {

    @Before
    fun setUp() {
        ActionLogger.clear()
    }

    @Test
    fun testLogAction() {
        ActionLogger.log("Opened file: test.mmd")

        val entries = ActionLogger.getRecentActions()

        assertEquals(1, entries.size)
        assertEquals("Opened file: test.mmd", entries[0].action)
    }

    @Test
    fun testMaxEntriesLimit() {
        repeat(25) { i ->
            ActionLogger.log("Action $i")
        }

        val entries = ActionLogger.getRecentActions()

        assertEquals(20, entries.size)
        assertEquals("Action 5", entries[0].action) // Oldest retained
        assertEquals("Action 24", entries[19].action) // Most recent
    }

    @Test
    fun testClear() {
        ActionLogger.log("Test action")
        ActionLogger.clear()

        val entries = ActionLogger.getRecentActions()

        assertTrue(entries.isEmpty())
    }
}
