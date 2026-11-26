package com.mediciplanner.app.core.util

import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine
import com.mediciplanner.app.ui.screens.prescription.DraftSchedule

import java.math.BigDecimal

data class ParsedDose(
    val slot: DoseSlot,
    val doseAmount: Double
)

data class ParsedMedicine(
    val name: String,
    val dose: String? = null,
    val foodRule: FoodRule? = null,
    val rawText: String = "",
    val duration: String? = null,
    val strength: String? = null,
    val parsedDoses: List<ParsedDose> = OcrParser.parseDosePattern(dose)
)

object OcrParser {

    // Single dose token supporting integers, decimals (0.5, 1.5), and fractions (1/2)
    private const val DOSE_TOKEN_PATTERN = """\d+(?:\.\d+|/\d+)?"""

    // Noise lines starting with "S+", "S +", or "+" (e.g., "S+ fD", "+ SfD")
    private val leadingPlusNoiseRegex = Regex(
        """^\s*(?:S\s*\+|\+)""",
        RegexOption.IGNORE_CASE
    )

    // Lines that contain ONLY dosage-like patterns (e.g., "0+0", "0 + 0", "0+0+0", "1+0+1", "1/2+1/2+1/2")
    private val dosageOnlyLineRegex = Regex(
        """^\s*$DOSE_TOKEN_PATTERN(?:\s*[+\-]\s*$DOSE_TOKEN_PATTERN){1,3}\s*$"""
    )

    // All-zero dosage patterns (e.g., "0+0", "0 + 0", "0+0+0", "0.0+0.0+0.0")
    private val allZeroDosageRegex = Regex(
        """^\s*0+(?:\.0+)?(?:\s*[+\-]\s*0+(?:\.0+)?)+\s*$"""
    )

    // Matches 3-part and 4-part dosages including fractions/decimals in any position (e.g., "0+1/2+0", "1/2+1/2+1/2", "1+1+1+1", "1-0-1")
    private val threePartDoseRegex = Regex(
        """(?<![\d./])$DOSE_TOKEN_PATTERN(?:\s*[+\-]\s*$DOSE_TOKEN_PATTERN){2,3}(?![\d./])"""
    )

    private val doseRegex = Regex(
        """(?<![\d./])$DOSE_TOKEN_PATTERN(?:\s*[+\-]\s*$DOSE_TOKEN_PATTERN){1,3}(?![\d./])"""
    )

    private val durationRegex = Regex(
        """(\d+)\s*(days?|d\b|দিন|weeks?|wks?|wk|সপ্তাহ|months?|mos?|mo|মাস)""",
        RegexOption.IGNORE_CASE
    )

    private val strengthRegex = Regex(
        """\b\d+(?:\.\d+)?\s*(?:mg/5\s*ml|mg/ml|mcg/puff|iu/ml|mg/g|mg|mcg|ml|gm|g|iu|%)\b""",
        RegexOption.IGNORE_CASE
    )

    // Trailing combination strength pattern (e.g., "75 mg + 75 mg", "500mg + 125mg", "75+75 mg")
    private val trailingCombinationStrengthRegex = Regex(
        """(?:\s*[\(\[\{]?\s*)(\d+(?:\.\d+)?\s*(?:mg/5\s*ml|mg/ml|mcg/puff|iu/ml|mg/g|mg|mcg|ml|gm|g|iu|%)?(?:\s*\+\s*\d+(?:\.\d+)?\s*(?:mg/5\s*ml|mg/ml|mcg/puff|iu/ml|mg/g|mg|mcg|ml|gm|g|iu|%))+)\s*[\)\]\}]?\s*$""",
        RegexOption.IGNORE_CASE
    )

    // Required medicine keywords ("Tab.", "Cap.", "Syp.", "Spray", "Inj.", "Drop", "mg", "mcg") or known medicine suffixes
    private val medicineKeywordRegex = Regex(
        """(?:^|\b)(?:tab(?:let)?|cap(?:sule)?|syp|syrup|susp(?:ension)?|spray|inj(?:ection)?|drops?|inhaler|oint(?:ment)?|cream|gel)\.?(?:\b|$)|\b\d*(?:\.\d+)?\s*(?:mg/5\s*ml|mg/ml|mcg/puff|iu/ml|mg/g|mg|mcg)\b""",
        RegexOption.IGNORE_CASE
    )

