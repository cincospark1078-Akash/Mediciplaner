package com.mediciplanner.app.ui.screens.prescription.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.theme.SlotAfternoonColor
import com.mediciplanner.app.core.theme.SlotCustomColor
import com.mediciplanner.app.core.theme.SlotEveningColor
import com.mediciplanner.app.core.theme.SlotMorningColor
import com.mediciplanner.app.core.theme.SlotNightColor
import com.mediciplanner.app.core.theme.SlotNoonColor
import com.mediciplanner.app.core.util.DoseAmountHelper
import com.mediciplanner.app.core.util.UnitHelper
import com.mediciplanner.app.core.util.formatTime
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.ui.components.AccessibleTimeInputDialog
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine
import com.mediciplanner.app.ui.screens.prescription.DraftSchedule
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotResId
import com.mediciplanner.app.ui.screens.prescription.getFoodRuleLabelResId
import com.mediciplanner.app.ui.screens.prescription.getFoodRuleResId
import com.mediciplanner.app.ui.screens.prescription.getUnitResId
import java.util.Locale

fun getSlotColor(slot: DoseSlot): Color {
    return when (slot) {
        DoseSlot.MORNING -> SlotMorningColor.copy(alpha = 0.5f)
        DoseSlot.NOON -> SlotNoonColor.copy(alpha = 0.5f)
        DoseSlot.AFTERNOON -> SlotAfternoonColor.copy(alpha = 0.5f)
        DoseSlot.EVENING -> SlotEveningColor.copy(alpha = 0.5f)
        DoseSlot.NIGHT -> SlotNightColor.copy(alpha = 0.5f)
        DoseSlot.CUSTOM -> SlotCustomColor.copy(alpha = 0.5f)
    }
}

