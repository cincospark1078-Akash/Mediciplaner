package com.mediciplanner.app.core.util

import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Utility for parsing and formatting medicine dose amounts, supporting integers,
 * decimals (e.g., "0.5", "1.5"), and fractions (e.g., "1/2", "1 1/2", "১/২").
 */
object DoseAmountHelper {

    fun normalizeDigits(input: String): String {
        return buildString(input.length) {
            for (ch in input) {
                if (ch in '০'..'৯') {
                    append(('0' + (ch - '০')))
                } else {
                    append(ch)
                }
            }
        }
    }

    /**
     * Filters raw text input from the dose field so the user can enter
     * digits (ASCII or Bengali), '.', '/', and spaces (for mixed fractions),
     * while preventing multiple slashes or stray invalid characters.
     */
    fun sanitizeDoseInput(input: String): String {
        val sb = StringBuilder(input.length)
        var slashCount = 0
        for (ch in input) {
            when {
                ch.isDigit() || ch == '.' -> sb.append(ch)
                ch == '/' -> {
                    if (slashCount == 0 && sb.isNotEmpty()) {
                        sb.append(ch)
                        slashCount++
                    }
                }
                ch == ' ' -> {
                    if (sb.isNotEmpty() && sb.last() != ' ') {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Parses a dose string into a positive Double, supporting:
     * - Whole numbers: "1", "2", "১" -> 1.0, 2.0, 1.0
     * - Decimals: "0.5", "1.5", ".5" -> 0.5, 1.5, 0.5
     * - Fractions: "1/2", "1/4", "3/4", "3/2" -> 0.5, 0.25, 0.75, 1.5
     * - Mixed fractions: "1 1/2" -> 1.5
     *
     * Returns null if blank, malformed, zero, or negative.
     */
    fun parseDoseAmount(input: String?): Double? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeDigits(input).trim()
        if (normalized.isEmpty()) return null

        val value: Double? = if (normalized.contains('/')) {
            val spaceParts = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
            when (spaceParts.size) {
                1 -> parseSimpleFraction(spaceParts[0])
                2 -> {
                    val whole = spaceParts[0].toDoubleOrNull()
                    val frac = parseSimpleFraction(spaceParts[1])
                    if (whole != null && whole >= 0.0 && frac != null) {
                        whole + frac
                    } else {
                        null
                    }
                }
                else -> null
            }
        } else {
            normalized.toDoubleOrNull()
        }

        return if (value != null && value.isFinite() && value > 0.0) value else null
    }

    private fun parseSimpleFraction(token: String): Double? {
        val parts = token.split('/')
        if (parts.size != 2) return null
        val numerator = parts[0].trim().toDoubleOrNull() ?: return null
        val denominator = parts[1].trim().toDoubleOrNull() ?: return null
        if (!numerator.isFinite() || !denominator.isFinite() || denominator <= 0.0 || numerator <= 0.0) {
            return null
        }
        return numerator / denominator
    }

    /**
     * Formats a Double dose amount for UI display, converting common fractional doses
     * like 0.5 -> "1/2" (or "১/২" in Bengali).
     */
    fun formatDoseAmount(amount: Double, useBengaliDigits: Boolean = true): String {
        if (!amount.isFinite() || amount <= 0.0) {
            val fallback = "1"
            return if (useBengaliDigits) toBengaliDigits(fallback) else fallback
        }

        val nearestInt = amount.roundToInt()
        val rawFormatted = when {
            abs(amount - nearestInt) < 1e-4 -> nearestInt.toString()
            abs(amount - 0.5) < 1e-4 -> "1/2"
            abs(amount - 0.25) < 1e-4 -> "1/4"
            abs(amount - 0.75) < 1e-4 -> "3/4"
            abs(amount - (1.0 / 3.0)) < 0.01 -> "1/3"
            abs(amount - (2.0 / 3.0)) < 0.01 -> "2/3"
            else -> BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }

        return if (useBengaliDigits) toBengaliDigits(rawFormatted) else rawFormatted
    }
}