    private val knownMedicineSuffixRegex = Regex(
        """\b[a-z]{3,}(?:cillin|mycin|floxacin|xacin|prazole|tidine|statin|sartan|dipine|pril|olol|gliptin|formin|azole|zepam|zolam|phylline|lukast|fenac|profen|coxib|tadine|rizine|mide|done|sone|lone)\b""",
        RegexOption.IGNORE_CASE
    )

    private val formPrefixRegex = Regex(
        """^(?:tab(?:let)?|cap(?:sule)?|syp|syrup|susp(?:ension)?|spray|inj(?:ection)?|drops?|inhaler|oint(?:ment)?|cream|gel)\.?\b""",
        RegexOption.IGNORE_CASE
    )

    private val leadingListMarkerRegex = Regex(
        """^(?:rx\b\.?:?\s*|\d+\s*[.)]\s*|[•*\-—–]+\s*)+""",
        RegexOption.IGNORE_CASE
    )

    // Ignore lines containing clinic/patient/vitals metadata
    private val ignoredLineKeywordsRegex = Regex(
        """\b(?:serial|phone|mob|mobile|tel|email|e-mail|date|bp|b\.p\.|pulse|diagnosis|dx|age|hospital|clinic|chamber|address|patient|dr\.?|prof\.?|mbbs|fcps)\b""",
        RegexOption.IGNORE_CASE
    )

    private val beforeFoodRegex = Regex(
        """(?:খাওয়ার\s*আগে|খাবার\s*আগে|খাবারের\s*আগে|\bbefore\s*(?:meal|food)\b|\ba\.?c\.?\b)""",
        RegexOption.IGNORE_CASE
    )

    private val afterFoodRegex = Regex(
        """(?:খাওয়ার\s*পরে|খাবার\s*পরে|খাবারের\s*পরে|\bafter\s*(?:meal|food)\b|\bp\.?c\.?\b)""",
        RegexOption.IGNORE_CASE
    )

    private val emptyStomachRegex = Regex(
        """(?:খালি\s*পেটে|খালিপেটে|\bempty\s*stomach\b)""",
        RegexOption.IGNORE_CASE
    )

    private val withFoodRegex = Regex(
        """(?:খাওয়ার\s*সাথে|খাবার\s*সাথে|খাবারের\s*সাথে|\bwith\s*(?:meal|food)\b)""",
        RegexOption.IGNORE_CASE
    )

    private val stopWordsOnlyRegex = Regex(
        """^(?:for|x|×|take|after|before|with|meal|food|daily|once|twice|thrice|or|and|bd|tds|od|qid|hs|sos|stat|খাওয়ার\s*আগে|খাওয়ার\s*পরে|খাবারের\s*আগে|খাবারের\s*পরে|খালি\s*পেটে|খালিপেটে|প্রতিদিন|\s|[+\-—–,.;:()/\[\]])+$""",
        RegexOption.IGNORE_CASE
    )

    fun isMedicineKeywordLine(text: String): Boolean {
        val normalized = normalizeBengaliDigits(text)
        return medicineKeywordRegex.containsMatchIn(normalized) ||
            knownMedicineSuffixRegex.containsMatchIn(normalized)
    }

    fun isNoiseLine(text: String): Boolean {
        val trimmed = normalizeBengaliDigits(text).trim()
        if (trimmed.isEmpty()) return true
        if (leadingPlusNoiseRegex.containsMatchIn(trimmed)) return true
        if (allZeroDosageRegex.matches(trimmed)) return true
        if (isIgnoredLine(trimmed)) return true
        return false
    }

    fun isValidParsedMedicine(medicine: ParsedMedicine): Boolean {
        val trimmedName = normalizeBengaliDigits(medicine.name).trim()
        if (trimmedName.isEmpty()) return false
        if (isNoiseLine(trimmedName)) return false
        if (dosageOnlyLineRegex.matches(trimmedName)) return false
        if (medicine.rawText.isNotBlank() && isNoiseLine(medicine.rawText)) return false
        return isMedicineKeywordLine(trimmedName) ||
            (medicine.rawText.isNotBlank() && isMedicineKeywordLine(medicine.rawText))
    }

