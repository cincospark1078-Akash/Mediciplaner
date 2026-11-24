package com.mediciplanner.app.ui.screens.prescription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediciplanner.app.data.local.relation.MedicineWithSchedules
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import com.mediciplanner.app.data.repository.PrescriptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ChangeType {
    NEW,
    EDITED,
    UNCHANGED
}

data class PrescriptionDetailsUiState(
    val currentPrescription: PrescriptionWithMedicines? = null,
    val previousPrescription: PrescriptionWithMedicines? = null,
    val medicineChangeTypes: Map<String, ChangeType> = emptyMap(),
    val isDateChanged: Boolean = false,
    val isDoctorChanged: Boolean = false,
    val isLocationChanged: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * Compares [current] prescription version against its immediate [previous] version
 * and returns a map keyed by current Medicine ID to [ChangeType].
 *
 * - If a medicine (by ID or lineageId) exists in [current] but NOT in [previous] -> [ChangeType.NEW]
 * - If it exists in both, but name/type/strength/dose/schedule/duration/interval/foodRule/note/image differs -> [ChangeType.EDITED]
 * - Otherwise -> [ChangeType.UNCHANGED]
 */
fun compareVersions(
    current: PrescriptionWithMedicines,
    previous: PrescriptionWithMedicines
): Map<String, ChangeType> {
    val previousById = previous.medicines.associateBy { it.medicine.id }
    val previousByLineage = previous.medicines.associateBy {
        it.medicine.lineageId.ifBlank { it.medicine.id }
    }

    val result = LinkedHashMap<String, ChangeType>(current.medicines.size)
    for (currMedWithSched in current.medicines) {
        val currMed = currMedWithSched.medicine
        val prevMatch = previousById[currMed.id]
            ?: previousByLineage[currMed.lineageId.ifBlank { currMed.id }]

        val changeType = if (prevMatch == null) {
            ChangeType.NEW
        } else if (hasMedicineChanged(currMedWithSched, prevMatch)) {
            ChangeType.EDITED
        } else {
            ChangeType.UNCHANGED
        }
        result[currMed.id] = changeType
    }
    return result
}

private fun hasMedicineChanged(
    current: MedicineWithSchedules,
    previous: MedicineWithSchedules
): Boolean {
    val currMed = current.medicine
    val prevMed = previous.medicine

    if (currMed.name.trim() != prevMed.name.trim()) return true
    if (currMed.type != prevMed.type) return true
    if (currMed.strength != prevMed.strength) return true
    if (currMed.strengthUnit.orEmpty().trim() != prevMed.strengthUnit.orEmpty().trim()) return true
    if (currMed.durationDays != prevMed.durationDays) return true
    if (maxOf(1, currMed.intervalDays) != maxOf(1, prevMed.intervalDays)) return true
    if (currMed.foodRule != prevMed.foodRule) return true
    if (currMed.note.orEmpty().trim() != prevMed.note.orEmpty().trim()) return true
    if (currMed.imagePath.orEmpty().trim() != prevMed.imagePath.orEmpty().trim()) return true

    val currScheds = current.doseSchedules.map { it.doseSchedule }
        .sortedWith(compareBy({ it.slot.name }, { it.time }))
    val prevScheds = previous.doseSchedules.map { it.doseSchedule }
        .sortedWith(compareBy({ it.slot.name }, { it.time }))

    if (currScheds.size != prevScheds.size) return true

    for (i in currScheds.indices) {
        val cs = currScheds[i]
        val ps = prevScheds[i]
        if (cs.slot != ps.slot) return true
        if (cs.time.trim() != ps.time.trim()) return true
        if (cs.doseAmount != ps.doseAmount) return true
        if (cs.doseUnit.trim() != ps.doseUnit.trim()) return true
        if ((cs.foodRuleOverride ?: cs.foodRule) != (ps.foodRuleOverride ?: ps.foodRule)) return true
    }

    return false
}

class PrescriptionDetailsViewModel(
    private val prescriptionRepository: PrescriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrescriptionDetailsUiState())
    val uiState: StateFlow<PrescriptionDetailsUiState> = _uiState.asStateFlow()

    fun loadVersion(current: PrescriptionWithMedicines): Job {
        val pres = current.prescription
        if (pres.versionNumber <= 1) {
            val unchangedMap = current.medicines.associate { it.medicine.id to ChangeType.UNCHANGED }
            _uiState.value = PrescriptionDetailsUiState(
                currentPrescription = current,
                previousPrescription = null,
                medicineChangeTypes = unchangedMap,
                isLoading = false
            )
            return Job().apply { complete() }
        }

        _uiState.value = PrescriptionDetailsUiState(
            currentPrescription = current,
            isLoading = true
        )

        return viewModelScope.launch {
            val previous = prescriptionRepository.getPrescriptionWithMedicinesByVersion(
                parentId = pres.parentId,
                versionNumber = pres.versionNumber - 1
            )
            val changeMap = if (previous != null) {
                compareVersions(current, previous)
            } else {
                current.medicines.associate { it.medicine.id to ChangeType.UNCHANGED }
            }
            val prevPres = previous?.prescription
            val dateChanged = prevPres != null &&
                normalizePrescriptionDate(pres.date) != normalizePrescriptionDate(prevPres.date)
            val doctorChanged = prevPres != null &&
                pres.doctorName.orEmpty().trim() != prevPres.doctorName.orEmpty().trim()
            val locationChanged = prevPres != null &&
                pres.location.orEmpty().trim() != prevPres.location.orEmpty().trim()

            _uiState.value = PrescriptionDetailsUiState(
                currentPrescription = current,
                previousPrescription = previous,
                medicineChangeTypes = changeMap,
                isDateChanged = dateChanged,
                isDoctorChanged = doctorChanged,
                isLocationChanged = locationChanged,
                isLoading = false
            )
        }
    }

    private fun normalizePrescriptionDate(raw: String?): String {
        val trimmed = raw.orEmpty().trim()
        return if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
            trimmed.take(10)
        } else {
            trimmed
        }
    }
}

class PrescriptionDetailsViewModelFactory(
    private val repository: PrescriptionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PrescriptionDetailsViewModel(repository) as T
    }
}
