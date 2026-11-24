package com.mediciplanner.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.util.SlotActivationHelper
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.notification.ReminderScheduler
import androidx.annotation.StringRes
import com.mediciplanner.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HomeToastMessage(
    val id: Long = System.nanoTime(),
    @StringRes val messageResId: Int
)

data class MedicineScheduleInfo(
    val medicine: MedicineEntity,
    val schedule: DoseScheduleEntity,
    val reminder: ReminderEntity?,
    val isSlotActive: Boolean = SlotActivationHelper.isSlotActive(schedule.time)
)

enum class HomeEmptyState {
    NONE,
    NO_PRESCRIPTIONS,
    NO_DOSES_TODAY,
    ALL_DOSES_COMPLETED
}

data class HomeUiState(
    val nextReminder: ReminderEntity? = null,
    val nextReminderMedicine: MedicineEntity? = null,
    val nextReminderSchedule: DoseScheduleEntity? = null,
    val todayMedicinesGrouped: Map<String, List<MedicineEntity>> = emptyMap(),
    val todayScheduleInfoList: List<MedicineScheduleInfo> = emptyList(),
    val progressTaken: Int = 0,
    val progressMissed: Int = 0,
    val progressTotal: Int = 0,
    val progressScheduled: Int = 0,
    val themeMode: AppThemeMode = AppThemeMode.NEUTRAL,
    val userName: String = "",
    val userProfilePicPath: String = "",
    val greetingPreference: String = "universal",
    val customGreeting: String = "",
    val finalGreeting: String = "",
    val todayDateFormatted: String = "",
    val isLoading: Boolean = false,
    val hasPrescriptions: Boolean = false,
    val nextDoseDateFormatted: String? = null,
    val emptyState: HomeEmptyState = HomeEmptyState.NONE,
    val timeFormat: String = ThemePreferences.TIME_FORMAT_12H
) {
    val is24Hour: Boolean get() = timeFormat == ThemePreferences.TIME_FORMAT_24H
    val totalDoses: Int get() = progressScheduled
    val groupedMedicines: Map<String, List<MedicineEntity>> get() = todayMedicinesGrouped
    val personalizedGreeting: String
        get() = if (finalGreeting.isNotBlank()) {
            finalGreeting
        } else {
            buildGreetingString(greetingPreference, userName, customGreeting = customGreeting)
        }
}

private data class HomePrefsSnapshot(
    val themeMode: AppThemeMode,
    val userName: String,
    val userProfilePicPath: String,
    val greetingPreference: String,
    val timeFormat: String,
    val customGreeting: String
)

fun buildGreetingString(
    greetingPreference: String,
    userName: String,
    hour: Int = java.time.LocalTime.now().hour,
    customGreeting: String = ""
): String {
    val cleanName = userName.trim()
    val cleanCustom = customGreeting.trim()
    val baseGreeting = when (greetingPreference.trim().lowercase()) {
        "islamic", "salam", "আসসালামু আলাইকুম" -> "আসসালামু আলাইকুম"
        "hindu", "namaskar", "নমস্কার" -> "নমস্কার"
        "english", "hello" -> "Hello"
        "custom", "কাস্টম" -> {
            if (cleanCustom.isNotEmpty()) {
                cleanCustom
            } else {
                when (hour) {
                    in 5..11 -> "শুভ সকাল"
                    in 12..16 -> "শুভ দুপুর"
                    else -> "শুভ সন্ধ্যা"
                }
            }
        }
        "universal", "সাধারণ" -> {
            when (hour) {
                in 5..11 -> "শুভ সকাল"
                in 12..16 -> "শুভ দুপুর"
                else -> "শুভ সন্ধ্যা"
            }
        }
        else -> {
            when (hour) {
                in 5..11 -> "শুভ সকাল"
                in 12..16 -> "শুভ দুপুর"
                else -> "শুভ সন্ধ্যা"
            }
        }
    }

    return if (cleanName.isNotEmpty()) {
        "$baseGreeting, $cleanName"
    } else {
        baseGreeting
    }
}