@Composable
fun Card3ScheduleDose(
    draftMedicine: DraftMedicine,
    onToggleSlot: (DoseSlot) -> Unit,
    onToggleExpanded: (DoseSlot) -> Unit,
    onTimeChanged: (DoseSlot, String) -> Unit,
    onAmountChanged: (DoseSlot, String) -> Unit,
    onUnitChanged: (DoseSlot, String) -> Unit,
    onSlotFoodRuleChanged: (String, FoodRule) -> Unit,
    addedMedicines: List<DraftMedicine> = emptyList(),
    selectedMedicineIndex: Int? = null,
    onSelectMedicine: (Int) -> Unit = {},
    showError: Boolean = false,
    is24Hour: Boolean = false,
    modifier: Modifier = Modifier
) {
    var activeTimePickerSlot by remember { mutableStateOf<DoseSlot?>(null) }
    val slots = DoseSlot.entries
    val availableUnits = UnitHelper.getDoseUnitsForType(draftMedicine.type)

    val isSlotSelectionEmpty = draftMedicine.schedules.isEmpty()
    val hasIncompleteSchedule = draftMedicine.schedules.any {
        DoseAmountHelper.parseDoseAmount(it.doseAmount) == null || it.doseUnit.isBlank()
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .testTag("carousel_card_3"),
        colors = CardDefaults.cardColors(
            containerColor = MediciTheme.colors.card
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = MediciTheme.spacing.medium,
                    top = MediciTheme.spacing.medium,
                    end = MediciTheme.spacing.medium,
                    bottom = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
            ) {
                Text(
                    text = stringResource(R.string.card3_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MediciTheme.tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.card3_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.tokens.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Medicine Switcher across added medicines (pinned at top)
            MedicineSwitcher(
                addedMedicines = addedMedicines,
                selectedMedicineIndex = selectedMedicineIndex,
                currentDraftMedicine = draftMedicine,
                onSelectMedicine = onSelectMedicine
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
            ) {

            // Slot Selection (FilterChips)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.field_dose_slots),
                    style = MaterialTheme.typography.labelLarge,
                    color = MediciTheme.tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("dose_slots_row"),
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    slots.forEach { slot ->
                        val isSelected = draftMedicine.schedules.any { it.slot == slot }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleSlot(slot) },
                            label = {
                                Text(
                                    text = stringResource(getDoseSlotResId(slot)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MediciTheme.tokens.primaryContainer,
                                selectedLabelColor = MediciTheme.tokens.primary,
                                containerColor = MediciTheme.tokens.cardBackground,
                                labelColor = MediciTheme.tokens.textPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MediciTheme.tokens.outline,
                                selectedBorderColor = MediciTheme.tokens.primary,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            ),
                            modifier = Modifier.testTag("slot_chip_${slot.name.lowercase()}")
                        )
                    }
                }

                if (showError && isSlotSelectionEmpty) {
                    Text(
                        text = stringResource(R.string.error_slot_required),
                        style = MaterialTheme.typography.labelMedium,
                        color = MediciTheme.tokens.warning,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = MediciTheme.spacing.small)
                            .testTag("error_slot_required")
                    )
                }
            }

            // Collapsible sections for each selected slot
            draftMedicine.schedules.forEach { schedule ->
                androidx.compose.runtime.key(schedule.id) {
                    ScheduleSectionItem(
                        schedule = schedule,
                        medicineType = draftMedicine.type,
                        availableUnits = availableUnits,
                        is24Hour = is24Hour,
                        onToggleExpanded = { onToggleExpanded(schedule.slot) },
                        onTimeClick = {
                            activeTimePickerSlot = schedule.slot
                        },
                        onAmountChanged = { onAmountChanged(schedule.slot, it) },
                        onUnitChanged = { onUnitChanged(schedule.slot, it) },
                        onFoodRuleChanged = { newRule -> onSlotFoodRuleChanged(schedule.id, newRule) },
                        showError = showError
                    )
                }
            }
            }
        }
    }

    val editingSchedule = activeTimePickerSlot?.let { slot ->
        draftMedicine.schedules.firstOrNull { it.slot == slot }
    }
    if (editingSchedule != null) {
        val parts = editingSchedule.time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        AccessibleTimeInputDialog(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = is24Hour,
            onConfirm = { h, m ->
                val formatted = String.format(Locale.US, "%02d:%02d", h, m)
                onTimeChanged(editingSchedule.slot, formatted)
                activeTimePickerSlot = null
            },
            onDismiss = { activeTimePickerSlot = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSectionItem(
    schedule: DraftSchedule,
    medicineType: MedicineType?,
    availableUnits: List<String>,
    is24Hour: Boolean,
    onToggleExpanded: () -> Unit,
    onTimeClick: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onFoodRuleChanged: (FoodRule) -> Unit,
    showError: Boolean
) {
    val slotTitle = stringResource(getDoseSlotResId(schedule.slot))
    val isAmountEmpty = schedule.doseAmount.isBlank()
    val isUnitEmpty = schedule.doseUnit.isBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_item_${schedule.slot.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = getSlotColor(schedule.slot),
            contentColor = MediciTheme.colors.textPrimary
        ),
        border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.small)
        ) {
            // Collapsible Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(MediciTheme.spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = slotTitle,
                        tint = MediciTheme.colors.primary
                    )
                    Text(
                        text = slotTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "(${formatTime(schedule.time, is24Hour)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (schedule.isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = slotTitle,
                        tint = MediciTheme.tokens.textSecondary
                    )
                }
            }

            // Expandable Content
            AnimatedVisibility(
                visible = schedule.isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MediciTheme.spacing.small, vertical = MediciTheme.spacing.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    // Time Picker Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MediciTheme.tokens.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = MediciTheme.tokens.cardBackground,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onTimeClick() }
                            .testTag("slot_time_row_${schedule.slot.name.lowercase()}")
                            .padding(
                                horizontal = MediciTheme.spacing.medium,
                                vertical = MediciTheme.spacing.small
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.field_time_picker),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MediciTheme.tokens.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = stringResource(R.string.field_time_picker),
                                tint = MediciTheme.tokens.primary
                            )
                            Text(
                                text = formatTime(schedule.time, is24Hour),
                                style = MaterialTheme.typography.titleMedium,
                                color = MediciTheme.tokens.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Reusable DoseInputRow component ensuring uniform design and independent state across all slots
                    DoseInputRow(
                        schedule = schedule,
                        availableUnits = availableUnits,
                        onAmountChanged = onAmountChanged,
                        onUnitChanged = onUnitChanged,
                        showError = showError
                    )

                    // Per-Slot Food Rule FilterChips
                    val slotTag = schedule.slot.name.lowercase()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MediciTheme.spacing.extraSmall),
                        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
                    ) {
                        Text(
                            text = stringResource(getFoodRuleLabelResId(medicineType)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MediciTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .testTag("food_rule_row_$slotTag"),
                            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                        ) {
                            FoodRule.entries.forEach { rule ->
                                val isSelected = schedule.foodRule == rule
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onFoodRuleChanged(rule) },
                                    label = {
                                        Text(
                                            text = stringResource(getFoodRuleResId(rule)),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MediciTheme.tokens.primaryContainer,
                                        selectedLabelColor = MediciTheme.tokens.primary,
                                        containerColor = MediciTheme.tokens.cardBackground,
                                        labelColor = MediciTheme.tokens.textPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = MediciTheme.tokens.outline,
                                        selectedBorderColor = MediciTheme.tokens.primary,
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.5.dp
                                    ),
                                    modifier = Modifier.testTag("food_rule_chip_${slotTag}_${rule.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
