package com.mediciplanner.app.ui.screens.prescription.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.components.BouncingButton
import com.mediciplanner.app.ui.components.BouncingOutlinedButton
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine
import com.mediciplanner.app.ui.screens.prescription.DraftPrescription
import com.mediciplanner.app.ui.screens.prescription.computeTotalMedicineSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Card4DurationNotes(
    draftMedicine: DraftMedicine,
    draftPrescription: DraftPrescription,
    onDurationChanged: (String) -> Unit,
    onIncrementDuration: () -> Unit,
    onDecrementDuration: () -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onNoteChanged: (String) -> Unit,
    onStockCountChanged: (Int) -> Unit = {},
    onIncrementStock: () -> Unit = {},
    onDecrementStock: () -> Unit = {},
    selectedMedicineIndex: Int? = null,
    onSelectMedicine: (Int) -> Unit = {},
    totalMedicineSummary: String? = null,
    showError: Boolean = false,
    durationError: String? = null,
    intervalError: String? = null,
    modifier: Modifier = Modifier
) {
    var showCustomIntervalDialog by remember { mutableStateOf(false) }
    var customIntervalInput by remember { mutableStateOf("") }
    // Distinguishes visual selection between "প্রতিদিন" (0, default) and "প্রতি ১ দিন পর" (1) while both map to intervalDays = 1
    var selectedDailyVariant by remember(selectedMedicineIndex) { mutableStateOf(0) }

    Card(
        modifier = modifier
            .fillMaxSize()
            .testTag("carousel_card_4"),
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
                    text = stringResource(R.string.card4_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.card4_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Medicine Switcher across added medicines (pinned at top)
            MedicineSwitcher(
                addedMedicines = draftPrescription.medicines,
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

            // Already added medicines counter badge if > 0
            if (draftPrescription.medicines.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MediciTheme.colors.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(
                            horizontal = MediciTheme.spacing.medium,
                            vertical = MediciTheme.spacing.small
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MediciTheme.colors.primary
                    )
                    Text(
                        text = stringResource(
                            R.string.medicines_added_count,
                            draftPrescription.medicines.size
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MediciTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // === INTERVAL / FREQUENCY SECTION ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interval_section"),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MediciTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.field_interval_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val currentInterval = draftMedicine.intervalDays
                val standardIntervals = listOf(1, 2, 3, 7)
                val isCustomActive = currentInterval !in standardIntervals

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 0: প্রতিদিন (Every day - intervalDays = 1, default selected)
                    FilterChip(
                        selected = currentInterval == 1 && selectedDailyVariant == 0,
                        onClick = {
                            selectedDailyVariant = 0
                            onIntervalChanged(1)
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.interval_everyday),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primary,
                            selectedLabelColor = MediciTheme.colors.onPrimary,
                            containerColor = MediciTheme.colors.card,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("interval_chip_everyday")
                    )

                    // 1: প্রতি ১ দিন পর (Every 1 day - intervalDays = 1)
                    FilterChip(
                        selected = currentInterval == 1 && selectedDailyVariant == 1,
                        onClick = {
                            selectedDailyVariant = 1
                            onIntervalChanged(1)
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.interval_daily),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primary,
                            selectedLabelColor = MediciTheme.colors.onPrimary,
                            containerColor = MediciTheme.colors.card,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("interval_chip_1")
                    )

                    // 2: প্রতি ২ দিন পর (Every 2 days)
                    FilterChip(
                        selected = currentInterval == 2,
                        onClick = { onIntervalChanged(2) },
                        label = {
                            Text(
                                text = stringResource(R.string.interval_every_2_days),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primary,
                            selectedLabelColor = MediciTheme.colors.onPrimary,
                            containerColor = MediciTheme.colors.card,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("interval_chip_2")
                    )

                    // 3: প্রতি ৩ দিন পর (Every 3 days)
                    FilterChip(
                        selected = currentInterval == 3,
                        onClick = { onIntervalChanged(3) },
                        label = {
                            Text(
                                text = stringResource(R.string.interval_every_3_days),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primary,
                            selectedLabelColor = MediciTheme.colors.onPrimary,
                            containerColor = MediciTheme.colors.card,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("interval_chip_3")
                    )

                    // 7: সাপ্তাহিক (Weekly)
                    FilterChip(
                        selected = currentInterval == 7,
                        onClick = { onIntervalChanged(7) },
                        label = {
                            Text(
                                text = stringResource(R.string.interval_weekly),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primary,
                            selectedLabelColor = MediciTheme.colors.onPrimary,
                            containerColor = MediciTheme.colors.card,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("interval_chip_7")
                    )

                    // Custom Interval Chip
                    val customLabel = if (isCustomActive) {
                        stringResource(R.string.interval_custom_badge, toBengaliDigits(currentInterval.toString()))
                    } else {
                        stringResource(R.string.interval_custom)
                    }

                    FilterChip(
                        selected = isCustomActive,
                        onClick = {
                            customIntervalInput = if (isCustomActive) currentInterval.toString() else ""
                            showCustomIntervalDialog = true
                        },
                        label = {
                            Text(
                                text = customLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primary,
                            selectedLabelColor = MediciTheme.colors.onPrimary,
                            containerColor = MediciTheme.colors.card,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("interval_chip_custom")
                    )
                }

                // Interval > Duration Warning validation
                val parsedDurationForInterval = draftMedicine.durationDays.toIntOrNull()
                val isIntervalExceeded = parsedDurationForInterval != null && currentInterval > parsedDurationForInterval
                if (isIntervalExceeded || intervalError != null) {
                    Text(
                        text = intervalError ?: stringResource(R.string.error_interval_greater_than_duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MediciTheme.colors.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = MediciTheme.spacing.small)
                            .testTag("error_interval_warning")
                    )
                }
            }

            // Duration in Days (with +/- buttons)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.field_duration_days),
                    style = MaterialTheme.typography.labelLarge,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val parsedDuration = draftMedicine.durationDays.toIntOrNull()
                val isDurationInvalid = draftMedicine.durationDays.isBlank() || parsedDuration == null || parsedDuration < 1

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    FilledIconButton(
                        onClick = onDecrementDuration,
                        shape = RoundedCornerShape(20.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MediciTheme.colors.primaryContainer,
                            contentColor = MediciTheme.colors.primary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("duration_decrement_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.duration_decrement)
                        )
                    }

                    OutlinedTextField(
                        value = draftMedicine.durationDays,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            onDurationChanged(digitsOnly)
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.field_duration_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = stringResource(R.string.field_duration_days),
                                tint = if (showError && isDurationInvalid) {
                                    MediciTheme.colors.warning
                                } else {
                                    MediciTheme.colors.primary
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = showError && isDurationInvalid,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("duration_days_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = if (showError && isDurationInvalid) {
                                MediciTheme.colors.warning
                            } else {
                                MediciTheme.colors.outlineVariant
                            },
                            errorBorderColor = MediciTheme.colors.warning,
                            errorLabelColor = MediciTheme.colors.warning,
                            focusedTextColor = MediciTheme.colors.textPrimary,
                            unfocusedTextColor = MediciTheme.colors.textPrimary,
                            focusedLabelColor = MediciTheme.colors.primary,
                            unfocusedLabelColor = MediciTheme.colors.textSecondary
                        )
                    )

                    FilledIconButton(
                        onClick = onIncrementDuration,
                        shape = RoundedCornerShape(20.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MediciTheme.colors.primaryContainer,
                            contentColor = MediciTheme.colors.primary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("duration_increment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.duration_increment)
                        )
                    }
                }

                if (showError && isDurationInvalid) {
                    Text(
                        text = durationError ?: stringResource(R.string.error_duration_required),
                        style = MaterialTheme.typography.labelMedium,
                        color = MediciTheme.colors.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = MediciTheme.spacing.small)
                            .testTag("error_duration_required")
                    )
                }
            }

            // Stock Quantity (with +/- buttons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stock_quantity_section"),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.field_stock_quantity),
                    style = MaterialTheme.typography.labelLarge,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    FilledIconButton(
                        onClick = onDecrementStock,
                        shape = RoundedCornerShape(20.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MediciTheme.colors.primaryContainer,
                            contentColor = MediciTheme.colors.primary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("stock_decrement_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.stock_decrement)
                        )
                    }

                    OutlinedTextField(
                        value = if (draftMedicine.stockCount > 0) draftMedicine.stockCount.toString() else "",
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            val parsed = digitsOnly.toIntOrNull() ?: 0
                            onStockCountChanged(parsed)
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.field_stock_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = stringResource(R.string.field_stock_quantity),
                                tint = MediciTheme.colors.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stock_count_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = MediciTheme.colors.outlineVariant,
                            focusedTextColor = MediciTheme.colors.textPrimary,
                            unfocusedTextColor = MediciTheme.colors.textPrimary,
                            focusedLabelColor = MediciTheme.colors.primary,
                            unfocusedLabelColor = MediciTheme.colors.textSecondary
                        )
                    )

                    FilledIconButton(
                        onClick = onIncrementStock,
                        shape = RoundedCornerShape(20.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MediciTheme.colors.primaryContainer,
                            contentColor = MediciTheme.colors.primary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("stock_increment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.stock_increment)
                        )
                    }
                }
            }

            // Total Medicine Calculation Summary (displayed only when Duration > 0)
            val durationInt = draftMedicine.durationDays.toIntOrNull() ?: 0
            val calculatedSummary = totalMedicineSummary ?: computeTotalMedicineSummary(
                duration = durationInt,
                interval = draftMedicine.intervalDays,
                schedules = draftMedicine.schedules
            )
            if (durationInt > 0 && calculatedSummary.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MediciTheme.colors.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("total_medicine_summary_box")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MediciTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = calculatedSummary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MediciTheme.colors.primary,
                            modifier = Modifier.testTag("total_medicine_summary_text")
                        )
                    }
                }
            }

            // Note (Optional, multiline)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.field_notes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedTextField(
                    value = draftMedicine.note,
                    onValueChange = onNoteChanged,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.field_notes_placeholder),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = stringResource(R.string.field_notes),
                            tint = MediciTheme.colors.primary
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MediciTheme.colors.primary,
                        unfocusedBorderColor = MediciTheme.colors.outlineVariant,
                        focusedTextColor = MediciTheme.colors.textPrimary,
                        unfocusedTextColor = MediciTheme.colors.textPrimary
                    )
                )
            }
            }
        }
    }

    // Custom Interval Input Dialog
    if (showCustomIntervalDialog) {
        var localInput by remember { mutableStateOf(customIntervalInput) }
        var inputError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCustomIntervalDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.interval_custom_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = localInput,
                        onValueChange = {
                            val digits = it.filter { ch -> ch.isDigit() }
                            localInput = digits
                            inputError = false
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.interval_custom_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = inputError,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_interval_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = if (inputError) MediciTheme.colors.warning else MediciTheme.colors.outlineVariant,
                            errorBorderColor = MediciTheme.colors.warning
                        )
                    )
                }
            },
            confirmButton = {
                BouncingButton(
                    onClick = {
                        val parsed = localInput.toIntOrNull()
                        if (parsed != null && parsed >= 1) {
                            onIntervalChanged(parsed)
                            showCustomIntervalDialog = false
                        } else {
                            inputError = true
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.primary,
                        contentColor = MediciTheme.colors.onPrimary
                    ),
                    modifier = Modifier.testTag("custom_interval_confirm_btn")
                ) {
                    Text(
                        text = stringResource(R.string.interval_custom_confirm),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            dismissButton = {
                BouncingOutlinedButton(
                    onClick = { showCustomIntervalDialog = false },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MediciTheme.colors.textSecondary
                    ),
                    modifier = Modifier.testTag("custom_interval_cancel_btn")
                ) {
                    Text(
                        text = stringResource(R.string.interval_custom_cancel),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MediciTheme.colors.surface
        )
    }
}
