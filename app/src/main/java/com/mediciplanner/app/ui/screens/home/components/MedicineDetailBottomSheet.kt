package com.mediciplanner.app.ui.screens.home.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.DoseAmountHelper
import com.mediciplanner.app.core.util.SlotActivationHelper
import com.mediciplanner.app.core.util.formatTime
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.ui.components.BouncingButton
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotEmojiResId
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotResId
import com.mediciplanner.app.ui.screens.prescription.getFoodRuleLabelResId
import com.mediciplanner.app.ui.screens.prescription.getFoodRuleResId
import com.mediciplanner.app.ui.screens.prescription.getMedicineTypeResId
import com.mediciplanner.app.ui.screens.prescription.getSlotMedicinesHeaderResId
import com.mediciplanner.app.ui.screens.prescription.getSlotSwitchedMessageResId
import java.io.File

data class MedicineDetailPage(
    val medicine: MedicineEntity,
    val schedules: List<DoseScheduleEntity>,
    val slot: DoseSlot? = schedules.firstOrNull()?.slot,
    val reminderId: String? = null,
    val isTaken: Boolean = false,
    val isSlotActive: Boolean = if (schedules.isEmpty()) true else schedules.any { SlotActivationHelper.isSlotActive(it.time) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineDetailBottomSheet(
    medicine: MedicineEntity,
    schedules: List<DoseScheduleEntity>,
    isTaken: Boolean = false,
    isReadOnly: Boolean = false,
    is24Hour: Boolean = false,
    isSlotActive: Boolean = if (schedules.isEmpty()) true else schedules.any { SlotActivationHelper.isSlotActive(it.time) },
    onTakeNowClick: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    MedicineDetailBottomSheet(
        pages = listOf(
            MedicineDetailPage(
                medicine = medicine,
                schedules = schedules,
                slot = null,
                isTaken = isTaken,
                isSlotActive = isSlotActive
            )
        ),
        initialIndex = 0,
        isReadOnly = isReadOnly,
        is24Hour = is24Hour,
        dismissOnTake = true,
        onTakeNowClick = { _ -> onTakeNowClick() },
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineDetailBottomSheet(
    pages: List<MedicineDetailPage>,
    initialIndex: Int = 0,
    isReadOnly: Boolean = false,
    is24Hour: Boolean = false,
    dismissOnTake: Boolean = false,
    onTakeNowClick: (MedicineDetailPage) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pages.isEmpty()) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clampedInitial = initialIndex.coerceIn(0, pages.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = clampedInitial,
        pageCount = { pages.size }
    )

    var previousSlot by remember { mutableStateOf(pages.getOrNull(clampedInitial)?.slot) }
    LaunchedEffect(pagerState.settledPage) {
        val currentSlot = pages.getOrNull(pagerState.settledPage)?.slot
        if (currentSlot != null && previousSlot != null && currentSlot != previousSlot) {
            Toast.makeText(
                context,
                context.getString(getSlotSwitchedMessageResId(currentSlot)),
                Toast.LENGTH_SHORT
            ).show()
        }
        if (currentSlot != null) {
            previousSlot = currentSlot
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MediciTheme.colors.card,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("medicine_detail_sheet")
    ) {
        val activePage = pages.getOrNull(pagerState.currentPage) ?: pages.first()
        val activeSlot = activePage.slot
        val showTopSlotHeader = activeSlot != null || pages.size > 1

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showTopSlotHeader) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MediciTheme.spacing.medium)
                        .padding(bottom = MediciTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeSlot != null) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                            ) {
                                Text(
                                    text = stringResource(getDoseSlotEmojiResId(activeSlot)),
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(getSlotMedicinesHeaderResId(activeSlot)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("detail_slot_indicator")
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_detail_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close_carousel),
                                tint = MediciTheme.colors.textSecondary
                            )
                        }
                    }

                    // Dot Indicator below slot name showing total count & current position
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("detail_pager_dot_indicator"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pages.size) { idx ->
                            val isSelected = idx == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (isSelected) 18.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MediciTheme.colors.primary
                                        else MediciTheme.colors.outline
                                    )
                            )
                        }
                    }

                    HorizontalDivider(color = MediciTheme.colors.outline.copy(alpha = 0.25f))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .testTag("medicine_detail_pager")
            ) { pageIndex ->
                val page = pages[pageIndex]
                MedicineDetailPageContent(
                    page = page,
                    showInlineCloseButton = !showTopSlotHeader,
                    isReadOnly = isReadOnly,
                    is24Hour = is24Hour,
                    dismissOnTake = dismissOnTake,
                    onTakeNowClick = { onTakeNowClick(page) },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun MedicineDetailPageContent(
    page: MedicineDetailPage,
    showInlineCloseButton: Boolean,
    isReadOnly: Boolean,
    is24Hour: Boolean,
    dismissOnTake: Boolean,
    onTakeNowClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val medicine = page.medicine
    val schedules = page.schedules
    val isTaken = page.isTaken
    val isSlotActive = page.isSlotActive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediciTheme.spacing.medium)
            .padding(bottom = MediciTheme.spacing.large)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
    ) {
        val imageFile = if (!medicine.imagePath.isNullOrBlank()) {
            File(context.filesDir, "medicine_images/${medicine.imagePath}")
        } else null

        // Medicine Photo (Top of sheet)
        if (imageFile != null && imageFile.exists()) {
            AsyncImage(
                model = imageFile,
                contentDescription = stringResource(R.string.medicine_image_preview),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = MediciTheme.colors.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("medicine_detail_image"),
                contentScale = ContentScale.Crop
            )
        }

        // Header with Title and optional Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MediciTheme.colors.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MediciTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val typeText = stringResource(getMedicineTypeResId(medicine.type))
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showInlineCloseButton) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_detail_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_carousel),
                        tint = MediciTheme.colors.textSecondary
                    )
                }
            }
        }

        HorizontalDivider(color = MediciTheme.colors.outline.copy(alpha = 0.3f))

        // Details List
        // 1. Strength
        if (medicine.strength != null && medicine.strengthUnit != null) {
            DetailRow(
                icon = Icons.Default.Medication,
                label = stringResource(R.string.field_strength),
                value = "${toBengaliDigits(medicine.strength.toString().removeSuffix(".0"))} ${medicine.strengthUnit}"
            )
        }

        // 2. Schedule & Doses (Per-slot Emoji + Food Rule)
        if (schedules.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(32.dp)
                        .background(
                            color = MediciTheme.colors.primaryContainer.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MediciTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.card3_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    schedules.forEach { sched ->
                        val slotName = stringResource(getDoseSlotResId(sched.slot))
                        val slotEmoji = stringResource(getDoseSlotEmojiResId(sched.slot))
                        val amountStr = DoseAmountHelper.formatDoseAmount(sched.doseAmount, useBengaliDigits = true)
                        val timeStr = toBengaliDigits(formatTime(sched.time, is24Hour))
                        val unitStr = if (sched.doseUnit.isNotBlank()) sched.doseUnit else stringResource(R.string.unit_ti)
                        val slotFoodRule = sched.foodRuleOverride ?: sched.foodRule
                        val foodRuleStr = stringResource(getFoodRuleResId(slotFoodRule))
                        val rowText = stringResource(
                            R.string.medicine_detail_time_slot,
                            slotName,
                            timeStr,
                            amountStr,
                            unitStr,
                            foodRuleStr
                        ).removePrefix("• ").trim()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                        ) {
                            Text(
                                text = slotEmoji,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("detail_slot_emoji_${sched.slot.name.lowercase()}")
                            )
                            Text(
                                text = rowText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MediciTheme.colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. Consumption / Administration Rule (Dynamic label by MedicineType)
        val distinctRules = schedules.map { it.foodRuleOverride ?: it.foodRule }.distinct()
        val ruleValue = when {
            schedules.isEmpty() -> stringResource(getFoodRuleResId(medicine.foodRule))
            distinctRules.size == 1 -> stringResource(getFoodRuleResId(distinctRules.first()))
            else -> schedules.joinToString("\n") { sched ->
                val slotName = context.getString(getDoseSlotResId(sched.slot))
                val ruleText = context.getString(getFoodRuleResId(sched.foodRuleOverride ?: sched.foodRule))
                "$slotName: $ruleText"
            }
        }
        DetailRow(
            icon = Icons.Default.Info,
            label = stringResource(getFoodRuleLabelResId(medicine.type)),
            value = ruleValue,
            maxLines = if (distinctRules.size <= 1) 2 else schedules.size + 1,
            modifier = Modifier.testTag("medicine_detail_food_rule")
        )

        // 4. Duration
        if (medicine.durationDays != null) {
            DetailRow(
                icon = Icons.Default.DateRange,
                label = stringResource(R.string.field_duration_days),
                value = stringResource(R.string.medicine_detail_duration, toBengaliDigits(medicine.durationDays.toString()))
            )
        }

        // 5. Note
        if (!medicine.note.isNullOrBlank()) {
            DetailRow(
                icon = Icons.Default.Note,
                label = stringResource(R.string.field_notes),
                value = medicine.note
            )
        }

        if (!isReadOnly) {
            Spacer(modifier = Modifier.height(MediciTheme.spacing.small))

            val isButtonEnabled = !isTaken && isSlotActive
            val disabledBgColor = if (isTaken) {
                MediciTheme.colors.success
            } else {
                MediciTheme.colors.textSecondary.copy(alpha = 0.18f)
            }
            val disabledFgColor = if (isTaken) {
                MediciTheme.colors.onPrimary
            } else {
                MediciTheme.colors.textSecondary.copy(alpha = 0.7f)
            }

            // Prominent "গ্রহণ করেছি" (Take Now) or "সম্পন্ন" (Already Completed) button
            BouncingButton(
                onClick = {
                    onTakeNowClick()
                    if (dismissOnTake) {
                        onDismiss()
                    }
                },
                enabled = isButtonEnabled,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediciTheme.colors.primary,
                    contentColor = MediciTheme.colors.onPrimary,
                    disabledContainerColor = disabledBgColor,
                    disabledContentColor = disabledFgColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("take_now_bottom_sheet_button")
            ) {
                if (isTaken) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MediciTheme.colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(MediciTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.action_completed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(MediciTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.action_mark_taken),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isTaken && !isSlotActive) {
                Text(
                    text = stringResource(R.string.slot_not_time_yet),
                    style = MaterialTheme.typography.labelMedium,
                    color = MediciTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_sheet_not_time_yet")
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    maxLines: Int = 4,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(32.dp)
                .background(
                    color = MediciTheme.colors.primaryContainer.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MediciTheme.colors.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MediciTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MediciTheme.colors.textPrimary,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
