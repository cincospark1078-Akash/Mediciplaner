package com.mediciplanner.app.ui.screens.followup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.data.repository.FollowUpRepository
import com.mediciplanner.app.notification.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class FollowUpTab {
    UPCOMING,
    HISTORY
}

enum class FollowUpDraftField {
    TITLE,
    TYPE,
    DATE,
    TIME,
    NOTE,
    DOCUMENT_PATHS,
    LOCATION,
    REMINDER_ENABLED
}

data class DraftFollowUp(
    val title: String = "",
    val type: FollowUpType = FollowUpType.DOCTOR_VISIT,
    val date: String = "",
    val time: String = "09:00",
    val note: String = "",
    val documentPaths: List<String> = emptyList(),
    val location: String = "",
    val reminderEnabled: Boolean = true
)

data class FollowUpUiState(
    val followUps: List<FollowUpEntity> = emptyList(),
    val upcomingFollowUps: List<FollowUpEntity> = emptyList(),
    val historyFollowUps: List<FollowUpEntity> = emptyList(),
    val selectedTab: FollowUpTab = FollowUpTab.UPCOMING,
    val isLoading: Boolean = false,
    val draftFollowUp: DraftFollowUp = DraftFollowUp(),
    val hasDraft: Boolean = false,
    val timeFormat: String = ThemePreferences.TIME_FORMAT_12H
) {
    val is24Hour: Boolean get() = timeFormat == ThemePreferences.TIME_FORMAT_24H
}

data class DraftDoctorVisit(
    val doctorName: String = "",
    val hospitalName: String = "",
    val date: String = "",
    val reminderTime: String = "09:00",
    val note: String = "",
    val reminderEnabled: Boolean = true,
    val documentPaths: List<String> = emptyList()
)

data class DraftTest(
    val testName: String = "",
    val testCenter: String = "",
    val date: String = "",
    val reminderTime: String = "09:00",
    val note: String = "",
    val reminderEnabled: Boolean = true,
    val documentPaths: List<String> = emptyList()
)

