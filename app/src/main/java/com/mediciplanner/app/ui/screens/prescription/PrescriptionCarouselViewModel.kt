package com.mediciplanner.app.ui.screens.prescription

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediciplanner.app.R
import com.mediciplanner.app.core.util.DoseAmountHelper
import com.mediciplanner.app.core.util.OcrHelper
import com.mediciplanner.app.core.util.OcrParser
import com.mediciplanner.app.core.util.ParsedMedicine
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.notification.ReminderScheduler
import com.mediciplanner.app.ui.screens.prescription.components.parsePrescriptionDateToLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs


enum class PrescriptionSaveType {
    SAVE_NEW,
    EDIT_PRESCRIPTION,
    ADD_MEDICINE,
    EDIT_MEDICINE
}

data class DraftSchedule(
    val slot: DoseSlot,
    val time: String = "08:00",
    val doseAmount: String = "1",
    val doseUnit: String = "",
    val isExpanded: Boolean = true,
    val foodRule: FoodRule = FoodRule.AFTER,
    val id: String = UUID.randomUUID().toString()
) {
    val parsedDoseAmount: Double?
        get() = DoseAmountHelper.parseDoseAmount(doseAmount)
}

data class DraftMedicine(
    val name: String = "",
    val type: MedicineType? = null,
    val strength: String = "",
    val strengthUnit: String = "",
    val schedules: List<DraftSchedule> = emptyList(),
    val foodRule: FoodRule = FoodRule.AFTER,
    val durationDays: String = "",
    val intervalDays: Int = 1,
    val note: String = "",
    val imagePath: String? = null,
    val lineageId: String? = null,
    val stockCount: Int = 0
)

data class DraftPrescription(
    val id: String? = null,
    val date: String? = null,
    val doctorName: String = "",
    val location: String = "",
    val medicines: List<DraftMedicine> = emptyList(),
    val prescriptionImagePaths: List<String> = emptyList(),
    val note: String = ""
)