fun formatPersonalizedGreeting(
    greetingPreference: String,
    userName: String,
    hour: Int = java.time.LocalTime.now().hour,
    customGreeting: String = ""
): String = buildGreetingString(greetingPreference, userName, hour, customGreeting)

fun toBengaliDigits(input: String): String {
    val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return input.map { char ->
        if (char in '0'..'9') {
            bengaliDigits[char - '0']
        } else {
            char
        }
    }.joinToString("")
}

fun formatBengaliDate(date: LocalDate): String {
    val day = toBengaliDigits(String.format(Locale.US, "%02d", date.dayOfMonth))
    val month = when (date.monthValue) {
        1 -> "জানুয়ারি"
        2 -> "ফেব্রুয়ারি"
        3 -> "মার্চ"
        4 -> "এপ্রিল"
        5 -> "মে"
        6 -> "জুন"
        7 -> "জুলাই"
        8 -> "আগস্ট"
        9 -> "সেপ্টেম্বর"
        10 -> "অক্টোবর"
        11 -> "নভেম্বর"
        12 -> "ডিসেম্বর"
        else -> ""
    }
    val year = toBengaliDigits(date.year.toString())
    return "$day $month, $year"
}

class HomeViewModel(
    private val prescriptionRepository: PrescriptionRepository,
    private val reminderRepository: ReminderRepository,
    private val themePreferences: ThemePreferences,
    private val reminderScheduler: ReminderScheduler? = null
) : ViewModel() {

    private val today: LocalDate get() = LocalDate.now()
    private val todayPrefix: String get() = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _toastMessage = MutableStateFlow<HomeToastMessage?>(null)
    val toastMessage: StateFlow<HomeToastMessage?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(650)
            _isRefreshing.value = false
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        prescriptionRepository.allPrescriptionsWithMedicines,
        reminderRepository.allReminders,
        combine(
            combine(
                themePreferences.themeModeFlow,
                themePreferences.userNameFlow,
                themePreferences.userProfilePicPathFlow
            ) { themeMode, userName, profilePicPath ->
                Triple(themeMode, userName, profilePicPath)
            },
            themePreferences.greetingPreferenceFlow,
            themePreferences.timeFormatFlow,
            themePreferences.customGreetingFlow
        ) { identityTriple, greetingPref, timeFormat, customGreeting ->
            HomePrefsSnapshot(
                themeMode = identityTriple.first,
                userName = identityTriple.second,
                userProfilePicPath = identityTriple.third,
                greetingPreference = greetingPref,
                timeFormat = timeFormat,
                customGreeting = customGreeting
            )
        }
    ) { prescriptionsWithMeds, allReminders, prefs ->
        val themeMode = prefs.themeMode
        val userName = prefs.userName
        val userProfilePicPath = prefs.userProfilePicPath
        val greetingPref = prefs.greetingPreference
        val timeFormat = prefs.timeFormat
        val customGreeting = prefs.customGreeting
        val formattedTodayBengali = formatBengaliDate(today)

        // Filter active prescriptions & medicines strictly for TODAY using java.time.LocalDate
        val activeMedicinesWithSchedules = mutableListOf<Pair<MedicineEntity, List<DoseScheduleEntity>>>()

        for (presWithMeds in prescriptionsWithMeds) {
            val createdAtStr = presWithMeds.prescription.createdAt
            val prescriptionDate = try {
                if (createdAtStr.length >= 10) {
                    LocalDate.parse(createdAtStr.substring(0, 10))
                } else {
                    today
                }
            } catch (e: Exception) {
                today
            }

            for (medWithScheds in presWithMeds.medicines) {
                val med = medWithScheds.medicine
                val duration = med.durationDays

                // Check if active for TODAY respecting intervalDays
                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(prescriptionDate, today)
                val interval = maxOf(1, med.intervalDays)
                val isIntervalDay = daysBetween >= 0 && (daysBetween % interval == 0L)

                val isActiveToday = if (duration == null || duration <= 0) {
                    !today.isBefore(prescriptionDate) && isIntervalDay
                } else {
                    val endDate = prescriptionDate.plusDays(duration.toLong())
                    !today.isBefore(prescriptionDate) && !today.isAfter(endDate) && isIntervalDay
                }

                if (isActiveToday) {
                    val schedules = medWithScheds.doseSchedules.map { it.doseSchedule }
                    if (schedules.isNotEmpty()) {
                        activeMedicinesWithSchedules.add(med to schedules)
                    }
                }
            }
        }

        val activeSchedulesWithMeds = mutableListOf<Pair<MedicineEntity, DoseScheduleEntity>>()
        for ((med, schedules) in activeMedicinesWithSchedules) {
            for (schedule in schedules) {
                activeSchedulesWithMeds.add(med to schedule)
            }
        }

        val todayRemindersByScheduleId = allReminders
            .filter { it.scheduledAt.startsWith(todayPrefix) }
            .associateBy { it.doseScheduleId }

        val scheduleInfoList = mutableListOf<MedicineScheduleInfo>()
        val groupedMap = mutableMapOf<String, MutableList<MedicineEntity>>()

        val nowDateTime = LocalDateTime.now()
        for ((med, schedule) in activeSchedulesWithMeds) {
            val slotKey = schedule.slot.name
            val list = groupedMap.getOrPut(slotKey) { mutableListOf() }
            if (!list.any { it.id == med.id }) {
                list.add(med)
            }

            val reminder = todayRemindersByScheduleId[schedule.id] ?: run {
                val scheduledAt = "${todayPrefix}T${schedule.time}:00"
                ReminderEntity.createDeterministic(
                    doseScheduleId = schedule.id,
                    scheduledDate = todayPrefix,
                    scheduledAt = scheduledAt,
                    status = ReminderStatus.PENDING
                )
            }

            scheduleInfoList.add(
                MedicineScheduleInfo(
                    medicine = med,
                    schedule = schedule,
                    reminder = reminder,
                    isSlotActive = SlotActivationHelper.isSlotActive(schedule.time, nowDateTime)
                )
            )
        }

        val slotOrder = listOf(
            DoseSlot.MORNING.name,
            DoseSlot.NOON.name,
            DoseSlot.AFTERNOON.name,
            DoseSlot.EVENING.name,
            DoseSlot.NIGHT.name,
            DoseSlot.CUSTOM.name
        )

        val sortedGroupedMap = LinkedHashMap<String, List<MedicineEntity>>()
        for (slotName in slotOrder) {
            if (groupedMap.containsKey(slotName)) {
                sortedGroupedMap[slotName] = groupedMap[slotName] ?: emptyList()
            }
        }

        val takenCount = scheduleInfoList.count { it.reminder?.status == ReminderStatus.TAKEN }
        val missedCount = scheduleInfoList.count { it.reminder?.status == ReminderStatus.MISSED }
        val evaluatedTotal = takenCount + missedCount
        val scheduledTotal = scheduleInfoList.size

        val pendingList = scheduleInfoList
            .filter { it.reminder?.status == ReminderStatus.PENDING }
            .sortedBy { it.schedule.time }

        val nextScheduleInfo = pendingList.firstOrNull()

        val hasPrescriptions = prescriptionsWithMeds.isNotEmpty()
        var calculatedNextDoseDate: String? = null

        val emptyState = when {
            !hasPrescriptions -> HomeEmptyState.NO_PRESCRIPTIONS
            scheduleInfoList.isEmpty() -> {
                // Prescriptions exist, but no doses today. Calculate next dose date.
                val futureReminders = allReminders
                    .filter { it.scheduledAt.take(10) > todayPrefix }
                    .sortedBy { it.scheduledAt }

                val earliestReminderDate = futureReminders.firstOrNull()?.let {
                    try {
                        LocalDate.parse(it.scheduledAt.take(10))
                    } catch (_: Exception) { null }
                }

                var nextMedicineDate: LocalDate? = null
                if (earliestReminderDate == null) {
                    for (offset in 1L..90L) {
                        val candidate = today.plusDays(offset)
                        var foundForDate = false
                        for (pwm in prescriptionsWithMeds) {
                            val cAt = pwm.prescription.createdAt
                            val pDate = try {
                                if (cAt.length >= 10) LocalDate.parse(cAt.substring(0, 10)) else today
                            } catch (_: Exception) { today }

                            for (m in pwm.medicines) {
                                val med = m.medicine
                                val dur = med.durationDays
                                val db = java.time.temporal.ChronoUnit.DAYS.between(pDate, candidate)
                                val intv = maxOf(1, med.intervalDays)
                                val isIntv = db >= 0 && (db % intv == 0L)
                                val isAct = if (dur == null || dur <= 0) {
                                    !candidate.isBefore(pDate) && isIntv
                                } else {
                                    val endD = pDate.plusDays(dur.toLong())
                                    !candidate.isBefore(pDate) && !candidate.isAfter(endD) && isIntv
                                }
                                if (isAct && m.doseSchedules.isNotEmpty()) {
                                    foundForDate = true
                                    break
                                }
                            }
                            if (foundForDate) break
                        }
                        if (foundForDate) {
                            nextMedicineDate = candidate
                            break
                        }
                    }
                }

                val resolvedNextDate = earliestReminderDate ?: nextMedicineDate ?: today.plusDays(1)
                calculatedNextDoseDate = formatBengaliDate(resolvedNextDate)
                HomeEmptyState.NO_DOSES_TODAY
            }
            scheduledTotal > 0 && takenCount == scheduledTotal -> HomeEmptyState.ALL_DOSES_COMPLETED
            else -> HomeEmptyState.NONE
        }

        val computedFinalGreeting = buildGreetingString(
            greetingPreference = greetingPref,
            userName = userName,
            customGreeting = customGreeting
        )

        HomeUiState(
            nextReminder = nextScheduleInfo?.reminder,
            nextReminderMedicine = nextScheduleInfo?.medicine,
            nextReminderSchedule = nextScheduleInfo?.schedule,
            todayMedicinesGrouped = sortedGroupedMap,
            todayScheduleInfoList = scheduleInfoList,
            progressTaken = takenCount,
            progressMissed = missedCount,
            progressTotal = evaluatedTotal,
            progressScheduled = scheduledTotal,
            themeMode = themeMode,
            userName = userName,
            userProfilePicPath = userProfilePicPath,
            greetingPreference = greetingPref,
            customGreeting = customGreeting,
            finalGreeting = computedFinalGreeting,
            todayDateFormatted = formattedTodayBengali,
            isLoading = false,
            hasPrescriptions = hasPrescriptions,
            nextDoseDateFormatted = calculatedNextDoseDate,
            emptyState = emptyState,
            timeFormat = timeFormat
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(
                isLoading = false,
                finalGreeting = buildGreetingString("universal", ""),
                todayDateFormatted = formatBengaliDate(LocalDate.now())
            )
        )

    fun markAsTaken(reminderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val nowDateTime = LocalDateTime.now()
            val nowIso = nowDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val currentScheduleInfos = uiState.value.todayScheduleInfoList
            val targetInfo = currentScheduleInfos.find { it.reminder?.id == reminderId }
            val existing = reminderRepository.getReminderById(reminderId)

            val isMissedInDb = existing?.status == ReminderStatus.MISSED ||
                targetInfo?.reminder?.status == ReminderStatus.MISSED
            val isPastBy30Minutes = if (targetInfo != null) {
                try {
                    val schedDateTime = LocalDateTime.parse("${todayPrefix}T${targetInfo.schedule.time}:00")
                    nowDateTime.isAfter(schedDateTime.plusMinutes(30))
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }
            val wasMissed = isMissedInDb || isPastBy30Minutes

            val slot = targetInfo?.schedule?.slot
            val remainingInSlot = if (slot != null) {
                currentScheduleInfos.count { info ->
                    info.schedule.slot == slot &&
                        info.reminder?.id != reminderId &&
                        info.reminder?.status != ReminderStatus.TAKEN
                }
            } else {
                1
            }
            val isSlotNowComplete = slot != null && remainingInSlot == 0

            val wasAlreadyTaken = existing?.status == ReminderStatus.TAKEN ||
                targetInfo?.reminder?.status == ReminderStatus.TAKEN

            if (existing != null) {
                reminderRepository.markAsTaken(reminderId, nowIso)
            } else if (targetInfo != null) {
                val scheduledAt = "${todayPrefix}T${targetInfo.schedule.time}:00"
                reminderRepository.insertReminder(
                    ReminderEntity(
                        id = reminderId,
                        doseScheduleId = targetInfo.schedule.id,
                        scheduledAt = scheduledAt,
                        status = ReminderStatus.TAKEN,
                        takenAt = nowIso
                    )
                )
            }

            if (!wasAlreadyTaken) {
                val medicineId = targetInfo?.medicine?.id
                    ?: existing?.doseScheduleId?.let { prescriptionRepository.getMedicineByScheduleId(it)?.id }
                if (medicineId != null) {
                    val updatedMedicine = prescriptionRepository.decrementStockIfAvailable(medicineId)
                    if (updatedMedicine != null && updatedMedicine.stockCount == 5) {
                        reminderScheduler?.notifyLowStock(updatedMedicine.id, updatedMedicine.name)
                    }
                }
            }

            // Cancel exact alarm when marked as taken
            reminderScheduler?.cancelReminder(reminderId)

            val msgRes = when {
                isSlotNowComplete -> getSlotCompletedMessageResId(slot!!)
                wasMissed -> R.string.msg_home_missed_taken
                else -> R.string.msg_home_dose_taken
            }
            _toastMessage.value = HomeToastMessage(messageResId = msgRes)
        }
    }

    private fun getSlotCompletedMessageResId(slot: DoseSlot): Int {
        return when (slot) {
            DoseSlot.MORNING -> R.string.msg_slot_completed_morning
            DoseSlot.NOON -> R.string.msg_slot_completed_noon
            DoseSlot.AFTERNOON -> R.string.msg_slot_completed_afternoon
            DoseSlot.EVENING -> R.string.msg_slot_completed_evening
            DoseSlot.NIGHT -> R.string.msg_slot_completed_night
            DoseSlot.CUSTOM -> R.string.msg_slot_completed_custom
        }
    }

    fun markAsMissed(reminderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderRepository.markAsMissed(reminderId)
        }
    }

    fun deleteMedicine(medicine: MedicineEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            prescriptionRepository.deleteMedicine(medicine)
            _toastMessage.value = HomeToastMessage(messageResId = R.string.msg_medicine_deleted)
        }
    }

    fun markMedicineAsTaken(medicineId: String) {
        val pending = uiState.value.todayScheduleInfoList.find {
            it.medicine.id == medicineId && it.reminder?.status != ReminderStatus.TAKEN
        } ?: uiState.value.todayScheduleInfoList.find { it.medicine.id == medicineId }

        pending?.reminder?.id?.let { reminderId ->
            markAsTaken(reminderId)
        }
    }
}

class HomeViewModelFactory(
    private val prescriptionRepository: PrescriptionRepository,
    private val reminderRepository: ReminderRepository,
    private val themePreferences: ThemePreferences,
    private val reminderScheduler: ReminderScheduler? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(
            prescriptionRepository = prescriptionRepository,
            reminderRepository = reminderRepository,
            themePreferences = themePreferences,
            reminderScheduler = reminderScheduler
        ) as T
    }
}