    fun parseMedicines(text: String): List<ParsedMedicine> {
        if (text.isBlank()) return emptyList()

        val results = mutableListOf<MutableParsedEntry>()
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (rawLine in lines) {
            val normalizedLine = normalizeBengaliDigits(rawLine)

            // Ignore noise lines ("S+", "S +", "+", "0+0", "0 + 0") and metadata lines
            if (isNoiseLine(normalizedLine)) {
                continue
            }

            // If line contains ONLY a dosage-like pattern (e.g., "1+0+0"), never create a standalone medicine;
            // only attach it as a continuation dose to a previously accepted valid medicine.
            if (dosageOnlyLineRegex.matches(normalizedLine)) {
                val cleanDose = normalizedLine
                    .replace(Regex("""\s*([+\-])\s*""")) { it.groupValues[1] }
                    .replace('-', '+')
                    .trim()
                if (results.isNotEmpty() && results.last().dose == null && !allZeroDosageRegex.matches(cleanDose)) {
                    val last = results.last()
                    last.dose = cleanDose
                    last.rawLines.add(rawLine)
                }
                continue
            }

            val hasMedicineKeywordOrSuffix = isMedicineKeywordLine(normalizedLine)

            // Extract food rule (if present on this line)
            val extractedFoodRule = extractFoodRule(normalizedLine)
            val lineWithoutFoodRule = stripFoodRulePhrases(normalizedLine)

            // Extract duration first so trailing numbers in duration aren't merged into dose
            val durationMatch = durationRegex.find(lineWithoutFoodRule)
            val extractedDuration = durationMatch?.let {
                val count = it.groupValues[1]
                val unit = it.groupValues[2].trim()
                "$count $unit"
            }

            val lineWithoutDuration = if (durationMatch != null) {
                lineWithoutFoodRule.replaceRange(durationMatch.range, " ")
            } else {
                lineWithoutFoodRule
            }

            // Extract dosage (skip matches immediately followed by strength units like "75+75 mg")
            val doseMatch = findDosageMatch(lineWithoutDuration)
            val extractedDose = doseMatch?.value
                ?.replace(Regex("""\s*([+\-])\s*""")) { it.groupValues[1] }
                ?.replace('-', '+')
                ?.takeUnless { allZeroDosageRegex.matches(it) }

            var candidate = if (doseMatch != null) {
                lineWithoutDuration.replaceRange(doseMatch.range, " ")
            } else {
                lineWithoutDuration
            }

            candidate = cleanCandidateName(candidate)

            // Split trailing combination strength pattern (e.g., "Tab. Preclot AS 75 mg + 75 mg")
            val (cleanedName, extractedStrength) = splitNameAndTrailingStrength(candidate)

            val hasName = cleanedName.isNotBlank() &&
                cleanedName.length >= 2 &&
                cleanedName.any { it.isLetter() } &&
                !stopWordsOnlyRegex.matches(cleanedName) &&
                !isLikelyInstructionOnly(cleanedName) &&
                !leadingPlusNoiseRegex.containsMatchIn(cleanedName)

            if (hasName && hasMedicineKeywordOrSuffix) {
                results.add(
                    MutableParsedEntry(
                        name = restoreOriginalSpacing(cleanedName),
                        dose = extractedDose,
                        foodRule = extractedFoodRule,
                        duration = extractedDuration,
                        strength = extractedStrength,
                        rawLines = mutableListOf(rawLine)
                    )
                )
            } else if (results.isNotEmpty() &&
                (extractedDose != null || extractedDuration != null || extractedFoodRule != null) &&
                !hasName
            ) {
                // Continuation line (e.g., dose, duration, or food rule on a separate line below medicine name)
                val last = results.last()
                if (last.dose == null && extractedDose != null) {
                    last.dose = extractedDose
                }
                if (last.duration == null && extractedDuration != null) {
                    last.duration = extractedDuration
                }
                if (last.foodRule == null && extractedFoodRule != null) {
                    last.foodRule = extractedFoodRule
                }
                last.rawLines.add(rawLine)
            }
        }

        return results.map {
            ParsedMedicine(
                name = it.name,
                dose = it.dose,
                foodRule = it.foodRule,
                rawText = it.rawLines.joinToString("\n"),
                duration = it.duration,
                strength = it.strength,
                parsedDoses = parseDosePattern(it.dose)
            )
        }
    }

