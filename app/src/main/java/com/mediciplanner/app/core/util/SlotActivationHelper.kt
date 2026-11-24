package com.mediciplanner.app.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

object SlotActivationHelper {
    const val ACTIVATION_WINDOW_MINUTES: Long = 30L

    /**
     * Parses a scheduled time string (e.g., "08:00", "19:30", "08:00:00",
     * "2026-11-18T08:00:00", or "07:00 PM") into a [LocalTime].
     */
    fun parseScheduledTime(timeString: String?): LocalTime? {
        if (timeString.isNullOrBlank()) return null
        val trimmed = timeString.trim()

        val rawTime = when {
            trimmed.contains("T") -> trimmed.substringAfter("T")
            trimmed.matches(Regex("^\\d{4}-\\d{2}-\\d{2}\\s+.*")) -> trimmed.substringAfter(" ").trim()
            else -> trimmed
        }

        val upper = rawTime.uppercase(Locale.US)
        val hasAm = upper.endsWith("AM")
        val hasPm = upper.endsWith("PM")
        val numericPart = when {
            hasAm -> rawTime.dropLast(2).trim()
            hasPm -> rawTime.dropLast(2).trim()
            else -> rawTime
        }

        val parts = numericPart.split(":")
        if (parts.size < 2) return null

        val parsedHour = parts[0].trim().toIntOrNull() ?: return null
        val parsedMinute = parts[1].trim().take(2).toIntOrNull() ?: return null
        if (parsedMinute !in 0..59) return null

        val hour24 = when {
            hasAm -> {
                if (parsedHour !in 0..12) return null
                if (parsedHour == 12) 0 else parsedHour
            }
            hasPm -> {
                if (parsedHour !in 1..12) return null
                if (parsedHour == 12) 12 else parsedHour + 12
            }
            else -> {
                if (parsedHour !in 0..23) return null
                parsedHour
            }
        }

        return LocalTime.of(hour24, parsedMinute)
    }

    /**
     * Calculates `isSlotActive = (currentTime >= scheduledTime - 30 min)`.
     */
    fun isSlotActive(
        scheduledTime: String?,
        currentTime: LocalDateTime = LocalDateTime.now(),
        scheduledDate: LocalDate = currentTime.toLocalDate()
    ): Boolean {
        val parsedTime = parseScheduledTime(scheduledTime) ?: return true
        val scheduledDateTime = LocalDateTime.of(scheduledDate, parsedTime)
        val activationTime = scheduledDateTime.minusMinutes(ACTIVATION_WINDOW_MINUTES)
        return !currentTime.isBefore(activationTime)
    }

    fun isSlotActive(
        scheduledTime: LocalTime,
        currentTime: LocalTime = LocalTime.now()
    ): Boolean {
        val today = LocalDate.now()
        val scheduledDateTime = LocalDateTime.of(today, scheduledTime)
        val activationTime = scheduledDateTime.minusMinutes(ACTIVATION_WINDOW_MINUTES)
        val currentDateTime = LocalDateTime.of(today, currentTime)
        return !currentDateTime.isBefore(activationTime)
    }
}
