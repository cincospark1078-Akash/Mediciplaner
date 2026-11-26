package com.mediciplanner.app.ui.screens.track

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.mediciplanner.app.core.util.formatTime
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.ui.components.EmptyStateIllustrationStyle
import com.mediciplanner.app.ui.components.EmptyStateView
import com.mediciplanner.app.ui.components.SlotFilterRow
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotResId
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

private fun formatBengaliTime(isoDateTimeOrTime: String?, is24Hour: Boolean): String {
    if (isoDateTimeOrTime.isNullOrBlank()) return ""
    return toBengaliDigits(formatTime(isoDateTimeOrTime, is24Hour))
}

private fun getSlotEnum(slotName: String): DoseSlot {
    return try {
        DoseSlot.valueOf(slotName)
    } catch (e: Exception) {
        DoseSlot.CUSTOM
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val app = context.applicationContext as MediciApplication
    val trackViewModel: TrackViewModel = viewModel(
        factory = TrackViewModelFactory(
            prescriptionRepository = app.prescriptionRepository,
            reminderRepository = app.reminderRepository,
            reminderScheduler = app.reminderScheduler,
            themePreferences = app.themePreferences
        )
    )

    val uiState by trackViewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by trackViewModel.selectedDate.collectAsStateWithLifecycle()
    val toastMessage by trackViewModel.toastMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(
                context,
                context.getString(msg.messageResId),
                Toast.LENGTH_SHORT
            ).show()
            trackViewModel.clearToastMessage()
        }
    }

    var showMissedSheet by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MediciTheme.colors.primaryContainer.copy(alpha = 0.45f),
            MediciTheme.colors.surface,
            MediciTheme.colors.surface
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .padding(horizontal = MediciTheme.spacing.medium)
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .testTag("track_screen"),
        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MediciTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
        ) {
            Text(
                text = stringResource(R.string.track_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MediciTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.track_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MediciTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 1. TOP SECTION: HORIZONTAL DATE STRIP (Past 7 days + Today + Next 6 days)
        DateStrip(
            dates = uiState.dateStripDates,
            selectedDate = selectedDate,
            onSelectDate = { trackViewModel.selectDate(it) },
            modifier = Modifier.fillMaxWidth()
        )

        // 2. PROGRESS RING (Same formula as Home: taken, missed, scheduled)
        if (uiState.totalDosesForDate > 0) {
            com.mediciplanner.app.ui.screens.home.components.ProgressRing(
                progressTaken = uiState.completedDosesForDate,
                progressMissed = uiState.missedCount,
                progressScheduled = uiState.totalDosesForDate,
                onNavigateToTrack = { /* already on track */ }
            )
        }

        // 3. MISSED DOSE SUMMARY CARD
        MissedDoseSummaryCard(
            missedCount = uiState.missedCount,
            onClick = {
                if (uiState.missedCount > 0) {
                    showMissedSheet = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 4. TAKEN HISTORY LIST Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.track_history_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MediciTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (uiState.totalDosesForDate > 0) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MediciTheme.colors.primaryContainer.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${toBengaliDigits(uiState.completedDosesForDate.toString())}/${toBengaliDigits(uiState.totalDosesForDate.toString())} ${stringResource(R.string.status_taken_label)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MediciTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Slot Filter Row above the medicine history list
        SlotFilterRow(
            selectedSlot = uiState.selectedSlot,
            onSlotSelected = { trackViewModel.selectSlot(it) },
            modifier = Modifier.fillMaxWidth()
        )

        // History Content
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
        } else if (uiState.trackedDoses.isEmpty()) {
            Box(modifier = Modifier.padding(top = MediciTheme.spacing.small, bottom = MediciTheme.spacing.large)) {
                EmptyStateView(
                    icon = Icons.Outlined.CheckCircle,
                    title = stringResource(R.string.empty_track_title),
                    subtitle = stringResource(R.string.empty_track_desc),
                    illustrationStyle = EmptyStateIllustrationStyle.PLANT_WELLNESS,
                    testTag = "track_empty_state"
                )
            }
        } else if (uiState.filteredGroupedHistory.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MediciTheme.spacing.large),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MediciTheme.colors.card
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.filter_slot_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediciTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("track_filtered_empty_text")
                    )
                }
            }
        } else {
            // Grouped History by Schedule Slot
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MediciTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
            ) {
                uiState.filteredGroupedHistory.forEach { (slot, dosesInSlot) ->
                    SlotGroupSection(
                        slot = slot,
                        doses = dosesInSlot,
                        is24Hour = uiState.is24Hour,
                        onMarkTaken = { trackViewModel.markAsTaken(it) }
                    )
                }
            }
        }

        // Bottom padding to clear floating bottom navigation bar
        Spacer(modifier = Modifier.height(100.dp))
    }

    // 3. MODAL BOTTOM SHEET (Missed Details)
    if (showMissedSheet) {
        MissedDosesBottomSheet(
            missedGrouped = uiState.missedDoseItemsGroupedBySlot,
            is24Hour = uiState.is24Hour,
            onDismiss = { showMissedSheet = false },
            onMarkTaken = { trackViewModel.markAsTaken(it) },
            onMarkAllTaken = {
                trackViewModel.markAllMissedAsTaken()
                showMissedSheet = false
            }
        )
    }
}