    fun toDraftMedicine(parsed: ParsedMedicine): DraftMedicine {
        val rawName = parsed.name.trim()
        val inferredType = inferMedicineType(rawName)

        // Strip form prefix (e.g., "Tab. ", "Cap. ") from display medicine name
        var cleanName = formPrefixRegex.replace(rawName, "").trim()
        cleanName = cleanName.trimStart('.', '-', ':', ' ')

        // Extract strength and strength unit if present in name, or fall back to parsed.strength
        var strengthValue = ""
        var strengthUnitValue = ""
        val strengthMatch = strengthRegex.find(cleanName)
        if (strengthMatch != null) {
            val fullToken = strengthMatch.value.trim()
            val numberMatch = Regex("""^(\d+(?:\.\d+)?)\s*(.+)$""").find(fullToken)
            if (numberMatch != null) {
                strengthValue = numberMatch.groupValues[1]
                strengthUnitValue = normalizeStrengthUnit(numberMatch.groupValues[2].trim())
            }
            cleanName = cleanName.removeRange(strengthMatch.range).trim()
                .replace(Regex("""\s{2,}"""), " ")
                .trimEnd('-', ',', ' ')
        } else if (!parsed.strength.isNullOrBlank()) {
            val fallbackMatch = strengthRegex.find(parsed.strength)
            if (fallbackMatch != null) {
                val fullToken = fallbackMatch.value.trim()
                val numberMatch = Regex("""^(\d+(?:\.\d+)?)\s*(.+)$""").find(fullToken)
                if (numberMatch != null) {
                    strengthValue = numberMatch.groupValues[1]
                    strengthUnitValue = normalizeStrengthUnit(numberMatch.groupValues[2].trim())
                }
            }
        }

        val finalName = if (cleanName.isNotBlank()) cleanName else rawName
        val doseUnit = UnitHelper.getDoseUnitsForType(inferredType).firstOrNull() ?: "টি"
        val resolvedFoodRule = parsed.foodRule ?: FoodRule.AFTER
        val schedules = parseSchedules(parsed.dose, doseUnit, resolvedFoodRule)
        val durationDays = parseDurationDays(parsed.duration)

        return DraftMedicine(
            name = finalName,
            type = inferredType,
            strength = strengthValue,
            strengthUnit = strengthUnitValue,
            schedules = schedules,
            foodRule = resolvedFoodRule,
            durationDays = durationDays
        )
    }

    private fun findDosageMatch(text: String): MatchResult? {
        val threePart = threePartDoseRegex.find(text)
        if (threePart != null && !isFollowedByStrengthUnit(text, threePart.range.last + 1)) {
            return threePart
        }
        return doseRegex.findAll(text).firstOrNull { match ->
            !isFollowedByStrengthUnit(text, match.range.last + 1)
        }
    }

    private fun isFollowedByStrengthUnit(text: String, endIndex: Int): Boolean {
        if (endIndex >= text.length) return false
        val rest = text.substring(endIndex).trimStart()
        return Regex("""^(?:mg|mcg|ml|gm|g|iu|%)\b""", RegexOption.IGNORE_CASE).containsMatchIn(rest)
    }

    private fun splitNameAndTrailingStrength(candidate: String): Pair<String, String?> {
        val match = trailingCombinationStrengthRegex.find(candidate) ?: return candidate to null
        val extractedStrength = match.groupValues[1]
            .replace(Regex("""\s{2,}"""), " ")
            .replace(Regex("""\s*\+\s*"""), " + ")
            .trim()
        val remainingName = cleanCandidateName(candidate.removeRange(match.range))
        return if (remainingName.isNotBlank()) {
            remainingName to extractedStrength
        } else {
            candidate to null
        }
    }

    fun parseDosePattern(dosePattern: String?): List<ParsedDose> {
        if (dosePattern.isNullOrBlank()) return emptyList()
        val normalizedPattern = normalizeBengaliDigits(dosePattern).trim()
        val rawTokens = normalizedPattern.split('+', '-')
        if (rawTokens.isEmpty() || rawTokens.size > 4) return emptyList()

        val amounts = rawTokens.map { token ->
            parseDoseFraction(token) ?: return emptyList()
        }
        if (amounts.any { !it.isFinite() || it < 0.0 }) return emptyList()

        val slots = when (amounts.size) {
            1 -> listOf(DoseSlot.MORNING)
            2 -> listOf(DoseSlot.MORNING, DoseSlot.NIGHT)
            3 -> listOf(DoseSlot.MORNING, DoseSlot.NOON, DoseSlot.NIGHT)
            4 -> listOf(DoseSlot.MORNING, DoseSlot.NOON, DoseSlot.EVENING, DoseSlot.NIGHT)
            else -> return emptyList()
        }

        return amounts.zip(slots).mapNotNull { (amount, slot) ->
            if (amount <= 0.0) null else ParsedDose(slot = slot, doseAmount = amount)
        }
    }

