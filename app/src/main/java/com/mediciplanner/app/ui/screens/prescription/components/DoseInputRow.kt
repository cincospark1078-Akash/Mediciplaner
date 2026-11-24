package com.mediciplanner.app.ui.screens.prescription.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.screens.prescription.DraftSchedule

/**
 * Reusable Dose Input Row component (Amount + Unit) for Card 3.
 *
 * - Left: OutlinedTextField for Dose Amount (weight 1f, numeric keyboard).
 * - Right: Exposed Dropdown for Dose Unit matching the exact border, height, and shape (weight 1f).
 * - Helper text below: "কতটুকু খাবেন?"
 * - Data state remains completely independent per slot schedule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseInputRow(
    schedule: DraftSchedule,
    availableUnits: List<String>,
    onAmountChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    showError: Boolean = false,
    modifier: Modifier = Modifier
) {
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    val isAmountEmpty = schedule.doseAmount.isBlank()
    val isUnitEmpty = schedule.doseUnit.isBlank()
    val slotTag = schedule.slot.name.lowercase()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: OutlinedTextField for Dose Amount (weight 1f, numeric)
            OutlinedTextField(
                value = schedule.doseAmount,
                onValueChange = onAmountChanged,
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
                isError = showError && isAmountEmpty,
                modifier = Modifier
                    .weight(1f)
                    .testTag("dose_amount_input_$slotTag"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediciTheme.tokens.primary,
                    unfocusedBorderColor = if (showError && isAmountEmpty) {
                        MediciTheme.tokens.warning
                    } else {
                        MediciTheme.tokens.outline
                    },
                    errorBorderColor = MediciTheme.tokens.warning,
                    errorLabelColor = MediciTheme.tokens.warning,
                    focusedTextColor = MediciTheme.tokens.textPrimary,
                    unfocusedTextColor = MediciTheme.tokens.textPrimary,
                    focusedLabelColor = MediciTheme.tokens.primary,
                    unfocusedLabelColor = MediciTheme.tokens.textSecondary
                )
            )

            // Right: Dropdown for Dose Unit (weight 1f, identical height, border, and style)
            ExposedDropdownMenuBox(
                expanded = unitDropdownExpanded && availableUnits.isNotEmpty(),
                onExpandedChange = {
                    if (availableUnits.isNotEmpty()) {
                        unitDropdownExpanded = it
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("dose_unit_box_$slotTag")
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
                            expanded = unitDropdownExpanded && availableUnits.isNotEmpty()
                        )
                    },
                    isError = showError && isUnitEmpty,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MediciTheme.tokens.primary,
                        unfocusedBorderColor = if (showError && isUnitEmpty) {
                            MediciTheme.tokens.warning
                        } else {
                            MediciTheme.tokens.outline
                        },
                        errorBorderColor = MediciTheme.tokens.warning,
                        errorLabelColor = MediciTheme.tokens.warning,
                        focusedTextColor = MediciTheme.tokens.textPrimary,
                        unfocusedTextColor = MediciTheme.tokens.textPrimary,
                        focusedLabelColor = MediciTheme.tokens.primary,
                        unfocusedLabelColor = MediciTheme.tokens.textSecondary
                    ),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("dose_unit_dropdown_$slotTag")
                )

                ExposedDropdownMenu(
                    expanded = unitDropdownExpanded && availableUnits.isNotEmpty(),
                    onDismissRequest = { unitDropdownExpanded = false },
                    modifier = Modifier.testTag("slot_${slotTag}_dose_units")
                ) {
                    availableUnits.forEach { unit ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = unit,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (schedule.doseUnit == unit) {
                                        MediciTheme.tokens.primary
                                    } else {
                                        MediciTheme.tokens.textPrimary
                                    },
                                    fontWeight = if (schedule.doseUnit == unit) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            },
                            onClick = {
                                onUnitChanged(unit)
                                unitDropdownExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            modifier = Modifier.testTag("dose_unit_item_${slotTag}_$unit")
                        )
                    }
                }
            }
        }

        // Helper text below the row
        Text(
            text = stringResource(R.string.field_dose_amount_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MediciTheme.tokens.textSecondary,
            modifier = Modifier.padding(start = MediciTheme.spacing.extraSmall)
        )

        // Error message if amount is empty
        if (showError && isAmountEmpty) {
            Text(
                text = stringResource(R.string.error_dose_amount_required),
                style = MaterialTheme.typography.labelSmall,
                color = MediciTheme.tokens.warning,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = MediciTheme.spacing.extraSmall)
            )
        } else if (showError && isUnitEmpty) {
            Text(
                text = stringResource(R.string.error_dose_unit_required),
                style = MaterialTheme.typography.labelSmall,
                color = MediciTheme.tokens.warning,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = MediciTheme.spacing.extraSmall)
            )
        }
    }
}
