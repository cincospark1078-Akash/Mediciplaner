package com.mediciplanner.app.core.util

import java.util.Locale

object TimeFormatHelper {
    /**
     * Formats a time string (e.g., "19:00", "19:00:00", "2026-11-12T19:00:00", or "07:00 PM")
     * into 12-hour format with AM/PM ("07:00 PM") or 24-hour format ("19:00").
     */
    fun formatTime(timeString: String, is24Hour: Boolean): String {
        val trimmed = timeString.trim()
        if (trimmed.isBlank()) return ""

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
        if (parts.size < 2) return timeString

        val parsedHour = parts[0].trim().toIntOrNull() ?: return timeString
        val parsedMinute = parts[1].trim().take(2).toIntOrNull() ?: return timeString
        if (parsedMinute !in 0..59) return timeString

        val hour24 = when {
            hasAm -> {
                if (parsedHour !in 0..12) return timeString
                if (parsedHour == 12) 0 else parsedHour
            }
            hasPm -> {
                if (parsedHour !in 1..12) return timeString
                if (parsedHour == 12) 12 else parsedHour + 12
            }
            else -> {
                if (parsedHour !in 0..23) return timeString
                parsedHour
            }
        }

        return if (is24Hour) {
            String.format(Locale.US, "%02d:%02d", hour24, parsedMinute)
        } else {
            val amPm = if (hour24 < 12) "AM" else "PM"
            val displayHour = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            String.format(Locale.US, "%02d:%02d %s", displayHour, minuteOrZero(parsedMinute), amPm)
        }
    }

    private fun minuteOrZero(minute: Int): Int = minute.coerceIn(0, 59)
}

/**
 * Top-level convenience delegate to [TimeFormatHelper.formatTime].
 */
fun formatTime(timeString: String, is24Hour: Boolean): String =
    TimeFormatHelper.formatTime(timeString, is24Hour)
