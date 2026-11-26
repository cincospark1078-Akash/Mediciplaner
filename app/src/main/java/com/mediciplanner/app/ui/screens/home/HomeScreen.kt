package com.mediciplanner.app.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediciplanner.app.MediciApplication
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mediciplanner.app.core.util.SlotActivationHelper
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import com.mediciplanner.app.ui.components.AboutAppDialog
import com.mediciplanner.app.ui.components.BouncingButton
import com.mediciplanner.app.ui.components.FrostedCard
import com.mediciplanner.app.ui.screens.home.components.GreetingHeader
import com.mediciplanner.app.ui.screens.prescription.AddPrescriptionListScreen
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselViewModel
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselViewModelFactory
import com.mediciplanner.app.ui.screens.home.components.MedicineCard
import com.mediciplanner.app.ui.screens.home.components.NextReminderCard
import com.mediciplanner.app.ui.screens.home.components.ProgressRing
import com.mediciplanner.app.ui.screens.home.components.SlotHeader

@Composable
fun HomeScreen(
    onNavigateToThemePicker: () -> Unit,
    onNavigateToTrack: () -> Unit = {},
    onNavigateToPrescription: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val app = context.applicationContext as MediciApplication
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            prescriptionRepository = app.prescriptionRepository,
            reminderRepository = app.reminderRepository,
            themePreferences = app.themePreferences,
            reminderScheduler = app.reminderScheduler
        )
    )

    val carouselViewModel: PrescriptionCarouselViewModel = viewModel(
        factory = PrescriptionCarouselViewModelFactory(
            repository = app.prescriptionRepository,
            reminderRepository = app.reminderRepository,
            reminderScheduler = app.reminderScheduler,
            themePreferences = app.themePreferences,
            ocrHelper = app.ocrHelper
        )
    )

    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by homeViewModel.isRefreshing.collectAsStateWithLifecycle()
    val homeToastMessage by homeViewModel.toastMessage.collectAsStateWithLifecycle()

    LaunchedEffect(homeToastMessage) {
        homeToastMessage?.let { msg ->
            Toast.makeText(
                context,
                context.getString(msg.messageResId),
                Toast.LENGTH_SHORT
            ).show()
            homeViewModel.clearToastMessage()
        }
    }

    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            currentTime = LocalDateTime.now()
        }
    }
    var selectedMedicineForDetail by remember { mutableStateOf<MedicineEntity?>(null) }
    var selectedScheduleForDetail by remember { mutableStateOf<DoseScheduleEntity?>(null) }
    var editingMedicineId by remember { mutableStateOf<String?>(null) }
    var editingPrescriptionId by remember { mutableStateOf<String?>(null) }
    var showNewPrescriptionList by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var expandedSlots by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // 1. BACKGROUND GRADIENT (Global): Very light blue/purple to off-white
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MediciTheme.colors.primaryContainer, // Very light blue/purple/teal
            MediciTheme.colors.surface // Off-white
        )
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        containerColor = MediciTheme.colors.surface.copy(alpha = 0f),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundGradient)
                .padding(scaffoldPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { homeViewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Top Bar / Greeting Area
                    GreetingHeader(
                        themeMode = uiState.themeMode,
                        formattedDate = uiState.todayDateFormatted,
                        greetingText = uiState.finalGreeting,
                        userName = uiState.userName,
                        userProfilePicPath = uiState.userProfilePicPath,
                        greetingPreference = uiState.greetingPreference,
                        customGreeting = uiState.customGreeting,
                        onMyProfileClick = onNavigateToProfile,
                        onSettingsClick = onNavigateToSettings,
                        onNotificationsClick = onNavigateToNotificationSettings,
                        onAboutClick = { showAboutDialog = true }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MediciTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
                    ) {

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MediciTheme.spacing.large),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MediciTheme.colors.primary,
                                strokeWidth = 3.dp
                            )
                        }
                    } else {
                        when (uiState.emptyState) {
                            com.mediciplanner.app.ui.screens.home.HomeEmptyState.NO_PRESCRIPTIONS -> {
                                // === State A (No Prescriptions at all) ===
                                FrostedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MediciTheme.spacing.small)
                                        .testTag("empty_state_a_card"),
                                    shape = RoundedCornerShape(20.dp),
                                    backgroundColor = MediciTheme.colors.card,
                                    elevation = 4.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MediciTheme.colors.primaryContainer.copy(alpha = 0.4f),
                                                        MediciTheme.colors.card
                                                    )
                                                )
                                            )
                                            .padding(MediciTheme.spacing.large),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .background(
                                                    color = MediciTheme.colors.primaryContainer.copy(alpha = 0.6f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Medication,
                                                contentDescription = null,
                                                tint = MediciTheme.colors.primary,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }

                                        Text(
                                            text = stringResource(R.string.empty_state_a_title),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MediciTheme.colors.textPrimary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("empty_state_a_title")
                                        )

                                        Text(
                                            text = stringResource(R.string.empty_state_a_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MediciTheme.colors.textSecondary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("empty_state_a_desc")
                                        )

                                        BouncingButton(
                                            onClick = {
                                                carouselViewModel.resetDraft()
                                                showNewPrescriptionList = true
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            scaleDown = 0.96f,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MediciTheme.colors.primary,
                                                contentColor = MediciTheme.colors.onPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("empty_state_a_cta")
                                        ) {
                                            Text(
                                                text = stringResource(R.string.empty_state_a_cta),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            com.mediciplanner.app.ui.screens.home.HomeEmptyState.NO_DOSES_TODAY -> {
                                // === State B (Prescriptions exist, but no doses TODAY) ===
                                FrostedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MediciTheme.spacing.small)
                                        .testTag("empty_state_b_card"),
                                    shape = RoundedCornerShape(20.dp),
                                    backgroundColor = MediciTheme.colors.card,
                                    elevation = 4.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MediciTheme.colors.primaryContainer.copy(alpha = 0.4f),
                                                        MediciTheme.colors.card
                                                    )
                                                )
                                            )
                                            .padding(MediciTheme.spacing.large),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .background(
                                                    color = MediciTheme.colors.primaryContainer.copy(alpha = 0.6f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MediciTheme.colors.primary,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }

                                        Text(
                                            text = stringResource(R.string.empty_state_b_title),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MediciTheme.colors.textPrimary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("empty_state_b_title")
                                        )

                                        val nextDateStr = uiState.nextDoseDateFormatted ?: ""
                                        Text(
                                            text = stringResource(R.string.empty_state_b_desc, nextDateStr),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MediciTheme.colors.textSecondary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("empty_state_b_desc")
                                        )

                                        BouncingButton(
                                            onClick = onNavigateToPrescription,
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MediciTheme.colors.primaryContainer,
                                                contentColor = MediciTheme.colors.primary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("empty_state_b_cta")
                                        ) {
                                            Text(
                                                text = stringResource(R.string.empty_state_b_cta),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MediciTheme.colors.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                // 2. Next Reminder Card (Shows Next Dose or Mint Success when all completed)
                                val isNextReminderActive = SlotActivationHelper.isSlotActive(
                                    scheduledTime = uiState.nextReminderSchedule?.time,
                                    currentTime = currentTime
                                )
                                NextReminderCard(
                                    reminder = uiState.nextReminder,
                                    medicine = uiState.nextReminderMedicine,
                                    schedule = uiState.nextReminderSchedule,
                                    hasMedicinesToday = uiState.todayScheduleInfoList.isNotEmpty(),
                                    onMarkAsTaken = { reminderId ->
                                        homeViewModel.markAsTaken(reminderId)
                                    },
                                    onNavigateToTrack = onNavigateToTrack,
                                    is24Hour = uiState.is24Hour,
                                    isSlotActive = isNextReminderActive
                                )

                                // 3. Progress Ring Section (FrostedCard + Canvas 12.dp stroke)
                                ProgressRing(
                                    progressTaken = uiState.progressTaken,
                                    progressMissed = uiState.progressMissed,
                                    progressScheduled = uiState.progressScheduled,
                                    onNavigateToTrack = onNavigateToTrack
                                )

                                // 4. Today's Medicines (Grouped by Slot)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = MediciTheme.spacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                                ) {
                                    Text(
                                        text = stringResource(R.string.today_medicines_header),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MediciTheme.colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val currentTimeSlotName = when (currentTime.hour) {
                                        in 5..11 -> DoseSlot.MORNING.name
                                        in 12..14 -> DoseSlot.NOON.name
                                        in 15..16 -> DoseSlot.AFTERNOON.name
                                        in 17..19 -> DoseSlot.EVENING.name
                                        else -> DoseSlot.NIGHT.name
                                    }
                                    val defaultExpandedSlot = uiState.nextReminderSchedule?.slot?.name
                                        ?.takeIf { uiState.todayMedicinesGrouped.containsKey(it) }
                                        ?: currentTimeSlotName.takeIf { uiState.todayMedicinesGrouped.containsKey(it) }
                                        ?: uiState.todayMedicinesGrouped.keys.firstOrNull()

                                    uiState.todayMedicinesGrouped.forEach { (slotName, medicines) ->
                                        val count = uiState.groupedMedicines[slotName]?.size ?: medicines.size
                                        val isExpanded = expandedSlots[slotName] ?: (slotName == defaultExpandedSlot)

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("slot_section_${slotName.lowercase()}")
                                        ) {
                                            SlotHeader(
                                                slotName = slotName,
                                                medicineCount = count,
                                                isExpanded = isExpanded,
                                                onClick = {
                                                    expandedSlots = expandedSlots + (slotName to !isExpanded)
                                                }
                                            )

                                            AnimatedVisibility(
                                                visible = isExpanded,
                                                enter = fadeIn(animationSpec = tween(durationMillis = 250)) +
                                                    expandVertically(
                                                        animationSpec = tween(
                                                            durationMillis = 250,
                                                            easing = FastOutSlowInEasing
                                                        )
                                                    ),
                                                exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                                                    shrinkVertically(
                                                        animationSpec = tween(
                                                            durationMillis = 250,
                                                            easing = FastOutSlowInEasing
                                                        )
                                                    )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    medicines.forEachIndexed { slotIndex, medicine ->
                                                        val scheduleInfo = uiState.todayScheduleInfoList.find {
                                                            it.medicine.id == medicine.id && it.schedule.slot.name == slotName
                                                        }
                                                        val isTaken = scheduleInfo?.reminder?.status == ReminderStatus.TAKEN
                                                        val isCardSlotActive = SlotActivationHelper.isSlotActive(
                                                            scheduledTime = scheduleInfo?.schedule?.time,
                                                            currentTime = currentTime
                                                        )

                                                        MedicineCard(
                                                            medicine = medicine,
                                                            schedule = scheduleInfo?.schedule,
                                                            isTaken = isTaken,
                                                            slotName = slotName,
                                                            staggerIndex = slotIndex,
                                                            isSlotActive = isCardSlotActive,
                                                            onMarkAsTaken = {
                                                                val reminderId = scheduleInfo?.reminder?.id
                                                                if (reminderId != null) {
                                                                    homeViewModel.markAsTaken(reminderId)
                                                                } else {
                                                                    homeViewModel.markMedicineAsTaken(medicine.id)
                                                                }
                                                            },
                                                            onClick = {
                                                                selectedMedicineForDetail = medicine
                                                                selectedScheduleForDetail = scheduleInfo?.schedule
                                                            },
                                                            onEditClick = {
                                                                carouselViewModel.loadMedicineForEdit(medicine.id)
                                                                editingMedicineId = medicine.id
                                                                editingPrescriptionId = medicine.prescriptionId
                                                            },
                                                            onDeleteClick = {
                                                                homeViewModel.deleteMedicine(medicine)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom padding to clear floating bottom navigation bar
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
            }

            // Medicine Detail BottomSheet (Swipeable across all today's medicines & slots)
            val currentSelectedMed = selectedMedicineForDetail
            if (currentSelectedMed != null) {
                val detailPages = remember(
                    uiState.todayMedicinesGrouped,
                    uiState.todayScheduleInfoList,
                    currentTime
                ) {
                    uiState.todayMedicinesGrouped.flatMap { (slotName, medicines) ->
                        val parsedSlot = try {
                            DoseSlot.valueOf(slotName)
                        } catch (_: Exception) {
                            DoseSlot.CUSTOM
                        }
                        medicines.map { med ->
                            val slotSchedInfo = uiState.todayScheduleInfoList.find {
                                it.medicine.id == med.id && it.schedule.slot.name == slotName
                            }
                            val medSchedules = uiState.todayScheduleInfoList
                                .filter { it.medicine.id == med.id }
                                .map { it.schedule }
                                .distinctBy { it.id }
                            val isPageTaken = slotSchedInfo?.reminder?.status == ReminderStatus.TAKEN
                            val isPageSlotActive = if (isPageTaken || slotSchedInfo == null) {
                                true
                            } else {
                                SlotActivationHelper.isSlotActive(slotSchedInfo.schedule.time, currentTime)
                            }
                            com.mediciplanner.app.ui.screens.home.components.MedicineDetailPage(
                                medicine = med,
                                schedules = medSchedules,
                                slot = slotSchedInfo?.schedule?.slot ?: parsedSlot,
                                reminderId = slotSchedInfo?.reminder?.id,
                                isTaken = isPageTaken,
                                isSlotActive = isPageSlotActive
                            )
                        }
                    }
                }

                val fallbackPages = if (detailPages.isEmpty()) {
                    val matchingScheduleInfos = uiState.todayScheduleInfoList.filter { it.medicine.id == currentSelectedMed.id }
                    val targetScheduleInfo = selectedScheduleForDetail?.let { targetSched ->
                        matchingScheduleInfos.find { it.schedule.id == targetSched.id }
                    } ?: matchingScheduleInfos.firstOrNull()
                    val isTaken = targetScheduleInfo?.reminder?.status == ReminderStatus.TAKEN
                    val isSlotActive = if (isTaken || targetScheduleInfo == null) {
                        true
                    } else {
                        SlotActivationHelper.isSlotActive(targetScheduleInfo.schedule.time, currentTime)
                    }
                    listOf(
                        com.mediciplanner.app.ui.screens.home.components.MedicineDetailPage(
                            medicine = currentSelectedMed,
                            schedules = matchingScheduleInfos.map { it.schedule }.distinctBy { it.id },
                            slot = selectedScheduleForDetail?.slot ?: targetScheduleInfo?.schedule?.slot,
                            reminderId = targetScheduleInfo?.reminder?.id,
                            isTaken = isTaken,
                            isSlotActive = isSlotActive
                        )
                    )
                } else {
                    detailPages
                }

                val clickedSlot = selectedScheduleForDetail?.slot
                val initialPageIndex = fallbackPages.indexOfFirst { page ->
                    page.medicine.id == currentSelectedMed.id &&
                        (clickedSlot == null || page.slot == clickedSlot)
                }.coerceAtLeast(0)

                com.mediciplanner.app.ui.screens.home.components.MedicineDetailBottomSheet(
                    pages = fallbackPages,
                    initialIndex = initialPageIndex,
                    is24Hour = uiState.is24Hour,
                    dismissOnTake = false,
                    onTakeNowClick = { page ->
                        val targetReminderId = page.reminderId
                        if (targetReminderId != null) {
                            homeViewModel.markAsTaken(targetReminderId)
                        } else {
                            homeViewModel.markMedicineAsTaken(page.medicine.id)
                        }
                    },
                    onDismiss = {
                        selectedMedicineForDetail = null
                        selectedScheduleForDetail = null
                    }
                )
            }

            // Edit Prescription List Screen
            val currentEditingMedId = editingMedicineId
            if (currentEditingMedId != null) {
                AddPrescriptionListScreen(
                    viewModel = carouselViewModel,
                    medicineId = currentEditingMedId,
                    prescriptionId = editingPrescriptionId,
                    onDismiss = {
                        editingMedicineId = null
                        editingPrescriptionId = null
                        carouselViewModel.resetDraft()
                    }
                )
            }

            // New Prescription List Screen (from State A CTA)
            if (showNewPrescriptionList) {
                AddPrescriptionListScreen(
                    viewModel = carouselViewModel,
                    onDismiss = {
                        showNewPrescriptionList = false
                        carouselViewModel.resetDraft()
                    }
                )
            }

            // About App Dialog (from Profile Dropdown Menu)
            if (showAboutDialog) {
                AboutAppDialog(
                    onDismiss = { showAboutDialog = false }
                )
            }
        }
    }
}
