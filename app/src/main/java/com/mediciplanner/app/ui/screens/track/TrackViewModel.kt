package com.mediciplanner.app.ui.screens.track

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediciplanner.app.R
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class TrackToastMessage(
    val id: Long = System.nanoTime(),
    @StringRes val messageResId: Int
)

data class TrackDoseItem(
    val reminderId: String?,
    val medicine: MedicineEntity,
    val schedule: DoseScheduleEntity,
    val status: ReminderStatus,
    val scheduledTime: String,
    val takenAt: String? = null
)

data class TrackUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val dateStripDates: List<LocalDate> = buildDateStripDates(LocalDate.now()),
    val missedCount: Int = 0,
    val missedMedicinesGroupedBySlot: Map<String, List<MedicineEntity>> = emptyMap(),
    val allMedicinesForDate: List<MedicineEntity> = emptyList(),
    val missedDoseItemsGroupedBySlot: Map<String, List<TrackDoseItem>> = emptyMap(),
    val allDosesGroupedBySlot: Map<String, List<TrackDoseItem>> = emptyMap(),
    val groupedHistory: Map<DoseSlot, List<TrackDoseItem>> = emptyMap(),
    val trackedDoses: List<TrackDoseItem> = emptyList(),
    val totalDosesForDate: Int = 0,
    val completedDosesForDate: Int = 0,
    val isLoading: Boolean = false,
    val timeFormat: String = ThemePreferences.TIME_FORMAT_12H,
    val selectedSlot: DoseSlot? = null
) {
    val is24Hour: Boolean get() = timeFormat == ThemePreferences.TIME_FORMAT_24H
    val filteredGroupedHistory: Map<DoseSlot, List<TrackDoseItem>>
        get() {
            val slot = selectedSlot
            return if (slot == null) groupedHistory else groupedHistory.filterKeys { it == slot }
        }
}

fun buildDateStripDates(today: LocalDate = LocalDate.now()): List<LocalDate> {
    // Past 7 days + today + next 6 days (total 7 days from today forward, 14 days total)
    return (-7L..6L).map { offset -> today.plusDays(offset) }
}