class FollowUpViewModel(
    private val followUpRepository: FollowUpRepository,
    private val reminderScheduler: ReminderScheduler? = null,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val themePreferences: ThemePreferences? = null
) : ViewModel() {

    companion object {
        const val KEY_DRAFT_TITLE = "draft_title"
        const val KEY_DRAFT_TYPE = "draft_type"
        const val KEY_DRAFT_DATE = "draft_date"
        const val KEY_DRAFT_TIME = "draft_time"
        const val KEY_DRAFT_NOTE = "draft_note"
        const val KEY_DRAFT_DOCUMENT_PATHS = "draft_document_paths"
        const val KEY_DRAFT_LOCATION = "draft_location"
        const val KEY_DRAFT_REMINDER_ENABLED = "draft_reminder_enabled"
        const val KEY_EDITING_ID = "draft_editing_id"
    }

    private val _isLoading = MutableStateFlow(false)
    private val _selectedTab = MutableStateFlow(FollowUpTab.UPCOMING)
    val selectedTab: StateFlow<FollowUpTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: FollowUpTab) {
        _selectedTab.value = tab
    }

    // === 1. SAVED-STATE DRAFT MANAGEMENT ===
    private val _draftFollowUp = MutableStateFlow(DraftFollowUp())
    val draftFollowUp: StateFlow<DraftFollowUp> = _draftFollowUp.asStateFlow()

    private val _selectedType = MutableStateFlow(FollowUpType.DOCTOR_VISIT)
    val selectedType: StateFlow<FollowUpType> = _selectedType.asStateFlow()

    private val _draftDoctorVisit = MutableStateFlow(DraftDoctorVisit())
    val draftDoctorVisit: StateFlow<DraftDoctorVisit> = _draftDoctorVisit.asStateFlow()

    private val _draftTest = MutableStateFlow(DraftTest())
    val draftTest: StateFlow<DraftTest> = _draftTest.asStateFlow()

    private val _editingFollowUp = MutableStateFlow<FollowUpEntity?>(null)
    val editingFollowUp: StateFlow<FollowUpEntity?> = _editingFollowUp.asStateFlow()

    init {
        restoreDraftFromSavedStateHandle()
        val editingId = savedStateHandle.get<String>(KEY_EDITING_ID)
        if (!editingId.isNullOrBlank()) {
            viewModelScope.launch {
                followUpRepository.allFollowUps.collect { list ->
                    if (_editingFollowUp.value == null && savedStateHandle.get<String>(KEY_EDITING_ID) == editingId) {
                        _editingFollowUp.value = list.firstOrNull { it.id == editingId }
                    }
                }
            }
        }
    }

    private fun restoreDraftFromSavedStateHandle() {
        val savedTitle = savedStateHandle.get<String>(KEY_DRAFT_TITLE) ?: ""
        val savedType = savedStateHandle.get<String>(KEY_DRAFT_TYPE)
            ?.let { runCatching { FollowUpType.valueOf(it) }.getOrNull() }
            ?: FollowUpType.DOCTOR_VISIT
        val savedDate = savedStateHandle.get<String>(KEY_DRAFT_DATE) ?: ""
        val savedTime = (savedStateHandle.get<String>(KEY_DRAFT_TIME) ?: "09:00").ifBlank { "09:00" }
        val savedNote = savedStateHandle.get<String>(KEY_DRAFT_NOTE) ?: ""
        val savedDocs = savedStateHandle.get<ArrayList<String>>(KEY_DRAFT_DOCUMENT_PATHS)?.toList()
            ?: savedStateHandle.get<List<String>>(KEY_DRAFT_DOCUMENT_PATHS)
            ?: emptyList()
        val savedLocation = savedStateHandle.get<String>(KEY_DRAFT_LOCATION) ?: ""
        val savedReminder = savedStateHandle.get<Boolean>(KEY_DRAFT_REMINDER_ENABLED) ?: true

        val restored = DraftFollowUp(
            title = savedTitle,
            type = savedType,
            date = savedDate,
            time = savedTime,
            note = savedNote,
            documentPaths = savedDocs,
            location = savedLocation,
            reminderEnabled = savedReminder
        )
        _draftFollowUp.value = restored
        _selectedType.value = restored.type
        syncLegacyDraftFlows(restored)
    }

    private fun syncLegacyDraftFlows(draft: DraftFollowUp) {
        if (draft.type == FollowUpType.DOCTOR_VISIT) {
            _draftDoctorVisit.value = DraftDoctorVisit(
                doctorName = draft.title,
                hospitalName = draft.location,
                date = draft.date,
                reminderTime = draft.time,
                note = draft.note,
                reminderEnabled = draft.reminderEnabled,
                documentPaths = draft.documentPaths
            )
            _draftTest.value = DraftTest()
        } else {
            _draftTest.value = DraftTest(
                testName = draft.title,
                testCenter = draft.location,
                date = draft.date,
                reminderTime = draft.time,
                note = draft.note,
                reminderEnabled = draft.reminderEnabled,
                documentPaths = draft.documentPaths
            )
            _draftDoctorVisit.value = DraftDoctorVisit()
        }
    }

    private fun computeHasDraft(draft: DraftFollowUp): Boolean {
        return draft.title.isNotBlank() ||
            draft.date.isNotBlank() ||
            draft.note.isNotBlank() ||
            draft.location.isNotBlank() ||
            draft.documentPaths.isNotEmpty() ||
            draft.time != "09:00" ||
            draft.type != FollowUpType.DOCTOR_VISIT ||
            !draft.reminderEnabled
    }

    val uiState: StateFlow<FollowUpUiState> = combine(
        followUpRepository.allFollowUps,
        _selectedTab,
        _isLoading,
        _draftFollowUp,
        themePreferences?.timeFormatFlow ?: flowOf(ThemePreferences.TIME_FORMAT_12H)
    ) { list, tab, loading, draft, timeFormat ->
        FollowUpUiState(
            followUps = list,
            upcomingFollowUps = list.filter { !it.isCompleted },
            historyFollowUps = list.filter { it.isCompleted },
            selectedTab = tab,
            isLoading = loading,
            draftFollowUp = draft,
            hasDraft = computeHasDraft(draft),
            timeFormat = timeFormat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FollowUpUiState(
            isLoading = true,
            draftFollowUp = _draftFollowUp.value,
            hasDraft = computeHasDraft(_draftFollowUp.value)
        )
    )

    // Legacy getter if any screen component accesses allFollowUps directly
    val allFollowUps: StateFlow<List<FollowUpEntity>> = followUpRepository.allFollowUps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateDraft(field: FollowUpDraftField, value: Any?) {
        when (field) {
            FollowUpDraftField.TITLE -> {
                val title = value as? String ?: ""
                savedStateHandle[KEY_DRAFT_TITLE] = title
                _draftFollowUp.update { it.copy(title = title) }
            }
            FollowUpDraftField.TYPE -> {
                val newType = when (value) {
                    is FollowUpType -> value
                    is String -> runCatching { FollowUpType.valueOf(value) }
                        .getOrDefault(FollowUpType.DOCTOR_VISIT)
                    else -> FollowUpType.DOCTOR_VISIT
                }
                if (_draftFollowUp.value.type != newType) {
                    savedStateHandle[KEY_DRAFT_TITLE] = ""
                    savedStateHandle[KEY_DRAFT_LOCATION] = ""
                    savedStateHandle[KEY_DRAFT_DATE] = ""
                    savedStateHandle[KEY_DRAFT_TIME] = "09:00"
                    savedStateHandle[KEY_DRAFT_NOTE] = ""
                    savedStateHandle[KEY_DRAFT_REMINDER_ENABLED] = true
                    savedStateHandle[KEY_DRAFT_DOCUMENT_PATHS] = arrayListOf<String>()
                    savedStateHandle[KEY_DRAFT_TYPE] = newType.name
                    val cleared = DraftFollowUp(type = newType)
                    _draftFollowUp.value = cleared
                    _selectedType.value = newType
                } else {
                    savedStateHandle[KEY_DRAFT_TYPE] = newType.name
                    _draftFollowUp.update { it.copy(type = newType) }
                    _selectedType.value = newType
                }
            }
            FollowUpDraftField.DATE -> {
                val date = value as? String ?: ""
                savedStateHandle[KEY_DRAFT_DATE] = date
                _draftFollowUp.update { it.copy(date = date) }
            }
            FollowUpDraftField.TIME -> {
                val time = (value as? String ?: "09:00").ifBlank { "09:00" }
                savedStateHandle[KEY_DRAFT_TIME] = time
                _draftFollowUp.update { it.copy(time = time) }
            }
            FollowUpDraftField.NOTE -> {
                val note = value as? String ?: ""
                savedStateHandle[KEY_DRAFT_NOTE] = note
                _draftFollowUp.update { it.copy(note = note) }
            }
            FollowUpDraftField.DOCUMENT_PATHS -> {
                val paths = (value as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                savedStateHandle[KEY_DRAFT_DOCUMENT_PATHS] = ArrayList(paths)
                _draftFollowUp.update { it.copy(documentPaths = paths) }
            }
            FollowUpDraftField.LOCATION -> {
                val location = value as? String ?: ""
                savedStateHandle[KEY_DRAFT_LOCATION] = location
                _draftFollowUp.update { it.copy(location = location) }
            }
            FollowUpDraftField.REMINDER_ENABLED -> {
                val enabled = value as? Boolean ?: true
                savedStateHandle[KEY_DRAFT_REMINDER_ENABLED] = enabled
                _draftFollowUp.update { it.copy(reminderEnabled = enabled) }
            }
        }
        syncLegacyDraftFlows(_draftFollowUp.value)
    }

    /**
     * Updates selected type.
     * When switching type, clears the other form's data so there is NO data overlap.
     */
    fun onTypeChanged(newType: FollowUpType) {
        updateDraft(FollowUpDraftField.TYPE, newType)
    }

    // Doctor Visit updates
    fun updateDoctorName(name: String) {
        updateDraft(FollowUpDraftField.TITLE, name)
    }

    fun updateHospitalName(hospital: String) {
        updateDraft(FollowUpDraftField.LOCATION, hospital)
    }

    fun updateDoctorVisitDate(date: String) {
        updateDraft(FollowUpDraftField.DATE, date)
    }

    fun updateDoctorVisitTime(time: String) {
        updateDraft(FollowUpDraftField.TIME, time)
    }

    fun updateDoctorVisitNote(note: String) {
        updateDraft(FollowUpDraftField.NOTE, note)
    }

    fun updateDoctorVisitReminder(enabled: Boolean) {
        updateDraft(FollowUpDraftField.REMINDER_ENABLED, enabled)
    }

    fun updateDoctorVisitDocuments(paths: List<String>) {
        updateDraft(FollowUpDraftField.DOCUMENT_PATHS, paths)
    }

    fun addDoctorVisitDocuments(paths: List<String>) {
        updateDraft(FollowUpDraftField.DOCUMENT_PATHS, _draftFollowUp.value.documentPaths + paths)
    }

    fun removeDoctorVisitDocument(path: String) {
        updateDraft(FollowUpDraftField.DOCUMENT_PATHS, _draftFollowUp.value.documentPaths - path)
    }

    // Test updates
    fun updateTestName(name: String) {
        updateDraft(FollowUpDraftField.TITLE, name)
    }

    fun updateTestCenter(center: String) {
        updateDraft(FollowUpDraftField.LOCATION, center)
    }

    fun updateTestDate(date: String) {
        updateDraft(FollowUpDraftField.DATE, date)
    }

    fun updateTestTime(time: String) {
        updateDraft(FollowUpDraftField.TIME, time)
    }

    fun updateTestNote(note: String) {
        updateDraft(FollowUpDraftField.NOTE, note)
    }

    fun updateTestReminder(enabled: Boolean) {
        updateDraft(FollowUpDraftField.REMINDER_ENABLED, enabled)
    }

    fun updateTestDocuments(paths: List<String>) {
        updateDraft(FollowUpDraftField.DOCUMENT_PATHS, paths)
    }

    fun addTestDocuments(paths: List<String>) {
        updateDraft(FollowUpDraftField.DOCUMENT_PATHS, _draftFollowUp.value.documentPaths + paths)
    }

    fun removeTestDocument(path: String) {
        updateDraft(FollowUpDraftField.DOCUMENT_PATHS, _draftFollowUp.value.documentPaths - path)
    }

    fun clearDraft() {
        savedStateHandle.remove<String>(KEY_DRAFT_TITLE)
        savedStateHandle.remove<String>(KEY_DRAFT_TYPE)
        savedStateHandle.remove<String>(KEY_DRAFT_DATE)
        savedStateHandle.remove<String>(KEY_DRAFT_TIME)
        savedStateHandle.remove<String>(KEY_DRAFT_NOTE)
        savedStateHandle.remove<ArrayList<String>>(KEY_DRAFT_DOCUMENT_PATHS)
        savedStateHandle.remove<List<String>>("draft_documentPaths")
        savedStateHandle.remove<String>(KEY_DRAFT_LOCATION)
        savedStateHandle.remove<Boolean>(KEY_DRAFT_REMINDER_ENABLED)
        savedStateHandle.remove<String>(KEY_EDITING_ID)

        _draftFollowUp.value = DraftFollowUp()
        _selectedType.value = FollowUpType.DOCTOR_VISIT
        _draftDoctorVisit.value = DraftDoctorVisit()
        _draftTest.value = DraftTest()
        _editingFollowUp.value = null
    }

    fun resetDrafts() {
        clearDraft()
    }

    fun loadFollowUpForEdit(followUp: FollowUpEntity) {
        _editingFollowUp.value = followUp
        val reminderTime = followUp.reminderTime.ifBlank { "09:00" }
        val location = followUp.location ?: ""
        val note = followUp.note ?: ""

        savedStateHandle[KEY_EDITING_ID] = followUp.id
        savedStateHandle[KEY_DRAFT_TITLE] = followUp.title
        savedStateHandle[KEY_DRAFT_TYPE] = followUp.type.name
        savedStateHandle[KEY_DRAFT_DATE] = followUp.date
        savedStateHandle[KEY_DRAFT_TIME] = reminderTime
        savedStateHandle[KEY_DRAFT_NOTE] = note
        savedStateHandle[KEY_DRAFT_DOCUMENT_PATHS] = ArrayList(followUp.documentPaths)
        savedStateHandle[KEY_DRAFT_LOCATION] = location
        savedStateHandle[KEY_DRAFT_REMINDER_ENABLED] = followUp.reminderEnabled

        val loaded = DraftFollowUp(
            title = followUp.title,
            type = followUp.type,
            date = followUp.date,
            time = reminderTime,
            note = note,
            documentPaths = followUp.documentPaths,
            location = location,
            reminderEnabled = followUp.reminderEnabled
        )
        _draftFollowUp.value = loaded
        _selectedType.value = followUp.type
        syncLegacyDraftFlows(loaded)
    }

    fun saveFollowUp(
        onSaved: () -> Unit = {},
        onSavedWithEditFlag: ((isEdit: Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val editingId = _editingFollowUp.value?.id ?: savedStateHandle.get<String>(KEY_EDITING_ID)
            val editing = _editingFollowUp.value
                ?: editingId?.let { id -> allFollowUps.value.firstOrNull { it.id == id } }
            val draft = _draftFollowUp.value
            if (draft.title.isBlank() || draft.date.isBlank()) return@launch

            val isEdit = editing != null || editingId != null

            val entity = FollowUpEntity(
                id = editing?.id ?: editingId ?: UUID.randomUUID().toString(),
                title = draft.title.trim(),
                type = draft.type,
                date = draft.date.trim(),
                note = draft.note.trim().ifBlank { null },
                reminderEnabled = draft.reminderEnabled,
                location = draft.location.trim().ifBlank { null },
                isCompleted = editing?.isCompleted ?: false,
                reminderTime = draft.time.trim().ifBlank { "09:00" },
                completedAt = editing?.completedAt,
                documentPaths = draft.documentPaths
            )

            if (isEdit) {
                followUpRepository.updateFollowUp(entity)
            } else {
                followUpRepository.insertFollowUp(entity)
            }

            if (entity.reminderEnabled && !entity.isCompleted) {
                scheduleReminder(entity)
            } else {
                reminderScheduler?.cancelFollowUpReminder(entity.id)
            }

            clearDraft()
            onSavedWithEditFlag?.invoke(isEdit)
            onSaved()
        }
    }

    fun saveCurrentDraft(onSaved: () -> Unit = {}) {
        saveFollowUp(onSaved)
    }

    fun loadFollowUps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            kotlinx.coroutines.delay(100)
            _isLoading.value = false
        }
    }

    fun addFollowUp(
        title: String,
        type: FollowUpType,
        date: String,
        note: String? = null,
        reminderEnabled: Boolean = true,
        location: String? = null,
        reminderTime: String = "09:00",
        isCompleted: Boolean = false,
        completedAt: String? = null
    ) {
        viewModelScope.launch {
            val entity = FollowUpEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                type = type,
                date = date,
                note = note,
                reminderEnabled = reminderEnabled,
                location = location,
                isCompleted = isCompleted,
                reminderTime = reminderTime,
                completedAt = completedAt
            )
            followUpRepository.insertFollowUp(entity)
            if (reminderEnabled && !isCompleted) {
                scheduleReminder(entity)
            }
        }
    }

    fun markAsCompleted(
        followUp: FollowUpEntity,
        onCompleted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val todayIso = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val updated = followUp.copy(
                isCompleted = true,
                completedAt = todayIso
            )
            followUpRepository.updateFollowUp(updated)
            reminderScheduler?.cancelFollowUpReminder(followUp.id)
            onCompleted()
        }
    }

    fun markAsUpcoming(followUp: FollowUpEntity) {
        viewModelScope.launch {
            val updated = followUp.copy(
                isCompleted = false,
                completedAt = null
            )
            followUpRepository.updateFollowUp(updated)
            if (updated.reminderEnabled) {
                scheduleReminder(updated)
            }
        }
    }

    fun updateFollowUp(followUp: FollowUpEntity) {
        viewModelScope.launch {
            followUpRepository.updateFollowUp(followUp)
            if (followUp.reminderEnabled && !followUp.isCompleted) {
                scheduleReminder(followUp)
            } else {
                reminderScheduler?.cancelFollowUpReminder(followUp.id)
            }
        }
    }

    fun deleteFollowUp(
        followUp: FollowUpEntity,
        onDeleted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            followUpRepository.deleteFollowUp(followUp)
            reminderScheduler?.cancelFollowUpReminder(followUp.id)
            onDeleted()
        }
    }

    fun scheduleReminder(followUp: FollowUpEntity) {
        try {
            reminderScheduler?.scheduleFollowUpReminder(followUp)
        } catch (e: Exception) {
            android.util.Log.e("FollowUpViewModel", "Failed to schedule reminder", e)
        }
    }
}

class FollowUpViewModelFactory(
    private val followUpRepository: FollowUpRepository,
    private val reminderScheduler: ReminderScheduler? = null,
    private val themePreferences: ThemePreferences? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        return FollowUpViewModel(followUpRepository, reminderScheduler, savedStateHandle, themePreferences) as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FollowUpViewModel(followUpRepository, reminderScheduler, SavedStateHandle(), themePreferences) as T
    }
}