/**
 * 1. Horizontal Scrollable Date Strip:
 * - Past 7 days + Today + Next 6 days (from TrackUiState.dateStripDates)
 * - Format: "DayName" (e.g. সোম, মঙ্গল) and "Date" (e.g. ০৫)
 * - Selected: filled circle with MediciTheme.colors.primary and white text
 * - Today (when unselected): primary border circle and indicator dot
 * - Unselected: transparent background with MediciTheme.colors.textSecondary
 */
@Composable
private fun DateStrip(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val listState = rememberLazyListState()

    // Scroll so today is comfortably visible with past dates on the left and future dates on the right
    androidx.compose.runtime.LaunchedEffect(dates) {
        val todayIndex = dates.indexOf(today)
        if (todayIndex >= 0) {
            listState.scrollToItem(index = (todayIndex - 2).coerceAtLeast(0))
        }
    }

    Card(
        modifier = modifier.testTag("date_strip_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MediciTheme.colors.card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MediciTheme.spacing.small)
                .testTag("date_strip_row"),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(
                items = dates,
                key = { it.toString() }
            ) { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val dayOfWeekBengali = when (date.dayOfWeek) {
                    DayOfWeek.SUNDAY -> stringResource(R.string.day_sun)
                    DayOfWeek.MONDAY -> stringResource(R.string.day_mon)
                    DayOfWeek.TUESDAY -> stringResource(R.string.day_tue)
                    DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wed)
                    DayOfWeek.THURSDAY -> stringResource(R.string.day_thu)
                    DayOfWeek.FRIDAY -> stringResource(R.string.day_fri)
                    DayOfWeek.SATURDAY -> stringResource(R.string.day_sat)
                }
                val dayNumber = toBengaliDigits(String.format(Locale.US, "%02d", date.dayOfMonth))

                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectDate(date) }
                        .padding(vertical = 6.dp)
                        .testTag("date_item_${date}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Day of week text
                    Text(
                        text = dayOfWeekBengali,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected || isToday) MediciTheme.colors.primary else MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Date circle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isSelected) MediciTheme.colors.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .then(
                                if (isToday && !isSelected) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = MediciTheme.colors.primary,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.SemiBold,
                            color = when {
                                isSelected -> MediciTheme.colors.onPrimary
                                isToday -> MediciTheme.colors.primary
                                else -> MediciTheme.colors.textSecondary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Small indicator dot for today
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = MediciTheme.colors.primary,
                                    shape = CircleShape
                                )
                        )
                    } else {
                        Box(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}

/**
 * 2. Missed Dose Summary Card:
 * - State A (Has Missed Doses):
 *   - Background: Soft Amber (#F59E0B with 0.15f alpha)
 *   - Border: Soft Amber (#F59E0B)
 *   - Icon: Warning/Alert icon tinted Amber
 *   - Text: "আজ [সংখ্যা]টি ডোজ মিস হয়েছে"
 *   - Tapping opens ModalBottomSheet
 * - State B (All Good):
 *   - Background: Mint Green (#22C55E with 0.15f alpha)
 *   - Border: Mint Green (#22C55E)
 *   - Icon: CheckCircle tinted Mint Green
 *   - Text: "আজ কোনো ডোজ মিস হয়নি, সময়মতো সব খাওয়া হয়েছে!"
 */
@Composable
private fun MissedDoseSummaryCard(
    missedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amberColor = MediciTheme.colors.warning
    val mintGreenColor = MediciTheme.colors.success
    val hasMissed = missedCount > 0

    val containerColor = if (hasMissed) amberColor.copy(alpha = 0.08f) else mintGreenColor.copy(alpha = 0.15f)
    val accentColor = if (hasMissed) amberColor else mintGreenColor
    val borderColor = if (hasMissed) amberColor.copy(alpha = 0.3f) else mintGreenColor

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = hasMissed) { onClick() }
            .testTag("missed_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (hasMissed) amberColor.copy(alpha = 0.14f) else mintGreenColor.copy(alpha = 0.22f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasMissed) Icons.Default.WarningAmber else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (hasMissed) {
                        stringResource(R.string.track_missed_doses_summary, toBengaliDigits(missedCount.toString()))
                    } else {
                        stringResource(R.string.track_all_good_message)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (hasMissed) MediciTheme.colors.textPrimary else mintGreenColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (hasMissed) {
                    Text(
                        text = stringResource(R.string.track_missed_tap_action),
                        style = MaterialTheme.typography.bodySmall,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Slot Group Section for History List:
 * - Displays a Slot Header (e.g. "সকাল", using getDoseSlotResId(slot))
 * - Immediately below the header, displays the MedicineCard items belonging to that slot
 */
@Composable
private fun SlotGroupSection(
    slot: DoseSlot,
    doses: List<TrackDoseItem>,
    is24Hour: Boolean = false,
    onMarkTaken: (TrackDoseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val slotTitle = stringResource(getDoseSlotResId(slot))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("slot_section_${slot.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
    ) {
        // Slot Header with Title and count badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("slot_header_${slot.name.lowercase()}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MediciTheme.colors.primary, CircleShape)
            )
            Text(
                text = slotTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MediciTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "(${toBengaliDigits(doses.size.toString())})",
                style = MaterialTheme.typography.bodySmall,
                color = MediciTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // List of Medicine cards belonging to this slot
        doses.forEachIndexed { index, doseItem ->
            com.mediciplanner.app.ui.components.StaggeredEntryItem(index = index) {
                MedicineCard(
                    doseItem = doseItem,
                    is24Hour = is24Hour,
                    onMarkTaken = { onMarkTaken(doseItem) }
                )
            }
        }
    }
}

@Composable
private fun MedicineCard(
    doseItem: TrackDoseItem,
    is24Hour: Boolean = false,
    onMarkTaken: () -> Unit,
    modifier: Modifier = Modifier
) {
    HistoryDoseCard(
        doseItem = doseItem,
        is24Hour = is24Hour,
        onMarkTaken = onMarkTaken,
        modifier = modifier
    )
}

/**
 * 4. Taken History Item Card:
 * - Taken State: Mint Green Checkmark and the time it was taken
 * - Pending State: Grey clock icon and "অপেক্ষমাণ"
 * - Missed State: Amber alert icon and "মিস হয়েছে"
 */
@Composable
private fun HistoryDoseCard(
    doseItem: TrackDoseItem,
    is24Hour: Boolean = false,
    onMarkTaken: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTaken = doseItem.status == ReminderStatus.TAKEN
    val isMissed = doseItem.status == ReminderStatus.MISSED
    val isPending = doseItem.status == ReminderStatus.PENDING

    val mintGreen = MediciTheme.colors.success
    val softAmber = MediciTheme.colors.warning
    val greyColor = MediciTheme.colors.textSecondary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("history_dose_${doseItem.schedule.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMissed) softAmber.copy(alpha = 0.08f) else MediciTheme.colors.card
        ),
        border = if (isMissed) BorderStroke(1.dp, softAmber.copy(alpha = 0.3f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMissed) 0.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // Left Status Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = when {
                            isTaken -> mintGreen.copy(alpha = 0.15f)
                            isMissed -> softAmber.copy(alpha = 0.14f)
                            else -> MediciTheme.colors.surface
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isTaken -> Icons.Default.Check
                        isMissed -> Icons.Default.WarningAmber
                        else -> Icons.Default.Schedule
                    },
                    contentDescription = null,
                    tint = when {
                        isTaken -> mintGreen
                        isMissed -> softAmber
                        else -> greyColor
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            // Middle Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = doseItem.medicine.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val timeStr = toBengaliDigits(formatTime(doseItem.schedule.time, is24Hour))
                    val amountStr = com.mediciplanner.app.core.util.DoseAmountHelper.formatDoseAmount(
                        doseItem.schedule.doseAmount,
                        useBengaliDigits = true
                    )
                    val doseUnit = if (doseItem.schedule.doseUnit.isNotBlank()) doseItem.schedule.doseUnit else stringResource(R.string.unit_ti)

                    Text(
                        text = "$timeStr · $amountStr $doseUnit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Status Badge or Action Button
            when {
                isTaken -> {
                    val formattedTime = formatBengaliTime(doseItem.takenAt, is24Hour)
                    Box(
                        modifier = Modifier
                            .background(
                                color = mintGreen.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (formattedTime.isNotBlank()) {
                                stringResource(R.string.status_taken_time, formattedTime)
                            } else {
                                stringResource(R.string.status_taken_label)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = mintGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                isMissed -> {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onMarkTaken()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = softAmber
                        ),
                        border = BorderStroke(1.dp, softAmber),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("mark_taken_missed_${doseItem.schedule.id}")
                    ) {
                        Text(
                            text = stringResource(R.string.action_take_now),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                else -> {
                    // Pending state
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onMarkTaken()
                        },
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MediciTheme.colors.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.action_mark_taken),
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Modal Bottom Sheet (Missed Details):
 * - Grouped by slot
 * - Subheader per slot
 * - "গ্রহণ করেছি" (Take Now) button per medicine row
 * - "সবগুলো গ্রহণ সম্পন্ন" (Take All) button at bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissedDosesBottomSheet(
    missedGrouped: Map<String, List<TrackDoseItem>>,
    is24Hour: Boolean = false,
    onDismiss: () -> Unit,
    onMarkTaken: (TrackDoseItem) -> Unit,
    onMarkAllTaken: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MediciTheme.colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = MediciTheme.colors.textSecondary.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediciTheme.spacing.large)
                .padding(bottom = MediciTheme.spacing.large)
                .testTag("missed_doses_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.missed_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.missed_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (missedGrouped.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MediciTheme.spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.track_no_missed_doses),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
                ) {
                    missedGrouped.forEach { (slotName, missedDoses) ->
                        val slotEnum = getSlotEnum(slotName)
                        val slotTitle = stringResource(getDoseSlotResId(slotEnum))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sub-header for slot
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MediciTheme.colors.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = slotTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "· ${stringResource(R.string.prescription_medicines_count, toBengaliDigits(missedDoses.size.toString()))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MediciTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Medicine Rows
                            missedDoses.forEach { item ->
                                MissedItemRow(
                                    item = item,
                                    is24Hour = is24Hour,
                                    onTakeNow = { onMarkTaken(item) }
                                )
                            }
                        }
                    }

                    // Bottom "সবগুলো গ্রহণ সম্পন্ন" (Take All) button
                    com.mediciplanner.app.ui.components.BouncingButton(
                        onClick = onMarkAllTaken,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MediciTheme.colors.success,
                            contentColor = MediciTheme.colors.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        hapticType = androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("take_all_missed_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.action_take_all_missed),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_take_all_missed),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Medicine row inside Modal Bottom Sheet with "গ্রহণ করেছি" button
 */
@Composable
private fun MissedItemRow(
    item: TrackDoseItem,
    is24Hour: Boolean = false,
    onTakeNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amberColor = MediciTheme.colors.warning

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("missed_sheet_item_${item.schedule.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = amberColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, amberColor.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = amberColor.copy(alpha = 0.14f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = amberColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.medicine.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val timeStr = toBengaliDigits(formatTime(item.schedule.time, is24Hour))
                val amountStr = com.mediciplanner.app.core.util.DoseAmountHelper.formatDoseAmount(
                    item.schedule.doseAmount,
                    useBengaliDigits = true
                )
                val doseUnit = if (item.schedule.doseUnit.isNotBlank()) item.schedule.doseUnit else stringResource(R.string.unit_ti)
                Text(
                    text = "$timeStr · $amountStr $doseUnit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // "গ্রহণ করেছি" (Take Now) Outlined Button
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onTakeNow()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = amberColor
                ),
                border = BorderStroke(1.dp, amberColor),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("take_now_btn_${item.schedule.id}")
            ) {
                Text(
                    text = stringResource(R.string.action_take_now),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
