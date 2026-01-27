package com.nereid.diagnostics

import java.util.concurrent.ConcurrentLinkedDeque

object ActionLogger {
    private const val MAX_ENTRIES = 20
    private val entries = ConcurrentLinkedDeque<ActionLogEntry>()

    fun log(action: String) {
        val entry = ActionLogEntry(
            timestamp = System.currentTimeMillis(),
            action = action
        )
        entries.addLast(entry)

        while (entries.size > MAX_ENTRIES) {
            entries.pollFirst()
        }
    }

    fun getRecentActions(): List<ActionLogEntry> {
        return entries.toList()
    }

    fun clear() {
        entries.clear()
    }
}
