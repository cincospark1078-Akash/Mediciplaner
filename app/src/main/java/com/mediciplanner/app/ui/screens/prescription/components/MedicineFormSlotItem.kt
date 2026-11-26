package com.mediciplanner.app.ui.screens.prescription.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.DoseAmountHelper
import com.mediciplanner.app.core.util.formatTime
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.ui.screens.prescription.DraftSchedule
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotResId
import com.mediciplanner.app.ui.screens.prescription.getFoodRuleLabelResId
import com.mediciplanner.app.ui.screens.prescription.getFoodRuleResId

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MedicineFormSlotItem(
    medicineIndex: Int,
    schedule: DraftSchedule,
    medicineType: MedicineType?,
    availableDoseUnits: List<String>,
    is24Hour: Boolean,
    showError: Boolean,
    onTimeClick: () -> Unit,
    onDoseAmountChanged: (String) -> Unit,
    onDoseUnitChanged: (String) -> Unit,
    onFoodRuleChanged: (FoodRule) -> Unit,
    modifier: Modifier = Modifier
) {
    val slotTag = schedule.slot.name.lowercase()
    val slotTitle = stringResource(getDoseSlotResId(schedule.slot))
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val isAmountEmpty = schedule.doseAmount.isBlank()
    val isAmountInvalid = !isAmountEmpty && DoseAmountHelper.parseDoseAmount(schedule.doseAmount) == null
    val hasAmountError = showError && (isAmountEmpty || isAmountInvalid)
    val isUnitEmpty = showError && schedule.doseUnit.isBlank()
    val fieldShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medicine_form_${medicineIndex}_slot_section_$slotTag"),
        shape = fieldShape,
        color = MediciTheme.colors.primaryContainer.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = slotTitle,
                    tint = MediciTheme.colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = slotTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(fieldShape)
                    .border(
                        width = 1.dp,
                        color = MediciTheme.colors.outline,
                        shape = fieldShape
                    )
                    .background(
                        color = MediciTheme.colors.surface,
                        shape = fieldShape
                    )
                    .clickable(onClick = onTimeClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("medicine_form_${medicineIndex}_slot_time_$slotTag"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.field_time_picker),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = stringResource(R.string.field_time_picker),
                        tint = MediciTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = formatTime(schedule.time, is24Hour),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dose Amount (Decimal / Fraction) + Dose Unit Dropdown (Synced with Type)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = schedule.doseAmount,
                    onValueChange = onDoseAmountChanged,
                    label = {
                        Text(
                            text = stringResource(R.string.field_dose_amount),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.dose_hint_example),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = hasAmountError,
                    shape = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MediciTheme.colors.primary,
                        unfocusedBorderColor = if (hasAmountError) {
                            MediciTheme.colors.warning
                        } else {
                            MediciTheme.colors.outline
                        },
                        errorBorderColor = MediciTheme.colors.warning,
                        errorLabelColor = MediciTheme.colors.warning,
                        focusedTextColor = MediciTheme.colors.textPrimary,
                        unfocusedTextColor = MediciTheme.colors.textPrimary,
                        focusedLabelColor = MediciTheme.colors.primary,
                        unfocusedLabelColor = MediciTheme.colors.textSecondary,
                        focusedContainerColor = MediciTheme.colors.surface,
                        unfocusedContainerColor = MediciTheme.colors.surface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("medicine_form_${medicineIndex}_dose_amount_$slotTag")
                )

                ExposedDropdownMenuBox(
                    expanded = unitDropdownExpanded && availableDoseUnits.isNotEmpty(),
                    onExpandedChange = {
                        if (availableDoseUnits.isNotEmpty()) {
                            unitDropdownExpanded = it
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("medicine_form_${medicineIndex}_dose_unit_box_$slotTag")
                ) {
                    OutlinedTextField(
                        value = schedule.doseUnit,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = {
                            Text(
                                text = stringResource(R.string.field_dose_unit),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.field_dose_unit),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = unitDropdownExpanded && availableDoseUnits.isNotEmpty()
                            )
                        },
                        isError = isUnitEmpty,
                        shape = fieldShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = if (isUnitEmpty) {
                                MediciTheme.colors.warning
                            } else {
                                MediciTheme.colors.outline
                            },
                            errorBorderColor = MediciTheme.colors.warning,
                            errorLabelColor = MediciTheme.colors.warning,
                            focusedTextColor = MediciTheme.colors.textPrimary,
                            unfocusedTextColor = MediciTheme.colors.textPrimary,
                            focusedLabelColor = MediciTheme.colors.primary,
                            unfocusedLabelColor = MediciTheme.colors.textSecondary,
                            focusedContainerColor = MediciTheme.colors.surface,
                            unfocusedContainerColor = MediciTheme.colors.surface
                        ),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("medicine_form_${medicineIndex}_dose_unit_$slotTag")
                    )

                    ExposedDropdownMenu(
                        expanded = unitDropdownExpanded && availableDoseUnits.isNotEmpty(),
                        onDismissRequest = { unitDropdownExpanded = false },
                        containerColor = MediciTheme.colors.card,
                        modifier = Modifier.testTag("medicine_form_${medicineIndex}_dose_units_menu_$slotTag")
                    ) {
                        availableDoseUnits.forEach { unit ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = unit,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (schedule.doseUnit == unit) {
                                            MediciTheme.colors.primary
                                        } else {
                                            MediciTheme.colors.textPrimary
                                        },
                                        fontWeight = if (schedule.doseUnit == unit) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    onDoseUnitChanged(unit)
                                    unitDropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                modifier = Modifier.testTag("medicine_form_${medicineIndex}_dose_unit_item_${slotTag}_$unit")
                            )
                        }
                    }
                }
            }

            if (showError && isAmountEmpty) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.error_dose_amount_required),
                    style = MaterialTheme.typography.labelSmall,
                    color = MediciTheme.colors.warning,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("medicine_form_${medicineIndex}_error_dose_amount_$slotTag")
                )
            } else if (showError && isAmountInvalid) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.error_dose_amount_invalid),
                    style = MaterialTheme.typography.labelSmall,
                    color = MediciTheme.colors.warning,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("medicine_form_${medicineIndex}_error_dose_amount_$slotTag")
                )
            }

            if (isUnitEmpty) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.error_dose_unit_required),
                    style = MaterialTheme.typography.labelSmall,
                    color = MediciTheme.colors.warning,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("medicine_form_${medicineIndex}_error_dose_unit_$slotTag")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Food Rule Chips (আগে / পরে / সাথে / খালি পেটে)
            Text(
                text = stringResource(getFoodRuleLabelResId(medicineType)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MediciTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_form_${medicineIndex}_food_rule_row_$slotTag"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        shape = fieldShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primaryContainer,
                            selectedLabelColor = MediciTheme.colors.primary,
                            containerColor = MediciTheme.colors.surface,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MediciTheme.colors.outline,
                            selectedBorderColor = MediciTheme.colors.primary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp
                        ),
                        modifier = Modifier.testTag("medicine_form_${medicineIndex}_food_rule_${slotTag}_${rule.name.lowercase()}")
                    )
                }
            }
        }
    }
}