    private fun parseSchedules(
        dosePattern: String?,
        doseUnit: String,
        foodRule: FoodRule
    ): List<DraftSchedule> {
        val parsedDoses = parseDosePattern(dosePattern)
        if (parsedDoses.isEmpty()) return emptyList()

        return parsedDoses.map { parsedDose ->
            val formattedAmount = if (parsedDose.doseAmount % 1.0 == 0.0) {
                parsedDose.doseAmount.toLong().toString()
            } else {
                BigDecimal.valueOf(parsedDose.doseAmount)
                    .stripTrailingZeros()
                    .toPlainString()
            }
            DraftSchedule(
                slot = parsedDose.slot,
                time = defaultTimeForSlot(parsedDose.slot),
                doseAmount = formattedAmount,
                doseUnit = doseUnit,
                isExpanded = true,
                foodRule = foodRule
            )
        }
    }

    private fun defaultTimeForSlot(slot: DoseSlot): String {
        return when (slot) {
            DoseSlot.MORNING -> "08:00"
            DoseSlot.NOON -> "13:00"
            DoseSlot.AFTERNOON -> "16:00"
            DoseSlot.EVENING -> "19:00"
            DoseSlot.NIGHT -> "22:00"
            DoseSlot.CUSTOM -> "08:00"
        }
    }

    private fun parseDoseFraction(token: String): Double? {
        val trimmed = normalizeBengaliDigits(token).trim()
        if (trimmed.isEmpty()) return null
        val zeroCheck = trimmed.toDoubleOrNull()
        if (zeroCheck != null) {
            return if (zeroCheck >= 0.0 && zeroCheck.isFinite()) zeroCheck else null
        }
        return DoseAmountHelper.parseDoseAmount(trimmed)
    }

    private fun parseDurationDays(duration: String?): String {
        if (duration.isNullOrBlank()) return ""
        val match = durationRegex.find(normalizeBengaliDigits(duration)) ?: return ""
        val count = match.groupValues[1].toIntOrNull() ?: return ""
        val unit = match.groupValues[2].lowercase()
        val days = when {
            unit.startsWith("week") || unit.startsWith("wk") || unit.contains("সপ্তাহ") -> count * 7
            unit.startsWith("month") || unit.startsWith("mo") || unit.contains("মাস") -> count * 30
            else -> count
        }
        return if (days > 0) days.toString() else ""
    }

    private fun inferMedicineType(text: String): MedicineType {
        val lower = text.lowercase()
        return when {
            Regex("""\bcap(?:sule)?\.?\b""").containsMatchIn(lower) -> MedicineType.CAPSULE
            Regex("""\b(?:syp|syrup|susp(?:ension)?)\.?\b""").containsMatchIn(lower) -> MedicineType.SYRUP
            Regex("""\bdrops?\.?\b""").containsMatchIn(lower) -> MedicineType.DROPS
            Regex("""\b(?:inhaler|spray)\.?\b""").containsMatchIn(lower) -> MedicineType.INHALER
            Regex("""\b(?:oint(?:ment)?|cream|gel)\.?\b""").containsMatchIn(lower) -> MedicineType.OINTMENT
            Regex("""\binj(?:ection)?\.?\b""").containsMatchIn(lower) -> MedicineType.INJECTION
            else -> MedicineType.TABLET
        }
    }

    private fun normalizeStrengthUnit(rawUnit: String): String {
        val compact = rawUnit.replace(" ", "").lowercase()
        return when (compact) {
            "mg" -> "mg"
            "mcg" -> "mcg"
            "mg/5ml" -> "mg/5mL"
            "mg/ml" -> "mg/mL"
            "mcg/puff" -> "mcg/puff"
            "iu/ml" -> "IU/mL"
            "%" -> "%"
            "mg/g" -> "mg/g"
            else -> rawUnit
        }
    }

