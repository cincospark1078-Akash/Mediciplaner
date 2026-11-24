package com.mediciplanner.app.data.util

import java.nio.charset.StandardCharsets
import java.util.UUID

object ReminderIdGenerator {
    /**
     * Generates a deterministic UUID based on the dose schedule ID and scheduled date.
     * This ensures that editing a dose time does not orphan old reminders.
     */
    fun generate(doseScheduleId: String, scheduledDate: String): String {
        val key = "$doseScheduleId|$scheduledDate"
        return UUID.nameUUIDFromBytes(key.toByteArray(StandardCharsets.UTF_8)).toString()
    }
}