class TrackViewModel(
    private val prescriptionRepository: PrescriptionRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler? = null,
    private val themePreferences: ThemePreferences? = null
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedSlot = MutableStateFlow<DoseSlot?>(null)
    val selectedSlot: StateFlow<DoseSlot?> = _selectedSlot

    private val _toastMessage = MutableStateFlow<TrackToastMessage?>(null)
    val toastMessage: StateFlow<TrackToastMessage?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    val uiState: StateFlow<TrackUiState> = combine(
        _selectedDate,
        prescriptionRepository.allPrescriptionsWithMedicines,
        reminderRepository.allReminders,
        themePreferences?.timeFormatFlow ?: flowOf(ThemePreferences.TIME_FORMAT_12H),
        _selectedSlot
    ) { date, prescriptions, allReminders, timeFormat, selectedSlot ->
        val dateIso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val dateStripDates = buildDateStripDates(today)

        // Find medicines active on selected date
        val activeDoseItems = mutableListOf<TrackDoseItem>()

        for (prescWithMeds in prescriptions) {
            val prescDate = try {
                LocalDate.parse(prescWithMeds.prescription.date.take(10))
            } catch (e: Exception) {
                date
            }

            for (medWithSched in prescWithMeds.medicines) {
                val med = medWithSched.medicine
                val durationDays = med.durationDays?.toLong() ?: 30L
                val endDate = prescDate.plusDays(durationDays)

                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(prescDate, date)
                val interval = maxOf(1, med.intervalDays)
                val isIntervalDay = daysBetween >= 0 && (daysBetween % interval == 0L)

                // Check if selected date falls within prescription duration and matches interval
                if (!date.isBefore(prescDate) && !date.isAfter(endDate) && isIntervalDay) {
                    for (schedWithRem in medWithSched.doseSchedules) {
                        val sched = schedWithRem.doseSchedule

                        // Find matching reminder in database
                        val matchingReminder = allReminders.find { rem ->
                            rem.doseScheduleId == sched.id && rem.scheduledAt.startsWith(dateIso)
                        }

                        val isPastBy30Minutes = try {
                            if (date.isBefore(today)) {
                                true
                            } else if (date == today) {
                                val schedDateTime = LocalDateTime.parse("${dateIso}T${sched.time}:00")
                                // Dose is missed if its scheduled time has passed by more than 30 minutes
                                now.isAfter(schedDateTime.plusMinutes(30))
                            } else {
                                false
                            }
                        } catch (e: Exception) {
                            date.isBefore(today)
                        }

                        val resolvedStatus = when {
                            matchingReminder?.status == ReminderStatus.TAKEN -> ReminderStatus.TAKEN
                            matchingReminder?.status == ReminderStatus.MISSED -> ReminderStatus.MISSED
                            isPastBy30Minutes -> ReminderStatus.MISSED
                            else -> ReminderStatus.PENDING
                        }

                        activeDoseItems.add(
                            TrackDoseItem(
                                reminderId = matchingReminder?.id,
                                medicine = med,
                                schedule = sched,
                                status = resolvedStatus,
                                scheduledTime = sched.time,
                                takenAt = matchingReminder?.takenAt
                            )
                        )
                    }
                }
            }
        }

        // Sort dose items by scheduled time
        activeDoseItems.sortBy { it.scheduledTime }

        val completed = activeDoseItems.count { it.status == ReminderStatus.TAKEN }
        val missedItems = activeDoseItems.filter { it.status == ReminderStatus.MISSED }
        val missedCount = missedItems.size

        val slotOrder = listOf(
            DoseSlot.MORNING,
            DoseSlot.NOON,
            DoseSlot.AFTERNOON,
            DoseSlot.EVENING,
            DoseSlot.NIGHT,
            DoseSlot.CUSTOM
        )

        // Group missed items by slot
        val missedDosesGrouped = LinkedHashMap<String, MutableList<TrackDoseItem>>()
        val missedMedsGrouped = LinkedHashMap<String, MutableList<MedicineEntity>>()

        for (slot in slotOrder) {
            val slotMissed = missedItems.filter { it.schedule.slot == slot }
            if (slotMissed.isNotEmpty()) {
                missedDosesGrouped[slot.name] = slotMissed.toMutableList()
                val distinctMeds = slotMissed.map { it.medicine }.distinctBy { it.id }.toMutableList()
                missedMedsGrouped[slot.name] = distinctMeds
            }
        }

        // Group all doses for date by slot in strict order: MORNING, NOON, AFTERNOON, EVENING, NIGHT, CUSTOM
        val groupedHistory = LinkedHashMap<DoseSlot, List<TrackDoseItem>>()
        val allDosesGrouped = LinkedHashMap<String, MutableList<TrackDoseItem>>()
        for (slot in slotOrder) {
            val slotDoses = activeDoseItems.filter { it.schedule.slot == slot }
            if (slotDoses.isNotEmpty()) {
                groupedHistory[slot] = slotDoses
                allDosesGrouped[slot.name] = slotDoses.toMutableList()
            }
        }

        val allMedsDistinct = activeDoseItems.map { it.medicine }.distinctBy { it.id }

        TrackUiState(
            selectedDate = date,
            dateStripDates = dateStripDates,
            missedCount = missedCount,
            missedMedicinesGroupedBySlot = missedMedsGrouped,
            allMedicinesForDate = allMedsDistinct,
            missedDoseItemsGroupedBySlot = missedDosesGrouped,
            allDosesGroupedBySlot = allDosesGrouped,
            groupedHistory = groupedHistory,
            trackedDoses = activeDoseItems,
            totalDosesForDate = activeDoseItems.size,
            completedDosesForDate = completed,
            isLoading = false,
            timeFormat = timeFormat,
            selectedSlot = selectedSlot
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrackUiState(isLoading = true)
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectSlot(slot: DoseSlot?) {
        _selectedSlot.value = slot
    }

    fun markAsTaken(doseItem: TrackDoseItem) {
        viewModelScope.launch {
            val wasAlreadyTaken = doseItem.status == ReminderStatus.TAKEN
            val wasMissed = doseItem.status == ReminderStatus.MISSED
            val dateIso = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            val resolvedReminderId = if (doseItem.reminderId != null) {
                reminderRepository.markAsTaken(doseItem.reminderId, nowIso)
                doseItem.reminderId
            } else {
                val reminder = reminderRepository.createDeterministicReminder(
                    doseScheduleId = doseItem.schedule.id,
                    scheduledDate = dateIso,
                    scheduledAt = "$dateIso ${doseItem.schedule.time}:00",
                    status = ReminderStatus.TAKEN
                )
                reminderRepository.markAsTaken(reminder.id, nowIso)
                reminder.id
            }

            var stockOut = false
            if (!wasAlreadyTaken) {
                val stockResult = prescriptionRepository.decrementStockWithResult(
                    medicineId = doseItem.medicine.id,
                    doseAmount = doseItem.schedule.doseAmount
                )
                if (stockResult != null) {
                    if (stockResult.crossedLowStockThreshold) {
                        reminderScheduler?.notifyLowStock(stockResult.medicine.id, stockResult.medicine.name)
                    }
                    if (stockResult.isOutOfStock) {
                        stockOut = true
                    }
                }
            }

            reminderScheduler?.cancelReminder(resolvedReminderId)

            when {
                stockOut -> _toastMessage.value = TrackToastMessage(messageResId = R.string.msg_stock_finished_refill)
                wasMissed -> _toastMessage.value = TrackToastMessage(messageResId = R.string.msg_track_missed_taken)
            }
        }
    }

    fun markAllMissedAsTaken() {
        viewModelScope.launch {
            val currentState = uiState.value
            val missedItems = currentState.trackedDoses.filter { it.status == ReminderStatus.MISSED }
            val dateIso = currentState.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            var anyStockOut = false

            for (doseItem in missedItems) {
                val resolvedReminderId = if (doseItem.reminderId != null) {
                    reminderRepository.markAsTaken(doseItem.reminderId, nowIso)
                    doseItem.reminderId
                } else {
                    val reminder = reminderRepository.createDeterministicReminder(
                        doseScheduleId = doseItem.schedule.id,
                        scheduledDate = dateIso,
                        scheduledAt = "$dateIso ${doseItem.schedule.time}:00",
                        status = ReminderStatus.TAKEN
                    )
                    reminderRepository.markAsTaken(reminder.id, nowIso)
                    reminder.id
                }
                val stockResult = prescriptionRepository.decrementStockWithResult(
                    medicineId = doseItem.medicine.id,
                    doseAmount = doseItem.schedule.doseAmount
                )
                if (stockResult != null) {
                    if (stockResult.crossedLowStockThreshold) {
                        reminderScheduler?.notifyLowStock(stockResult.medicine.id, stockResult.medicine.name)
                    }
                    if (stockResult.isOutOfStock) {
                        anyStockOut = true
                    }
                }
                reminderScheduler?.cancelReminder(resolvedReminderId)
            }

            if (missedItems.isNotEmpty()) {
                _toastMessage.value = TrackToastMessage(
                    messageResId = if (anyStockOut) {
                        R.string.msg_stock_finished_refill
                    } else {
                        R.string.msg_track_all_missed_taken
                    }
                )
            }
        }
    }
}

class TrackViewModelFactory(
    private val prescriptionRepository: PrescriptionRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler? = null,
    private val themePreferences: ThemePreferences? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TrackViewModel(prescriptionRepository, reminderRepository, reminderScheduler, themePreferences) as T
    }
}