    private fun extractFoodRule(line: String): FoodRule? {
        return when {
            emptyStomachRegex.containsMatchIn(line) -> FoodRule.EMPTY
            beforeFoodRegex.containsMatchIn(line) -> FoodRule.BEFORE
            afterFoodRegex.containsMatchIn(line) -> FoodRule.AFTER
            withFoodRegex.containsMatchIn(line) -> FoodRule.WITH
            else -> null
        }
    }

    private fun stripFoodRulePhrases(line: String): String {
        return line
            .replace(emptyStomachRegex, " ")
            .replace(beforeFoodRegex, " ")
            .replace(afterFoodRegex, " ")
            .replace(withFoodRegex, " ")
    }

    private fun isIgnoredLine(line: String): Boolean {
        if (ignoredLineKeywordsRegex.containsMatchIn(line)) return true
        // Skip date-only or phone-only lines
        if (Regex("""^\d{1,4}[/\-.]\d{1,2}[/\-.]\d{2,4}$""").matches(line)) return true
        if (Regex("""^[+\d\s()-]{7,}$""").matches(line)) return true
        return false
    }

    private fun isLikelyInstructionOnly(candidate: String): Boolean {
        val lower = candidate.lowercase()
        return lower in setOf(
            "after meal",
            "before meal",
            "with meal",
            "empty stomach",
            "before food",
            "after food",
            "continue",
            "if needed",
            "sos",
            "খাওয়ার আগে",
            "খাওয়ার পরে",
            "খাবারের আগে",
            "খাবারের পরে",
            "খালি পেটে",
            "খালিপেটে"
        )
    }

    private fun cleanCandidateName(raw: String): String {
        var text = leadingListMarkerRegex.replace(raw.trim(), "")
        text = text.replace(Regex("""\b(?:for|continue)\s*$""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""[×x]\s*$""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""\(\s*\)|\[\s*\]|\{\s*\}"""), " ")
        text = text.replace(Regex("""(?:\s*[-—–,]+\s*)+$"""), "")
        text = text.replace(Regex("""^(?:\s*[-—–,]+\s*)+"""), "")
        text = stripOuterAndUnmatchedBrackets(text.trim())
        text = text.replace(Regex("""(?:\s*[-—–,]+\s*)+$"""), "")
        text = text.replace(Regex("""^(?:\s*[-—–,]+\s*)+"""), "")
        return text.replace(Regex("""\s{2,}"""), " ").trim()
    }

    private fun stripOuterAndUnmatchedBrackets(input: String): String {
        var text = input.trim()
        val pairs = listOf('(' to ')', '[' to ']', '{' to '}')

        // Unwrap matching outer brackets if the entire string is wrapped in them
        var unwrapped: Boolean
        do {
            unwrapped = false
            for ((open, close) in pairs) {
                if (text.length >= 2 && text.first() == open && text.last() == close) {
                    var depth = 0
                    var wrapsWhole = true
                    for (i in 0 until text.length - 1) {
                        if (text[i] == open) depth++
                        else if (text[i] == close) depth--
                        if (depth == 0) {
                            wrapsWhole = false
                            break
                        }
                    }
                    if (wrapsWhole) {
                        text = text.substring(1, text.length - 1).trim()
                        unwrapped = true
                    }
                }
            }
        } while (unwrapped)

        // Strip leading opening brackets and trailing closing brackets that are unmatched
        for ((open, close) in pairs) {
            while (text.startsWith(open) && text.count { it == open } > text.count { it == close }) {
                text = text.drop(1).trimStart()
            }
            while (text.endsWith(close) && text.count { it == close } > text.count { it == open }) {
                text = text.dropLast(1).trimEnd()
            }
            // Also strip any stray closing bracket at the start or opening bracket at the end
            text = text.trimStart(close).trimEnd(open).trim()
        }

        return text
    }

    private fun restoreOriginalSpacing(text: String): String {
        return text.replace(Regex("""\s{2,}"""), " ").trim()
    }

    private fun normalizeBengaliDigits(input: String): String {
        val map = mapOf(
            '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
            '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
        )
        return buildString(input.length) {
            for (ch in input) {
                append(map[ch] ?: ch)
            }
        }
    }

    private data class MutableParsedEntry(
        val name: String,
        var dose: String?,
        var foodRule: FoodRule?,
        var duration: String?,
        var strength: String?,
        val rawLines: MutableList<String>
    )
}