fun computeTotalMedicineSummary(
    duration: Int,
    interval: Int,
    schedules: List<DraftSchedule>
): String {
    if (duration <= 0 || schedules.isEmpty()) return ""
    val safeInterval = maxOf(1, interval)
    val activeDays = (duration + safeInterval - 1) / safeInterval
    if (activeDays <= 0) return ""

    val primaryUnit = schedules.firstOrNull { it.doseUnit.isNotBlank() }?.doseUnit?.trim() ?: return ""

    val perDayTotal = when (primaryUnit) {
        "mL", "tsp" -> schedules.sumOf { sched ->
            val amt = DoseAmountHelper.parseDoseAmount(sched.doseAmount) ?: 0.0
            when (sched.doseUnit.trim()) {
                "tsp" -> amt * 5.0
                else -> amt
            }
        }
        else -> schedules.sumOf { sched ->
            DoseAmountHelper.parseDoseAmount(sched.doseAmount) ?: 0.0
        }
    }

    val totalQuantity = activeDays * perDayTotal
    if (totalQuantity <= 0.0) return ""

    val formattedQuantity = if (totalQuantity % 1.0 == 0.0) {
        totalQuantity.toLong().toString()
    } else {
        BigDecimal.valueOf(totalQuantity)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    return when (primaryUnit) {
        "pcs", "টি" -> "মোট ওষুধ লাগবে: $formattedQuantity টি"
        "mL", "tsp" -> "আনুমানিক: $formattedQuantity mL"
        "puff" -> "মোট: $formattedQuantity puff"
        "drop" -> "প্রায়: $formattedQuantity ফোঁটা"
        "unit" -> "মোট: $formattedQuantity unit"
        "application" -> "মোট: $formattedQuantity application"
        else -> ""
    }
}

/**
 * Compares the original prescription (and its medicines and schedules) from the database
 * with the DraftPrescription to determine if any changes were made.
 * Returns true if changes exist, false if identical.
 */
fun hasPrescriptionChanged(
    original: PrescriptionWithMedicines,
    draft: DraftPrescription
): Boolean {
    val origPres = original.prescription

    // 1. Compare Date (normalize ISO & Bengali dates consistently)
    fun normalizeDate(raw: String?): String {
        val trimmed = raw.orEmpty().trim()
        val parsed = parsePrescriptionDateToLocalDate(trimmed)
        if (parsed != null) {
            return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        return if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
            trimmed.take(10)
        } else {
            trimmed
        }
    }
    val origDate = normalizeDate(origPres.date)
    val draftDate = normalizeDate(draft.date)
    if (origDate != draftDate) return true

    // 2. Compare Doctor Name
    val origDoctor = origPres.doctorName.orEmpty().trim()
    val draftDoctor = draft.doctorName.trim()
    if (origDoctor != draftDoctor) return true

    // 3. Compare Location
    val origLocation = origPres.location.orEmpty().trim()
    val draftLocation = draft.location.trim()
    if (origLocation != draftLocation) return true

    // 4. Compare Prescription Note
    val origPresNote = origPres.note.orEmpty().trim()
    val draftPresNote = draft.note.trim()
    if (origPresNote != draftPresNote) return true

    // 5. Compare Prescription Image Paths
    if (origPres.prescriptionImagePaths != draft.prescriptionImagePaths) return true

    // 5. Compare Medicines
    val origMeds = original.medicines
    val draftMeds = draft.medicines

    if (origMeds.size != draftMeds.size) return true

    for (i in origMeds.indices) {
        val origMedWithSched = origMeds[i]
        val origMed = origMedWithSched.medicine
        val draftMed = draftMeds[i]

        // Name
        if (origMed.name.trim() != draftMed.name.trim()) return true

        // Type
        if (origMed.type != draftMed.type) return true

        // Strength
        val origStrength = origMed.strength
        val draftStrength = draftMed.strength.toDoubleOrNull()
        if (origStrength != draftStrength) return true

        // Strength Unit
        val origStrengthUnit = origMed.strengthUnit.orEmpty().trim()
        val draftStrengthUnit = draftMed.strengthUnit.trim()
        if (origStrengthUnit != draftStrengthUnit) return true

        // Food Rule
        if (origMed.foodRule != draftMed.foodRule) return true

        // Duration Days
        val origDuration = origMed.durationDays
        val draftDuration = draftMed.durationDays.toIntOrNull()
        if (origDuration != draftDuration) return true

        // Interval Days
        val origInterval = maxOf(1, origMed.intervalDays)
        val draftInterval = maxOf(1, draftMed.intervalDays)
        if (origInterval != draftInterval) return true

        // Note
        val origNote = origMed.note.orEmpty().trim()
        val draftNote = draftMed.note.trim()
        if (origNote != draftNote) return true

        // Medicine Image Path
        val origImg = origMed.imagePath.orEmpty().trim()
        val draftImg = draftMed.imagePath.orEmpty().trim()
        if (origImg != draftImg) return true

        // Stock Count
        if (origMed.stockCount != draftMed.stockCount) return true

        // Dose Schedules
        val origScheds = origMedWithSched.doseSchedules.map { it.doseSchedule }
            .sortedWith(compareBy({ it.slot.name }, { it.time }))
        val draftScheds = draftMed.schedules
            .sortedWith(compareBy({ it.slot.name }, { it.time }))

        if (origScheds.size != draftScheds.size) return true

        for (j in origScheds.indices) {
            val os = origScheds[j]
            val ds = draftScheds[j]

            if (os.slot != ds.slot) return true
            if (os.time.trim() != ds.time.trim()) return true

            val osAmt = os.doseAmount
            val dsAmt = DoseAmountHelper.parseDoseAmount(ds.doseAmount) ?: 1.0
            if (abs(osAmt - dsAmt) > 1e-6) return true

            if (os.doseUnit.trim() != ds.doseUnit.trim()) return true
            if ((os.foodRuleOverride ?: os.foodRule) != ds.foodRule) return true
        }
    }

    return false
}

class PrescriptionCarouselViewModel(
    private val prescriptionRepository: PrescriptionRepository,
    private val reminderRepository: ReminderRepository? = null,
    private val reminderScheduler: ReminderScheduler? = null,
    private val themePreferences: ThemePreferences? = null,
    private val ocrHelper: OcrHelper? = null
) : ViewModel() {

    val timeFormat: StateFlow<String> = (themePreferences?.timeFormatFlow ?: flowOf(ThemePreferences.TIME_FORMAT_12H))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePreferences.TIME_FORMAT_12H
        )

    private val _editingPrescription = MutableStateFlow<PrescriptionEntity?>(null)
    val editingPrescription: StateFlow<PrescriptionEntity?> = _editingPrescription.asStateFlow()

    private val _editingPrescriptionId = MutableStateFlow<String?>(null)
    val editingPrescriptionId: StateFlow<String?> = _editingPrescriptionId.asStateFlow()

    private val _editingParentId = MutableStateFlow<String?>(null)
    val editingParentId: StateFlow<String?> = _editingParentId.asStateFlow()

    private val _editingMedicineId = MutableStateFlow<String?>(null)
    val editingMedicineId: StateFlow<String?> = _editingMedicineId.asStateFlow()

    private val _initialPage = MutableStateFlow(0)
    val initialPage: StateFlow<Int> = _initialPage.asStateFlow()

    private val _draftPrescription = MutableStateFlow(DraftPrescription())
    val draftPrescription: StateFlow<DraftPrescription> = _draftPrescription.asStateFlow()

    private val _draftMedicine = MutableStateFlow(DraftMedicine())
    val draftMedicine: StateFlow<DraftMedicine> = _draftMedicine.asStateFlow()

    private val _selectedMedicineIndex = MutableStateFlow<Int?>(null)
    val selectedMedicineIndex: StateFlow<Int?> = _selectedMedicineIndex.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _intervalError = MutableStateFlow<String?>(null)
    val intervalError: StateFlow<String?> = _intervalError.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _saveMode = MutableStateFlow(PrescriptionSaveType.SAVE_NEW)
    val saveMode: StateFlow<PrescriptionSaveType> = _saveMode.asStateFlow()

    private val _pendingOcrImageUri = MutableStateFlow<Uri?>(null)
    val pendingOcrImageUri: StateFlow<Uri?> = _pendingOcrImageUri.asStateFlow()

    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText.asStateFlow()

    private val _isScanningOcr = MutableStateFlow(false)
    val isScanningOcr: StateFlow<Boolean> = _isScanningOcr.asStateFlow()

    private val _parsedMedicines = MutableStateFlow<List<ParsedMedicine>>(emptyList())
    val parsedMedicines: StateFlow<List<ParsedMedicine>> = _parsedMedicines.asStateFlow()

    private val _showOcrReview = MutableStateFlow(false)
    val showOcrReview: StateFlow<Boolean> = _showOcrReview.asStateFlow()

    private val _reviewMedicines = MutableStateFlow<List<ParsedMedicine>>(emptyList())
    val reviewMedicines: StateFlow<List<ParsedMedicine>> = _reviewMedicines.asStateFlow()

    private val _ocrReviewErrorRes = MutableStateFlow<Int?>(null)
    val ocrReviewErrorRes: StateFlow<Int?> = _ocrReviewErrorRes.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun hasPrescriptionChanged(original: PrescriptionWithMedicines, draft: DraftPrescription): Boolean {
        return com.mediciplanner.app.ui.screens.prescription.hasPrescriptionChanged(original, draft)
    }

    fun resetDraft() {
        _saveMode.value = PrescriptionSaveType.SAVE_NEW
        _editingPrescription.value = null
        _editingPrescriptionId.value = null
        _editingParentId.value = null
        _editingMedicineId.value = null
        _initialPage.value = 0
        _selectedMedicineIndex.value = null
        _draftPrescription.value = DraftPrescription()
        _draftMedicine.value = DraftMedicine()
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
        _intervalError.value = null
        _pendingOcrImageUri.value = null
        _extractedText.value = ""
        _isScanningOcr.value = false
        _parsedMedicines.value = emptyList()
        _showOcrReview.value = false
        _reviewMedicines.value = emptyList()
    }

    fun setErrorMessage(msg: String?) {
        _errorMessage.value = msg
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
        _saveError.value = null
    }

    // Validation State Flows
    fun isCard1Valid(): Boolean {
        val date = _draftPrescription.value.date
        return date != null && date.isNotBlank()
    }

    fun isCard2Valid(): Boolean {
        val med = _draftMedicine.value
        return med.name.isNotBlank() && med.type != null
    }

    fun isCard3Valid(): Boolean {
        val schedules = _draftMedicine.value.schedules
        return schedules.isNotEmpty() && schedules.all {
            DoseAmountHelper.parseDoseAmount(it.doseAmount) != null && it.doseUnit.isNotBlank()
        }
    }

    fun isCard4Valid(): Boolean {
        val duration = _draftMedicine.value.durationDays
        val parsed = duration.toIntOrNull()
        val interval = _draftMedicine.value.intervalDays
        return parsed != null && parsed >= 1 && interval <= parsed
    }

    val isNextEnabled: Boolean
        get() = _draftMedicine.value.name.isNotBlank() &&
                _draftMedicine.value.type != null &&
                _draftMedicine.value.schedules.isNotEmpty() &&
                _draftMedicine.value.schedules.all {
                    DoseAmountHelper.parseDoseAmount(it.doseAmount) != null && it.doseUnit.isNotBlank()
                }

    fun isCurrentMedicineComplete(): Boolean {
        return isCard2Valid() && isCard3Valid() && isCard4Valid()
    }

    private val _durationError = MutableStateFlow<String?>(null)
    val durationError: StateFlow<String?> = _durationError.asStateFlow()

    /**
     * Validates the card at [cardIndex] and sets [errorMessage] dynamically if invalid.
     * Returns true if valid, false otherwise.
     */
    fun validateCard(cardIndex: Int): Boolean {
        when (cardIndex) {
            0 -> {
                val date = _draftPrescription.value.date
                if (date.isNullOrBlank()) {
                    _errorMessage.value = "অনুগ্রহ করে তারিখ সিলেক্ট করুন"
                    return false
                }
            }
            1 -> {
                val med = _draftMedicine.value
                if (med.name.isBlank()) {
                    _errorMessage.value = "অনুগ্রহ করে ওষুধের নাম লিখুন"
                    return false
                }
                if (med.type == null) {
                    _errorMessage.value = "অনুগ্রহ করে ওষুধের ধরন সিলেক্ট করুন"
                    return false
                }
                // Strength (মাত্রা) is optional. Only if strength is entered, validate unit if available.
                if (med.strength.isNotBlank()) {
                    val availableStrengthUnits = getUnitsForType(med.type)
                    if (availableStrengthUnits.isNotEmpty() && med.strengthUnit.isBlank()) {
                        _errorMessage.value = "অনুগ্রহ করে মাত্রার একক সিলেক্ট করুন"
                        return false
                    }
                }
            }
            2 -> {
                val med = _draftMedicine.value
                if (med.schedules.isEmpty()) {
                    _errorMessage.value = "অনুগ্রহ করে সেবনের সময় সিলেক্ট করুন"
                    return false
                }
                if (med.schedules.any { it.doseAmount.isBlank() }) {
                    _errorMessage.value = "অনুগ্রহ করে ডোজের পরিমাণ লিখুন"
                    return false
                }
                if (med.schedules.any { DoseAmountHelper.parseDoseAmount(it.doseAmount) == null }) {
                    _errorMessage.value = "সঠিক ডোজ পরিমাণ লিখুন (যেমন: 1, 0.5, 1/2)"
                    return false
                }
                if (med.schedules.any { it.doseUnit.isBlank() }) {
                    _errorMessage.value = "অনুগ্রহ করে ডোজের একক সিলেক্ট করুন"
                    return false
                }
            }
            3 -> {
                val duration = _draftMedicine.value.durationDays
                val parsed = duration.toIntOrNull()
                if (duration.isBlank() || parsed == null || parsed < 1) {
                    val msg = "অনুগ্রহ করে ওষুধ সেবনের মেয়াদ (কত দিন) লিখুন"
                    _durationError.value = msg
                    _errorMessage.value = msg
                    return false
                }
                val interval = _draftMedicine.value.intervalDays
                if (interval > parsed) {
                    val msg = "মেয়াদের চেয়ে বেশি বিরতি দেয়া যাবে না"
                    _intervalError.value = msg
                    _errorMessage.value = msg
                    return false
                }
            }
        }
        _durationError.value = null
        _intervalError.value = null
        _errorMessage.value = null
        return true
    }

    // Card 1 updates
    fun updateDate(newDate: String) {
        _errorMessage.value = null
        _saveError.value = null
        _draftPrescription.update { it.copy(date = newDate) }
    }

    fun updateDoctorName(name: String) {
        _draftPrescription.update { it.copy(doctorName = name) }
    }

    fun updateLocation(location: String) {
        _draftPrescription.update { it.copy(location = location) }
    }

    fun updatePrescriptionImagePaths(paths: List<String>) {
        _draftPrescription.update { it.copy(prescriptionImagePaths = paths) }
    }

    fun addPrescriptionImagePaths(paths: List<String>) {
        _draftPrescription.update { it.copy(prescriptionImagePaths = it.prescriptionImagePaths + paths) }
    }

    fun removePrescriptionImagePath(path: String) {
        _draftPrescription.update { it.copy(prescriptionImagePaths = it.prescriptionImagePaths - path) }
    }

    fun onScanPrescriptionImageSelected(uri: Uri) {
        processOcrImage(uri)
    }

    fun processOcrImage(uri: Uri) {
        processOcrImages(listOf(uri))
    }

    fun processOcrImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _pendingOcrImageUri.value = uris.first()
        val helper = ocrHelper ?: return
        _isScanningOcr.value = true
        _errorMessage.value = null

        val collectedTexts = mutableListOf<String>()
        val perImageParsed = mutableListOf<ParsedMedicine>()
        var lastError: Exception? = null

        fun scanNext(index: Int) {
            if (index >= uris.size) {
                _isScanningOcr.value = false
                val combinedText = collectedTexts.joinToString("\n\n")
                _extractedText.value = combinedText
                val mergedParsed = perImageParsed
                    .filter { OcrParser.isValidParsedMedicine(it) }
                    .distinctBy {
                        Triple(
                            it.name.trim().lowercase(),
                            it.dose?.trim().orEmpty(),
                            it.duration?.trim().orEmpty()
                        )
                    }
                _parsedMedicines.value = mergedParsed
                if (combinedText.isBlank()) {
                    if (lastError != null) {
                        _errorMessage.value = lastError?.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "টেক্সট স্ক্যান করতে সমস্যা হয়েছে"
                    } else {
                        _toastMessage.value = "ছবিতে কোনো টেক্সট পাওয়া যায়নি"
                    }
                } else {
                    _reviewMedicines.value = mergedParsed
                    _showOcrReview.value = true
                }
                return
            }

            helper.recognizeText(
                imageUri = uris[index],
                onSuccess = { text ->
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        collectedTexts.add(trimmed)
                        perImageParsed.addAll(OcrParser.parseMedicines(trimmed))
                    }
                    scanNext(index + 1)
                },
                onFailure = { exception ->
                    lastError = exception
                    scanNext(index + 1)
                }
            )
        }

        scanNext(0)
    }

    fun parseMedicinesFromExtractedText(): List<ParsedMedicine> {
        val parsed = OcrParser.parseMedicines(_extractedText.value)
        _parsedMedicines.value = parsed
        if (parsed.isEmpty()) {
            _toastMessage.value = "টেক্সট থেকে কোনো ওষুধ শনাক্ত করা যায়নি"
        }
        return parsed
    }

    fun openOcrReview() {
        val source = _parsedMedicines.value.ifEmpty {
            OcrParser.parseMedicines(_extractedText.value)
        }.filter { OcrParser.isValidParsedMedicine(it) }
        _parsedMedicines.value = source
        _reviewMedicines.value = source
        _ocrReviewErrorRes.value = null
        _showOcrReview.value = true
    }

    fun updateReviewMedicine(
        index: Int,
        name: String,
        dose: String?,
        foodRule: FoodRule?
    ) {
        _ocrReviewErrorRes.value = null
        _reviewMedicines.update { list ->
            if (index !in list.indices) return@update list
            list.toMutableList().apply {
                this[index] = this[index].copy(
                    name = name,
                    dose = dose,
                    foodRule = foodRule,
                    parsedDoses = OcrParser.parseDosePattern(dose)
                )
            }
        }
    }

    fun removeReviewMedicine(index: Int) {
        _ocrReviewErrorRes.value = null
        _reviewMedicines.update { list ->
            if (index !in list.indices) return@update list
            list.toMutableList().apply { removeAt(index) }
        }
    }

    fun addEmptyReviewMedicine() {
        _ocrReviewErrorRes.value = null
        _reviewMedicines.update { list ->
            list + ParsedMedicine(
                name = "",
                dose = "",
                foodRule = null,
                rawText = "",
                parsedDoses = emptyList()
            )
        }
    }

    fun cancelOcrReview() {
        _ocrReviewErrorRes.value = null
        _reviewMedicines.value = emptyList()
        _showOcrReview.value = false
    }

    fun confirmAddAllReviewedMedicines(onNavigateToCard2: () -> Unit) {
        val validMedicines = _reviewMedicines.value
            .map {
                val cleanDose = it.dose?.trim()?.ifBlank { null }
                it.copy(
                    name = it.name.trim(),
                    dose = cleanDose,
                    parsedDoses = OcrParser.parseDosePattern(cleanDose)
                )
            }
            .filter { it.name.isNotBlank() }

        if (validMedicines.isEmpty()) {
            _ocrReviewErrorRes.value = R.string.ocr_review_empty_error
            _toastMessage.value = "কমপক্ষে একটি ওষুধের নাম লিখুন"
            return
        }

        if (validMedicines.any { it.foodRule == null }) {
            _ocrReviewErrorRes.value = R.string.ocr_review_food_rule_missing_error
            _toastMessage.value = "অনুগ্রহ করে সব ওষুধের খাবারের নিয়ম নির্বাচন করুন।"
            return
        }

        _ocrReviewErrorRes.value = null
        addAllParsedMedicinesToDraft(validMedicines)
        _reviewMedicines.value = emptyList()
        _showOcrReview.value = false
        onNavigateToCard2()
    }

    fun addAllParsedMedicinesToDraft(medicines: List<ParsedMedicine>) {
        if (medicines.isEmpty()) return
        val newDraftMeds = medicines.map { OcrParser.toDraftMedicine(it) }
        val currentMed = _draftMedicine.value
        val selIdx = _selectedMedicineIndex.value
        val currentList = _draftPrescription.value.medicines.toMutableList()

        if (selIdx != null && selIdx in currentList.indices && currentMed.name.isNotBlank()) {
            currentList[selIdx] = currentMed
        } else if (selIdx == null && isCurrentMedicineComplete()) {
            currentList.add(currentMed)
        }

        val firstNewIndex = currentList.size
        currentList.addAll(newDraftMeds)

        _draftPrescription.update { it.copy(medicines = currentList) }
        _selectedMedicineIndex.value = firstNewIndex
        _draftMedicine.value = currentList[firstNewIndex]
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
        _intervalError.value = null
    }

    fun addParsedMedicineToDraft(parsed: ParsedMedicine) {
        val newDraftMed = OcrParser.toDraftMedicine(parsed)
        val currentMed = _draftMedicine.value
        val selIdx = _selectedMedicineIndex.value
        val currentList = _draftPrescription.value.medicines.toMutableList()

        if (selIdx != null && selIdx in currentList.indices && currentMed.name.isNotBlank()) {
            currentList[selIdx] = currentMed
        } else if (selIdx == null && isCurrentMedicineComplete()) {
            currentList.add(currentMed)
        }

        currentList.add(newDraftMed)
        val newIndex = currentList.lastIndex

        _draftPrescription.update { it.copy(medicines = currentList) }
        _selectedMedicineIndex.value = newIndex
        _draftMedicine.value = newDraftMed
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
        _intervalError.value = null
    }

    // Card 2 updates
    fun updateMedicineName(name: String) {
        _errorMessage.value = null
        _draftMedicine.update { it.copy(name = name) }
        val selIdx = _selectedMedicineIndex.value
        if (selIdx != null && selIdx in _draftPrescription.value.medicines.indices) {
            _draftPrescription.update { pres ->
                val updated = pres.medicines.toMutableList()
                updated[selIdx] = updated[selIdx].copy(name = name)
                pres.copy(medicines = updated)
            }
        }
    }

    fun updateMedicineType(newType: MedicineType) {
        _errorMessage.value = null
        val validDoseUnits = getDoseUnitsForType(newType)
        _draftMedicine.update { current ->
            // Reset strength unit on type change to prevent mismatch
            val updatedStrengthUnit = ""

            // Reset dose units in schedules if not valid for new type
            val updatedSchedules = current.schedules.map { schedule ->
                val updatedDoseUnit = if (schedule.doseUnit in validDoseUnits) schedule.doseUnit else ""
                schedule.copy(doseUnit = updatedDoseUnit)
            }

            current.copy(
                type = newType,
                strengthUnit = updatedStrengthUnit,
                schedules = updatedSchedules
            )
        }
    }

    fun clearStrengthUnit() {
        _draftMedicine.update { it.copy(strengthUnit = "") }
    }

    fun updateStrength(strength: String) {
        _errorMessage.value = null
        val filtered = strength.filter { it.isDigit() || it == '.' }
        _draftMedicine.update { it.copy(strength = filtered) }
    }

    fun updateStrengthUnit(unit: String) {
        _errorMessage.value = null
        _draftMedicine.update { it.copy(strengthUnit = unit) }
    }

    fun updateMedicineImagePath(imagePath: String?) {
        _draftMedicine.update { it.copy(imagePath = imagePath) }
    }

    // Card 3 updates
    fun toggleSlot(slot: DoseSlot) {
        _errorMessage.value = null
        _draftMedicine.update { current ->
            val existing = current.schedules.find { it.slot == slot }
            val updatedSchedules = if (existing != null) {
                current.schedules.filterNot { it.slot == slot }
            } else {
                val availableUnits = getDoseUnitsForType(current.type)
                val defaultUnit = availableUnits.firstOrNull() ?: ""
                val defaultTime = when (slot) {
                    DoseSlot.MORNING -> "08:00"
                    DoseSlot.NOON -> "13:00"
                    DoseSlot.AFTERNOON -> "16:00"
                    DoseSlot.EVENING -> "19:00"
                    DoseSlot.NIGHT -> "22:00"
                    DoseSlot.CUSTOM -> "10:00"
                }
                current.schedules + DraftSchedule(
                    slot = slot,
                    time = defaultTime,
                    doseAmount = "1",
                    doseUnit = defaultUnit,
                    isExpanded = true,
                    foodRule = FoodRule.AFTER
                )
            }
            current.copy(schedules = updatedSchedules)
        }
    }

    fun toggleScheduleExpanded(slot: DoseSlot) {
        _draftMedicine.update { current ->
            val updated = current.schedules.map {
                if (it.slot == slot) it.copy(isExpanded = !it.isExpanded) else it
            }
            current.copy(schedules = updated)
        }
    }

    fun updateScheduleTime(slot: DoseSlot, newTime: String) {
        _draftMedicine.update { current ->
            val updated = current.schedules.map {
                if (it.slot == slot) it.copy(time = newTime) else it
            }
            current.copy(schedules = updated)
        }
    }

    fun updateScheduleDoseAmount(slot: DoseSlot, amount: String) {
        _errorMessage.value = null
        val filtered = DoseAmountHelper.sanitizeDoseInput(amount)
        _draftMedicine.update { current ->
            val updated = current.schedules.map {
                if (it.slot == slot) it.copy(doseAmount = filtered) else it
            }
            current.copy(schedules = updated)
        }
    }

    fun updateScheduleDoseUnit(slot: DoseSlot, unit: String) {
        _errorMessage.value = null
        _draftMedicine.update { current ->
            val updated = current.schedules.map {
                if (it.slot == slot) it.copy(doseUnit = unit) else it
            }
            current.copy(schedules = updated)
        }
    }

    fun updateSlotFoodRule(slotId: String, rule: FoodRule) {
        _draftMedicine.update { current ->
            val updated = current.schedules.map {
                if (it.id == slotId) it.copy(foodRule = rule) else it
            }
            current.copy(schedules = updated)
        }
    }

    fun updateSlotFoodRule(slot: DoseSlot, rule: FoodRule) {
        _draftMedicine.update { current ->
            val updated = current.schedules.map {
                if (it.slot == slot) it.copy(foodRule = rule) else it
            }
            current.copy(schedules = updated)
        }
    }

    fun updateFoodRule(foodRule: FoodRule) {
        _draftMedicine.update { it.copy(foodRule = foodRule) }
    }

    // Card 4 updates
    fun updateDurationDays(days: String) {
        _durationError.value = null
        _errorMessage.value = null
        val filtered = days.filter { it.isDigit() }
        _draftMedicine.update { it.copy(durationDays = filtered) }
        validateIntervalAgainstDuration()
    }

    fun updateIntervalDays(interval: Int) {
        val clean = maxOf(1, interval)
        _draftMedicine.update { it.copy(intervalDays = clean) }
        validateIntervalAgainstDuration()
    }

    fun validateIntervalAgainstDuration(): Boolean {
        val interval = _draftMedicine.value.intervalDays
        val parsedDuration = _draftMedicine.value.durationDays.toIntOrNull()
        if (parsedDuration != null && interval > parsedDuration) {
            val msg = "মেয়াদের চেয়ে বেশি বিরতি দেয়া যাবে না"
            _intervalError.value = msg
            _errorMessage.value = msg
            return false
        }
        _intervalError.value = null
        return true
    }

    fun incrementDuration() {
        val currentDays = _draftMedicine.value.durationDays.toIntOrNull() ?: 0
        updateDurationDays((currentDays + 1).toString())
    }

    fun decrementDuration() {
        val currentDays = _draftMedicine.value.durationDays.toIntOrNull() ?: 1
        if (currentDays > 1) {
            updateDurationDays((currentDays - 1).toString())
        } else {
            updateDurationDays("1")
        }
    }

    fun updateMedicineNote(note: String) {
        _draftMedicine.update { it.copy(note = note) }
    }

    fun updateStockCount(count: Int) {
        val clean = maxOf(0, count)
        _draftMedicine.update { it.copy(stockCount = clean) }
    }

    fun incrementStockCount() {
        updateStockCount(_draftMedicine.value.stockCount + 1)
    }

    fun decrementStockCount() {
        val current = _draftMedicine.value.stockCount
        if (current > 0) {
            updateStockCount(current - 1)
        }
    }

    fun calculateTotalMedicine(
        duration: Int,
        interval: Int,
        schedules: List<DraftSchedule>
    ): String = computeTotalMedicineSummary(duration, interval, schedules)

    fun selectMedicineForEditing(index: Int) {
        val currentMeds = _draftPrescription.value.medicines
        if (index !in currentMeds.indices) return
        val currentIdx = _selectedMedicineIndex.value
        if (currentIdx == index) return

        val currentMed = _draftMedicine.value
        if (currentIdx != null && currentIdx in currentMeds.indices && currentMed.name.isNotBlank()) {
            val updated = currentMeds.toMutableList()
            updated[currentIdx] = currentMed
            _draftPrescription.update { it.copy(medicines = updated) }
        } else if (currentIdx == null && isCurrentMedicineComplete()) {
            _draftPrescription.update { it.copy(medicines = it.medicines + currentMed) }
        }
        _selectedMedicineIndex.value = index
        _draftMedicine.value = _draftPrescription.value.medicines[index]
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
        _intervalError.value = null
    }

    fun hasNextMedicine(): Boolean {
        val currentIdx = _selectedMedicineIndex.value ?: return false
        val total = _draftPrescription.value.medicines.size
        return total > 1 && currentIdx in 0 until (total - 1)
    }

    fun goToNextMedicine(): Boolean {
        val currentIdx = _selectedMedicineIndex.value ?: return false
        val total = _draftPrescription.value.medicines.size
        if (total <= 1 || currentIdx >= total - 1) return false
        selectMedicineForEditing(currentIdx + 1)
        return true
    }

    fun removeDraftMedicineAt(index: Int) {
        val currentMeds = _draftPrescription.value.medicines
        if (index !in currentMeds.indices) return

        val updatedList = currentMeds.toMutableList().apply { removeAt(index) }
        _draftPrescription.update { it.copy(medicines = updatedList) }

        val currentIdx = _selectedMedicineIndex.value
        when {
            updatedList.isEmpty() -> {
                _selectedMedicineIndex.value = null
                _draftMedicine.value = DraftMedicine()
            }
            currentIdx == index -> {
                val nextIndex = index.coerceAtMost(updatedList.lastIndex)
                _selectedMedicineIndex.value = nextIndex
                _draftMedicine.value = updatedList[nextIndex]
            }
            currentIdx != null && currentIdx > index -> {
                _selectedMedicineIndex.value = currentIdx - 1
            }
        }
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
        _intervalError.value = null
    }

    fun addEmptyDraftMedicine(): Int {
        val currentMeds = _draftPrescription.value.medicines
        val updatedList = currentMeds + DraftMedicine()
        val newIndex = updatedList.lastIndex
        _draftPrescription.update { it.copy(medicines = updatedList) }
        _selectedMedicineIndex.value = newIndex
        _draftMedicine.value = updatedList[newIndex]
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
        _intervalError.value = null
        return newIndex
    }

    private fun updateMedicineAt(index: Int, transform: (DraftMedicine) -> DraftMedicine) {
        val currentMeds = _draftPrescription.value.medicines
        if (index !in currentMeds.indices) return
        val updatedMed = transform(currentMeds[index])
        val updatedList = currentMeds.toMutableList().apply { this[index] = updatedMed }
        _draftPrescription.update { it.copy(medicines = updatedList) }
        if (_selectedMedicineIndex.value == index) {
            _draftMedicine.value = updatedMed
        }
        _errorMessage.value = null
        _saveError.value = null
    }

    fun updateMedicineNameAt(index: Int, name: String) {
        updateMedicineAt(index) { it.copy(name = name) }
    }

    fun updateMedicineTypeAt(index: Int, newType: MedicineType) {
        val validDoseUnits = getDoseUnitsForType(newType)
        updateMedicineAt(index) { current ->
            val updatedSchedules = current.schedules.map { schedule ->
                val updatedDoseUnit = if (schedule.doseUnit in validDoseUnits) schedule.doseUnit else ""
                schedule.copy(doseUnit = updatedDoseUnit)
            }
            current.copy(
                type = newType,
                strengthUnit = if (current.type == newType) current.strengthUnit else "",
                schedules = updatedSchedules
            )
        }
    }

    fun updateStrengthAt(index: Int, strength: String) {
        val filtered = strength.filter { it.isDigit() || it == '.' }
        updateMedicineAt(index) { it.copy(strength = filtered) }
    }

    fun updateStrengthUnitAt(index: Int, unit: String) {
        updateMedicineAt(index) { it.copy(strengthUnit = unit) }
    }

    fun toggleSlotAt(index: Int, slot: DoseSlot) {
        updateMedicineAt(index) { current ->
            val existing = current.schedules.find { it.slot == slot }
            val updatedSchedules = if (existing != null) {
                current.schedules.filterNot { it.slot == slot }
            } else {
                val availableUnits = getDoseUnitsForType(current.type)
                val defaultUnit = availableUnits.firstOrNull() ?: ""
                val defaultTime = when (slot) {
                    DoseSlot.MORNING -> "08:00"
                    DoseSlot.NOON -> "13:00"
                    DoseSlot.AFTERNOON -> "16:00"
                    DoseSlot.EVENING -> "19:00"
                    DoseSlot.NIGHT -> "22:00"
                    DoseSlot.CUSTOM -> "10:00"
                }
                current.schedules + DraftSchedule(
                    slot = slot,
                    time = defaultTime,
                    doseAmount = "1",
                    doseUnit = defaultUnit,
                    isExpanded = true,
                    foodRule = FoodRule.AFTER
                )
            }
            current.copy(schedules = updatedSchedules)
        }
    }

    fun updateScheduleTimeAt(index: Int, slot: DoseSlot, newTime: String) {
        updateMedicineAt(index) { current ->
            current.copy(
                schedules = current.schedules.map {
                    if (it.slot == slot) it.copy(time = newTime) else it
                }
            )
        }
    }

    fun updateScheduleDoseAmountAt(index: Int, slot: DoseSlot, amount: String) {
        val filtered = DoseAmountHelper.sanitizeDoseInput(amount)
        updateMedicineAt(index) { current ->
            current.copy(
                schedules = current.schedules.map {
                    if (it.slot == slot) it.copy(doseAmount = filtered) else it
                }
            )
        }
    }

    fun updateScheduleDoseUnitAt(index: Int, slot: DoseSlot, unit: String) {
        updateMedicineAt(index) { current ->
            current.copy(
                schedules = current.schedules.map {
                    if (it.slot == slot) it.copy(doseUnit = unit) else it
                }
            )
        }
    }

    fun updateSlotFoodRuleAt(index: Int, slot: DoseSlot, rule: FoodRule) {
        updateMedicineAt(index) { current ->
            current.copy(
                schedules = current.schedules.map {
                    if (it.slot == slot) it.copy(foodRule = rule) else it
                }
            )
        }
    }

    fun updateIntervalDaysAt(index: Int, interval: Int) {
        val clean = maxOf(1, interval)
        updateMedicineAt(index) { it.copy(intervalDays = clean) }
        _intervalError.value = null
    }

    fun updateDurationDaysAt(index: Int, days: String) {
        val filtered = days.filter { it.isDigit() }
        _durationError.value = null
        _intervalError.value = null
        updateMedicineAt(index) { it.copy(durationDays = filtered) }
    }

    fun updateStockCountAt(index: Int, count: Int) {
        val clean = maxOf(0, count)
        updateMedicineAt(index) { it.copy(stockCount = clean) }
    }

    fun updateMedicineNoteAt(index: Int, note: String) {
        updateMedicineAt(index) { it.copy(note = note) }
    }

    fun updateMedicineImagePathAt(index: Int, imagePath: String?) {
        updateMedicineAt(index) { it.copy(imagePath = imagePath) }
    }

    /**
     * LOCKED LOGIC:
     * Do NOT reset the DraftPrescription state when 'Add More' is clicked.
     * Only clear the DraftMedicine fields (name, type, strength, schedules, etc.)
     * and move the pager index back to 1 (Card 2).
     * The Date, Doctor Name, and Location must remain intact.
     */
    fun onAddMoreClicked(onNavigateToCard2: () -> Unit) {
        val currentMed = _draftMedicine.value
        val selIdx = _selectedMedicineIndex.value
        val currentList = _draftPrescription.value.medicines
        if (isCurrentMedicineComplete()) {
            if (selIdx != null && selIdx in currentList.indices) {
                val updated = currentList.toMutableList()
                updated[selIdx] = currentMed
                _draftPrescription.update { it.copy(medicines = updated) }
            } else {
                _draftPrescription.update { prescription ->
                    prescription.copy(medicines = prescription.medicines + currentMed)
                }
            }
        }
        _selectedMedicineIndex.value = null
        _draftMedicine.value = DraftMedicine()
        _saveError.value = null
        onNavigateToCard2()
    }

    /**
     * Review & Save:
     * Validates and persists the prescription, medicines, and schedules to Room.
     * Generates reminders for the next 7 days and schedules alarms via ReminderScheduler.
     */
    fun reviewAndSave(
        onSuccess: () -> Unit,
        onNoChanges: (() -> Unit)? = null,
        onSuccessWithType: ((PrescriptionSaveType) -> Unit)? = null
    ): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null

            try {
                val currentMed = _draftMedicine.value
                val selIdx = _selectedMedicineIndex.value
                val currentList = _draftPrescription.value.medicines
                val allMedicines = if (selIdx != null && selIdx in currentList.indices) {
                    val updated = currentList.toMutableList()
                    if (isCurrentMedicineComplete()) {
                        updated[selIdx] = currentMed
                    }
                    updated
                } else {
                    buildList {
                        addAll(currentList)
                        if (isCurrentMedicineComplete()) {
                            add(currentMed)
                        }
                    }
                }

                val dateStr = _draftPrescription.value.date
                if (dateStr.isNullOrBlank()) {
                    _errorMessage.value = "অনুগ্রহ করে তারিখ সিলেক্ট করুন"
                    _saveError.value = "অনুগ্রহ করে তারিখ সিলেক্ট করুন"
                    _isSaving.value = false
                    return@launch
                }

                // If currently editing a medicine, ensure duration is valid
                val currentDuration = currentMed.durationDays.toIntOrNull()
                if (currentMed.name.isNotBlank() && (currentDuration == null || currentDuration < 1)) {
                    val msg = "অনুগ্রহ করে ওষুধ সেবনের মেয়াদ (কত দিন) লিখুন"
                    _durationError.value = msg
                    _errorMessage.value = msg
                    _saveError.value = msg
                    _isSaving.value = false
                    return@launch
                }

                if (currentMed.name.isNotBlank() &&
                    currentMed.schedules.isNotEmpty() &&
                    currentMed.schedules.any { DoseAmountHelper.parseDoseAmount(it.doseAmount) == null }
                ) {
                    val msg = if (currentMed.schedules.any { it.doseAmount.isBlank() }) {
                        "অনুগ্রহ করে ডোজের পরিমাণ লিখুন"
                    } else {
                        "সঠিক ডোজ পরিমাণ লিখুন (যেমন: 1, 0.5, 1/2)"
                    }
                    _errorMessage.value = msg
                    _saveError.value = msg
                    _isSaving.value = false
                    return@launch
                }

                if (allMedicines.isEmpty()) {
                    val msg = if (currentMed.name.isBlank()) {
                        "কমপক্ষে একটি ওষুধ যোগ করা আবশ্যক"
                    } else {
                        "অনুগ্রহ করে ওষুধ সেবনের মেয়াদ (কত দিন) লিখুন"
                    }
                    _errorMessage.value = msg
                    _saveError.value = msg
                    _isSaving.value = false
                    return@launch
                }

                // Ensure every saved medicine has valid required fields, schedules, duration, and interval
                for (med in allMedicines) {
                    if (med.name.isBlank()) {
                        val msg = "অনুগ্রহ করে ওষুধের নাম লিখুন"
                        _errorMessage.value = msg
                        _saveError.value = msg
                        _isSaving.value = false
                        return@launch
                    }
                    if (med.type == null) {
                        val msg = "অনুগ্রহ করে ওষুধের ধরন সিলেক্ট করুন"
                        _errorMessage.value = msg
                        _saveError.value = msg
                        _isSaving.value = false
                        return@launch
                    }
                    if (med.strength.isNotBlank() &&
                        getUnitsForType(med.type).isNotEmpty() &&
                        med.strengthUnit.isBlank()
                    ) {
                        val msg = "অনুগ্রহ করে মাত্রার একক সিলেক্ট করুন"
                        _errorMessage.value = msg
                        _saveError.value = msg
                        _isSaving.value = false
                        return@launch
                    }
                    if (med.schedules.isEmpty()) {
                        val msg = "অনুগ্রহ করে সেবনের সময় সিলেক্ট করুন"
                        _errorMessage.value = msg
                        _saveError.value = msg
                        _isSaving.value = false
                        return@launch
                    }
                    if (med.schedules.any { it.doseAmount.isBlank() }) {
                        val msg = "অনুগ্রহ করে ডোজের পরিমাণ লিখুন"
                        _errorMessage.value = msg
                        _saveError.value = msg
                        _isSaving.value = false
                        return@launch
                    }
                    if (med.schedules.any { DoseAmountHelper.parseDoseAmount(it.doseAmount) == null }) {
                        val msg = "সঠিক ডোজ পরিমাণ লিখুন (যেমন: 1, 0.5, 1/2)"
                        _errorMessage.value = msg
                        _saveError.value = msg
                        _isSaving.value = false
                        return@launch
                    }
                    if (med.schedules.any { it.doseUnit.isBlank() }) {
                        val msg = "অনুগ্রহ করে ডোজের একক সিলেক্ট করুন"
                        _errorMessage.value = msg
                        _saveError.value = msg
                        _isSaving.value = false
                        return@launch
                    }
                    val parsedDuration = med.durationDays.toIntOrNull()
                    if (parsedDuration == null || parsedDuration < 1) {
                        val msg = "অনুগ্রহ করে ওষুধ সেবনের মেয়াদ (কত দিন) লিখুন"
                        _durationError.value = msg
                        _errorMessage.value = msg
                        _saveError.value = msg
                        _isSaving.value = false
                        return@launch
                    }
                    if (maxOf(1, med.intervalDays) > parsedDuration) {
                        val msg = "মেয়াদের চেয়ে বেশি বিরতি দেয়া যাবে না"
                        _intervalError.value = msg
                        _errorMessage.value = msg
                        _saveError.value = msg
                        _isSaving.value = false
                        return@launch
                    }
                }

                val activeEditingPresId = _editingPrescription.value?.id
                    ?: _editingPrescriptionId.value
                    ?: _draftPrescription.value.id
                val activeEditingParentId = _editingPrescription.value?.parentId
                    ?: _editingParentId.value
                val isEditMode = (_saveMode.value != PrescriptionSaveType.SAVE_NEW &&
                    (activeEditingPresId != null || activeEditingParentId != null || _editingMedicineId.value != null)) ||
                    (activeEditingPresId != null && activeEditingParentId != null)

                val effectiveDraft = _draftPrescription.value.copy(medicines = allMedicines)

                // Resolve the original latest prescription when in Edit Mode
                val original = if (isEditMode) {
                    activeEditingParentId?.let {
                        prescriptionRepository.getLatestPrescriptionWithMedicinesByParentId(it)
                    } ?: activeEditingPresId?.let { presId ->
                        val byId = prescriptionRepository.getPrescriptionWithMedicinesById(presId)
                        if (byId != null && !byId.prescription.isLatest) {
                            prescriptionRepository.getLatestPrescriptionWithMedicinesByParentId(byId.prescription.parentId) ?: byId
                        } else {
                            byId
                        }
                    } ?: _editingMedicineId.value?.let { medId ->
                        prescriptionRepository.getMedicineById(medId)?.let { med ->
                            val byId = prescriptionRepository.getPrescriptionWithMedicinesById(med.prescriptionId)
                            if (byId != null && !byId.prescription.isLatest) {
                                prescriptionRepository.getLatestPrescriptionWithMedicinesByParentId(byId.prescription.parentId) ?: byId
                            } else {
                                byId
                            }
                        }
                    }
                } else {
                    null
                }

                // Bug #19 Part 2: If editing an existing prescription, check if anything actually changed
                if (original != null) {
                    val hasChanged = hasPrescriptionChanged(original, effectiveDraft)
                    if (!hasChanged) {
                        // If NO CHANGES detected:
                        // 1. Do NOT insert a new version into the database.
                        // 2. Show Toast/Snackbar: "কোনো পরিবর্তন করা হয়নি"
                        // 3. Close the carousel (navigate back).
                        _toastMessage.value = "কোনো পরিবর্তন করা হয়নি"
                        _editingPrescription.value = null
                        _editingPrescriptionId.value = null
                        _editingParentId.value = null
                        _editingMedicineId.value = null
                        _initialPage.value = 0
                        _selectedMedicineIndex.value = null
                        _draftPrescription.value = DraftPrescription()
                        _draftMedicine.value = DraftMedicine()
                        _isSaving.value = false
                        if (onNoChanges != null) {
                            onNoChanges()
                        } else {
                            onSuccess()
                        }
                        return@launch
                    }
                }

                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                val newPrescriptionId = UUID.randomUUID().toString()
                val existingPres = original?.prescription ?: _editingPrescription.value

                val (parentId, versionNumber) = if (isEditMode && existingPres != null) {
                    // Edit Mode: New version of the EXISTING prescription (same parentId, versionNumber + 1, isLatest = true)
                    existingPres.parentId to (existingPres.versionNumber + 1)
                } else {
                    // New Mode: Brand new prescription with a new UUID parentId
                    newPrescriptionId to 1
                }

                val prescriptionEntity = PrescriptionEntity(
                    id = newPrescriptionId,
                    parentId = parentId,
                    versionNumber = versionNumber,
                    isLatest = true,
                    date = dateStr,
                    doctorName = _draftPrescription.value.doctorName.ifBlank { null },
                    location = _draftPrescription.value.location.ifBlank { null },
                    note = _draftPrescription.value.note.ifBlank { null },
                    createdAt = nowIso,
                    prescriptionImagePaths = _draftPrescription.value.prescriptionImagePaths
                )

                val medicineEntities = mutableListOf<MedicineEntity>()
                val scheduleEntities = mutableListOf<DoseScheduleEntity>()

                for (draftMed in allMedicines) {
                    val medId = UUID.randomUUID().toString()
                    val medLineageId = draftMed.lineageId?.takeIf { it.isNotBlank() } ?: medId
                    val medType = draftMed.type ?: MedicineType.TABLET

                    val medEntity = MedicineEntity(
                        id = medId,
                        prescriptionId = newPrescriptionId,
                        name = draftMed.name.trim(),
                        type = medType,
                        strength = draftMed.strength.toDoubleOrNull(),
                        strengthUnit = draftMed.strengthUnit.ifBlank { null },
                        durationDays = draftMed.durationDays.toIntOrNull(),
                        intervalDays = maxOf(1, draftMed.intervalDays),
                        foodRule = draftMed.foodRule,
                        note = draftMed.note.ifBlank { null },
                        createdAt = nowIso,
                        imagePath = draftMed.imagePath,
                        lineageId = medLineageId,
                        stockCount = maxOf(0, draftMed.stockCount),
                        initialStock = maxOf(0, draftMed.stockCount)
                    )
                    medicineEntities.add(medEntity)

                    for (sched in draftMed.schedules) {
                        val schedEntity = DoseScheduleEntity(
                            id = UUID.randomUUID().toString(),
                            medicineId = medId,
                            slot = sched.slot,
                            time = sched.time,
                            doseAmount = DoseAmountHelper.parseDoseAmount(sched.doseAmount) ?: 1.0,
                            doseUnit = sched.doseUnit.trim(),
                            foodRuleOverride = null,
                            foodRule = sched.foodRule
                        )
                        scheduleEntities.add(schedEntity)
                    }
                }

                val stalePendingReminderIds = if (isEditMode && existingPres != null && reminderRepository != null) {
                    reminderRepository.getFuturePendingReminderIdsByParentId(
                        parentId = existingPres.parentId,
                        now = nowIso
                    )
                } else {
                    emptyList()
                }

                if (isEditMode && existingPres != null) {
                    prescriptionRepository.saveNewPrescriptionVersion(
                        oldPrescriptionId = existingPres.id,
                        newPrescription = prescriptionEntity,
                        medicines = medicineEntities,
                        schedules = scheduleEntities
                    )
                } else {
                    prescriptionRepository.saveFullPrescription(
                        prescription = prescriptionEntity,
                        medicines = medicineEntities,
                        schedules = scheduleEntities
                    )
                }

                if (stalePendingReminderIds.isNotEmpty()) {
                    if (reminderScheduler != null) {
                        for (reminderId in stalePendingReminderIds) {
                            reminderScheduler.cancelReminder(reminderId)
                        }
                    } else {
                        Log.d(
                            "PrescriptionCarouselVM",
                            "reminderScheduler is null; skipping alarm cancellation for ${stalePendingReminderIds.size} stale reminders"
                        )
                    }
                    reminderRepository?.deleteRemindersByIds(stalePendingReminderIds)
                }

                // 4. INTEGRATION: Generate Reminders for the duration and schedule alarms according to intervalDays
                if (reminderRepository != null) {
                    val startDate = parsePrescriptionDateToLocalDate(dateStr) ?: LocalDate.now()

                    val generatedReminders = mutableListOf<Pair<ReminderEntity, String>>()
                    val schedulesByMedicine = scheduleEntities.groupBy { it.medicineId }

                    for (medEntity in medicineEntities) {
                        val medSchedules = schedulesByMedicine[medEntity.id] ?: emptyList()
                        val duration = maxOf(1, medEntity.durationDays ?: 7)
                        val interval = maxOf(1, medEntity.intervalDays)

                        for (dayOffset in 0 until duration) {
                            if (dayOffset % interval != 0) continue

                            val targetDate = startDate.plusDays(dayOffset.toLong())
                            val scheduledDate = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                            for (sched in medSchedules) {
                                val scheduledAt = "${scheduledDate}T${sched.time}:00"
                                val reminder = ReminderEntity.createDeterministic(
                                    doseScheduleId = sched.id,
                                    scheduledDate = scheduledDate,
                                    scheduledAt = scheduledAt,
                                    status = ReminderStatus.PENDING
                                )
                                generatedReminders.add(reminder to medEntity.name)
                            }
                        }
                    }

                    if (generatedReminders.isNotEmpty()) {
                        reminderRepository.insertReminders(generatedReminders.map { it.first })

                        if (reminderScheduler != null) {
                            for ((reminder, medicineName) in generatedReminders) {
                                reminderScheduler.scheduleReminder(reminder, medicineName)
                            }
                        }
                    }
                }

                val completedSaveType = _saveMode.value
                _editingPrescription.value = null
                _editingPrescriptionId.value = null
                _editingParentId.value = null
                _editingMedicineId.value = null
                _initialPage.value = 0
                _selectedMedicineIndex.value = null
                _draftPrescription.value = DraftPrescription()
                _draftMedicine.value = DraftMedicine()
                _saveMode.value = PrescriptionSaveType.SAVE_NEW
                _isSaving.value = false
                onSuccessWithType?.invoke(completedSaveType)
                onSuccess()
            } catch (e: Exception) {
                val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "ডাটাবেজ সংরক্ষণে ত্রুটি হয়েছে"
                _errorMessage.value = detail
                _saveError.value = detail
                _isSaving.value = false
            }
        }
    }

    /**
     * Alias for reviewAndSave to match naming conventions.
     */
    fun savePrescription(
        onSuccess: () -> Unit,
        onNoChanges: (() -> Unit)? = null,
        onSuccessWithType: ((PrescriptionSaveType) -> Unit)? = null
    ): kotlinx.coroutines.Job = reviewAndSave(onSuccess, onNoChanges, onSuccessWithType)

    fun loadPrescriptionForEditing(
        prescriptionWithMedicines: com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
    ) {
        _saveMode.value = PrescriptionSaveType.EDIT_PRESCRIPTION
        val pres = prescriptionWithMedicines.prescription
        _editingPrescription.value = pres
        _editingPrescriptionId.value = pres.id
        _editingParentId.value = pres.parentId
        _editingMedicineId.value = null
        _initialPage.value = 0
        val draftMeds = prescriptionWithMedicines.medicines.map { medWithSched ->
            val draftScheds = medWithSched.doseSchedules.map { sWithRem ->
                val sched = sWithRem.doseSchedule
                DraftSchedule(
                    slot = sched.slot,
                    time = sched.time,
                    doseAmount = DoseAmountHelper.formatDoseAmount(sched.doseAmount, useBengaliDigits = false),
                    doseUnit = sched.doseUnit,
                    isExpanded = true,
                    foodRule = sched.foodRuleOverride ?: sched.foodRule,
                    id = sched.id
                )
            }
            DraftMedicine(
                name = medWithSched.medicine.name,
                type = medWithSched.medicine.type,
                strength = medWithSched.medicine.strength?.toString()?.removeSuffix(".0") ?: "",
                strengthUnit = medWithSched.medicine.strengthUnit ?: "",
                schedules = draftScheds,
                foodRule = medWithSched.medicine.foodRule,
                durationDays = medWithSched.medicine.durationDays?.toString() ?: "7",
                intervalDays = medWithSched.medicine.intervalDays,
                note = medWithSched.medicine.note ?: "",
                imagePath = medWithSched.medicine.imagePath,
                lineageId = medWithSched.medicine.lineageId,
                stockCount = medWithSched.medicine.stockCount
            )
        }

        if (draftMeds.isNotEmpty()) {
            _selectedMedicineIndex.value = 0
            _draftMedicine.value = draftMeds.first()
            _draftPrescription.value = DraftPrescription(
                id = pres.id,
                date = pres.date,
                doctorName = pres.doctorName ?: "",
                location = pres.location ?: "",
                medicines = draftMeds,
                prescriptionImagePaths = pres.prescriptionImagePaths,
                note = pres.note ?: ""
            )
        } else {
            _selectedMedicineIndex.value = null
            _draftMedicine.value = DraftMedicine()
            _draftPrescription.value = DraftPrescription(
                id = pres.id,
                date = pres.date,
                doctorName = pres.doctorName ?: "",
                location = pres.location ?: "",
                medicines = emptyList(),
                prescriptionImagePaths = pres.prescriptionImagePaths,
                note = pres.note ?: ""
            )
        }
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
    }

    fun loadPrescriptionForAddingMedicine(
        prescriptionWithMedicines: com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
    ) {
        _saveMode.value = PrescriptionSaveType.ADD_MEDICINE
        val pres = prescriptionWithMedicines.prescription
        _editingPrescription.value = pres
        _editingPrescriptionId.value = pres.id
        _editingParentId.value = pres.parentId
        _editingMedicineId.value = null
        _initialPage.value = 1
        val draftMeds = prescriptionWithMedicines.medicines.map { medWithSched ->
            val draftScheds = medWithSched.doseSchedules.map { sWithRem ->
                val sched = sWithRem.doseSchedule
                DraftSchedule(
                    slot = sched.slot,
                    time = sched.time,
                    doseAmount = DoseAmountHelper.formatDoseAmount(sched.doseAmount, useBengaliDigits = false),
                    doseUnit = sched.doseUnit,
                    isExpanded = true,
                    foodRule = sched.foodRuleOverride ?: sched.foodRule,
                    id = sched.id
                )
            }
            DraftMedicine(
                name = medWithSched.medicine.name,
                type = medWithSched.medicine.type,
                strength = medWithSched.medicine.strength?.toString()?.removeSuffix(".0") ?: "",
                strengthUnit = medWithSched.medicine.strengthUnit ?: "",
                schedules = draftScheds,
                foodRule = medWithSched.medicine.foodRule,
                durationDays = medWithSched.medicine.durationDays?.toString() ?: "7",
                intervalDays = medWithSched.medicine.intervalDays,
                note = medWithSched.medicine.note ?: "",
                imagePath = medWithSched.medicine.imagePath,
                lineageId = medWithSched.medicine.lineageId,
                stockCount = medWithSched.medicine.stockCount
            )
        }

        // Fresh medicine for Card 2, all existing medicines preserved in draftPrescription
        _selectedMedicineIndex.value = null
        _draftMedicine.value = DraftMedicine()
        _draftPrescription.value = DraftPrescription(
            id = pres.id,
            date = pres.date,
            doctorName = pres.doctorName ?: "",
            location = pres.location ?: "",
            medicines = draftMeds,
            prescriptionImagePaths = pres.prescriptionImagePaths,
            note = pres.note ?: ""
        )
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
    }

    fun loadPrescriptionForEditingMedicine(
        prescriptionWithMedicines: com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines,
        medicineId: String
    ) {
        _saveMode.value = PrescriptionSaveType.EDIT_MEDICINE
        val pres = prescriptionWithMedicines.prescription
        _editingPrescription.value = pres
        _editingPrescriptionId.value = pres.id
        _editingParentId.value = pres.parentId
        _editingMedicineId.value = medicineId
        _initialPage.value = 1
        val draftMeds = prescriptionWithMedicines.medicines.map { medWithSched ->
            val draftScheds = medWithSched.doseSchedules.map { sWithRem ->
                val sched = sWithRem.doseSchedule
                DraftSchedule(
                    slot = sched.slot,
                    time = sched.time,
                    doseAmount = DoseAmountHelper.formatDoseAmount(sched.doseAmount, useBengaliDigits = false),
                    doseUnit = sched.doseUnit,
                    isExpanded = true,
                    foodRule = sched.foodRuleOverride ?: sched.foodRule,
                    id = sched.id
                )
            }
            DraftMedicine(
                name = medWithSched.medicine.name,
                type = medWithSched.medicine.type,
                strength = medWithSched.medicine.strength?.toString()?.removeSuffix(".0") ?: "",
                strengthUnit = medWithSched.medicine.strengthUnit ?: "",
                schedules = draftScheds,
                foodRule = medWithSched.medicine.foodRule,
                durationDays = medWithSched.medicine.durationDays?.toString() ?: "7",
                intervalDays = medWithSched.medicine.intervalDays,
                note = medWithSched.medicine.note ?: "",
                imagePath = medWithSched.medicine.imagePath,
                lineageId = medWithSched.medicine.lineageId,
                stockCount = medWithSched.medicine.stockCount
            )
        }

        val targetIndex = prescriptionWithMedicines.medicines.indexOfFirst { it.medicine.id == medicineId }.coerceAtLeast(0)
        _selectedMedicineIndex.value = targetIndex
        _draftMedicine.value = draftMeds.getOrElse(targetIndex) { DraftMedicine() }
        _draftPrescription.value = DraftPrescription(
            id = pres.id,
            date = pres.date,
            doctorName = pres.doctorName ?: "",
            location = pres.location ?: "",
            medicines = draftMeds,
            prescriptionImagePaths = pres.prescriptionImagePaths,
            note = pres.note ?: ""
        )
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
        _intervalError.value = null
    }

    fun loadPrescriptionByIdForEditing(prescriptionId: String): kotlinx.coroutines.Job {
        _saveMode.value = PrescriptionSaveType.EDIT_PRESCRIPTION
        _editingPrescriptionId.value = prescriptionId
        _editingMedicineId.value = null
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
        _intervalError.value = null

        return viewModelScope.launch {
            val presWithMeds = prescriptionRepository.getPrescriptionWithMedicinesById(prescriptionId)
                ?: return@launch
            val targetPresWithMeds = if (presWithMeds.prescription.isLatest) {
                presWithMeds
            } else {
                prescriptionRepository.getLatestPrescriptionWithMedicinesByParentId(presWithMeds.prescription.parentId)
                    ?: presWithMeds
            }
            loadPrescriptionForEditing(targetPresWithMeds)
        }
    }

    fun loadMedicineForEdit(medicineId: String): kotlinx.coroutines.Job {
        _saveMode.value = PrescriptionSaveType.EDIT_MEDICINE
        _editingMedicineId.value = medicineId
        _initialPage.value = 1
        _errorMessage.value = null
        _saveError.value = null
        _durationError.value = null
        _intervalError.value = null

        return viewModelScope.launch {
            val medicine = prescriptionRepository.getMedicineById(medicineId) ?: return@launch
            val presWithMeds = prescriptionRepository.getPrescriptionWithMedicinesById(medicine.prescriptionId)
                ?: return@launch
            val targetPresWithMeds = if (presWithMeds.prescription.isLatest) {
                presWithMeds
            } else {
                prescriptionRepository.getLatestPrescriptionWithMedicinesByParentId(presWithMeds.prescription.parentId)
                    ?: presWithMeds
            }
            val resolvedMedicineId = if (targetPresWithMeds.medicines.any { it.medicine.id == medicineId }) {
                medicineId
            } else {
                targetPresWithMeds.medicines.firstOrNull {
                    medicine.lineageId != null && it.medicine.lineageId == medicine.lineageId
                }?.medicine?.id ?: medicineId
            }
            loadPrescriptionForEditingMedicine(targetPresWithMeds, resolvedMedicineId)
        }
    }

    fun deletePrescription(prescriptionId: String, onDeleted: () -> Unit = {}): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            try {
                val pendingReminderIds = reminderRepository?.getPendingReminderIdsByPrescriptionId(prescriptionId).orEmpty()
                prescriptionRepository.deletePrescriptionById(prescriptionId)
                if (pendingReminderIds.isNotEmpty()) {
                    if (reminderScheduler != null) {
                        for (reminderId in pendingReminderIds) {
                            reminderScheduler.cancelReminder(reminderId)
                        }
                    } else {
                        Log.d(
                            "PrescriptionCarouselVM",
                            "reminderScheduler is null; skipping alarm cancellation for deleted prescription $prescriptionId"
                        )
                    }
                }
                if (_editingPrescriptionId.value == prescriptionId) {
                    resetDraft()
                }
                onDeleted()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun deleteMedicine(medicine: MedicineEntity, onDeleted: () -> Unit = {}): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            try {
                val pendingReminderIds = reminderRepository?.getPendingReminderIdsByMedicineId(medicine.id).orEmpty()
                prescriptionRepository.deleteMedicine(medicine)
                if (pendingReminderIds.isNotEmpty()) {
                    if (reminderScheduler != null) {
                        for (reminderId in pendingReminderIds) {
                            reminderScheduler.cancelReminder(reminderId)
                        }
                    } else {
                        Log.d(
                            "PrescriptionCarouselVM",
                            "reminderScheduler is null; skipping alarm cancellation for deleted medicine ${medicine.id}"
                        )
                    }
                }
                onDeleted()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun loadForEditing(
        medicine: MedicineEntity,
        schedules: List<DoseScheduleEntity>,
        prescription: PrescriptionEntity? = null
    ) {
        _saveMode.value = PrescriptionSaveType.EDIT_MEDICINE
        _editingPrescription.value = prescription
        _editingPrescriptionId.value = prescription?.id ?: medicine.prescriptionId
        _editingParentId.value = prescription?.parentId
        _editingMedicineId.value = medicine.id
        _initialPage.value = 1
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _draftPrescription.value = DraftPrescription(
            id = prescription?.id,
            date = prescription?.date ?: todayStr,
            doctorName = prescription?.doctorName ?: "",
            location = prescription?.location ?: "",
            medicines = emptyList(),
            prescriptionImagePaths = prescription?.prescriptionImagePaths ?: emptyList(),
            note = prescription?.note ?: ""
        )
        val draftSchedules = schedules.map { sched ->
            DraftSchedule(
                slot = sched.slot,
                time = sched.time,
                doseAmount = DoseAmountHelper.formatDoseAmount(sched.doseAmount, useBengaliDigits = false),
                doseUnit = sched.doseUnit,
                isExpanded = true,
                foodRule = sched.foodRuleOverride ?: sched.foodRule,
                id = sched.id
            )
        }
        val defaultDoseUnit = getDoseUnitsForType(medicine.type).firstOrNull() ?: "টি"
        _draftMedicine.value = DraftMedicine(
            name = medicine.name,
            type = medicine.type,
            strength = medicine.strength?.toString()?.removeSuffix(".0") ?: "",
            strengthUnit = medicine.strengthUnit ?: "",
            schedules = if (draftSchedules.isNotEmpty()) draftSchedules else listOf(
                DraftSchedule(
                    slot = DoseSlot.MORNING,
                    time = "08:00",
                    doseAmount = "1",
                    doseUnit = defaultDoseUnit,
                    isExpanded = true
                )
            ),
            foodRule = medicine.foodRule,
            durationDays = medicine.durationDays?.toString() ?: "7",
            intervalDays = medicine.intervalDays,
            note = medicine.note ?: "",
            imagePath = medicine.imagePath,
            lineageId = medicine.lineageId,
            stockCount = medicine.stockCount
        )
        _errorMessage.value = null
        _saveError.value = null
    }
}

class PrescriptionCarouselViewModelFactory(
    private val repository: PrescriptionRepository,
    private val reminderRepository: ReminderRepository? = null,
    private val reminderScheduler: ReminderScheduler? = null,
    private val themePreferences: ThemePreferences? = null,
    private val ocrHelper: OcrHelper? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PrescriptionCarouselViewModel(
            prescriptionRepository = repository,
            reminderRepository = reminderRepository,
            reminderScheduler = reminderScheduler,
            themePreferences = themePreferences,
            ocrHelper = ocrHelper
        ) as T
    }
}
