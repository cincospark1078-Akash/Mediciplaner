package com.mediciplanner.app.ui.screens.prescription.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.UnitHelper
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.ui.components.AccessibleTimeInputDialog
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotResId
import com.mediciplanner.app.ui.screens.prescription.getMedicineTypeResId
import java.io.File
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MedicineFormCard(
    index: Int,
    medicine: DraftMedicine,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    onNameChanged: (String) -> Unit = {},
    onTypeChanged: (MedicineType) -> Unit = {},
    onStrengthChanged: (String) -> Unit = {},
    onStrengthUnitChanged: (String) -> Unit = {},
    onToggleSlot: (DoseSlot) -> Unit = {},
    onSlotTimeChanged: (DoseSlot, String) -> Unit = { _, _ -> },
    onSlotDoseAmountChanged: (DoseSlot, String) -> Unit = { _, _ -> },
    onSlotDoseUnitChanged: (DoseSlot, String) -> Unit = { _, _ -> },
    onSlotFoodRuleChanged: (DoseSlot, FoodRule) -> Unit = { _, _ -> },
    onIntervalChanged: (Int) -> Unit = {},
    onDurationChanged: (String) -> Unit = {},
    onStockChanged: (Int) -> Unit = {},
    onNoteChanged: (String) -> Unit = {},
    onImagePathChanged: (String?) -> Unit = {},
    is24Hour: Boolean = false,
    showErrors: Boolean = false,
    onRequestTimePicker: ((DoseSlot) -> Unit)? = null,
    onRequestCustomInterval: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val imagesDir = File(context.filesDir, "medicine_images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val fileName = "${UUID.randomUUID()}.jpg"
                val destFile = File(imagesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                onImagePathChanged(fileName)
            } catch (_: Exception) {}
        }
    }

    val cardTitle = stringResource(
        R.string.card2_medicine_default_name,
        toBengaliDigits((index + 1).toString())
    )
    val fieldShape = RoundedCornerShape(16.dp)
    val availableStrengthUnits = UnitHelper.getStrengthUnitsForType(medicine.type)
    val availableDoseUnits = UnitHelper.getDoseUnitsForType(medicine.type)

    var strengthUnitDropdownExpanded by remember { mutableStateOf(false) }
    var activeTimePickerSlot by remember { mutableStateOf<DoseSlot?>(null) }
    var showCustomIntervalDialog by remember { mutableStateOf(false) }
    var customIntervalInitial by remember { mutableStateOf("") }

    val isNameMissing = showErrors && medicine.name.isBlank()
    val isTypeMissing = showErrors && medicine.type == null
    val isStrengthUnitMissing = showErrors &&
        medicine.strength.isNotBlank() &&
        availableStrengthUnits.isNotEmpty() &&
        medicine.strengthUnit.isBlank()
    val isSlotsMissing = showErrors && medicine.schedules.isEmpty()
    val parsedDuration = medicine.durationDays.toIntOrNull()
    val isDurationInvalid = showErrors && (parsedDuration == null || parsedDuration < 1)
    val isIntervalExceeded = parsedDuration != null && medicine.intervalDays > parsedDuration

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medicine_form_card_$index"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MediciTheme.colors.card
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isExpanded) {
                MediciTheme.colors.primary.copy(alpha = 0.5f)
            } else {
                MediciTheme.colors.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header: "ওষুধ [index]" + Expand/Collapse Icon + Delete (X) Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onToggleExpand)
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                    .testTag("medicine_form_card_header_$index"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MediciTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("medicine_form_card_title_$index")
                    )
                    if (medicine.name.isNotBlank()) {
                        Text(
                            text = medicine.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_form_card_subtitle_$index")
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("medicine_form_card_toggle_$index")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(
                            if (isExpanded) R.string.medicine_card_collapse_desc
                            else R.string.medicine_card_expand_desc
                        ),
                        tint = MediciTheme.colors.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("medicine_form_card_delete_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.card2_delete_medicine_desc),
                        tint = MediciTheme.colors.warning
                    )
                }
            }

            // Expanded Content with all required medicine input fields
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .testTag("medicine_form_card_content_$index")
                ) {
                    HorizontalDivider(color = MediciTheme.colors.outlineVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. ওষুধের নাম (Required, maxLines = 3 for full visibility of long names)
                    FormSectionHeader(
                        text = stringResource(R.string.section_header_medicine_name),
                        modifier = Modifier.testTag("medicine_form_${index}_header_name")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = medicine.name,
                        onValueChange = onNameChanged,
                        label = {
                            Text(
                                text = stringResource(R.string.field_medicine_name),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.field_medicine_placeholder),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = stringResource(R.string.field_medicine_name),
                                tint = if (isNameMissing) MediciTheme.colors.warning else MediciTheme.colors.primary
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        isError = isNameMissing,
                        shape = fieldShape,
                        colors = medicineOutlinedFieldColors(isError = isNameMissing),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp, max = 120.dp)
                            .testTag("medicine_form_${index}_name_input")
                    )

                    if (isNameMissing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.error_name_required),
                            style = MaterialTheme.typography.labelMedium,
                            color = MediciTheme.colors.warning,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_form_${index}_error_name")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. ধরন (MedicineType FilterChips)
                    FormSectionHeader(
                        text = stringResource(R.string.section_header_medicine_type),
                        modifier = Modifier.testTag("medicine_form_${index}_header_type")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_form_${index}_type_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MedicineType.entries.forEach { type ->
                            val isSelected = medicine.type == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTypeChanged(type) },
                                label = {
                                    Text(
                                        text = stringResource(getMedicineTypeResId(type)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                shape = fieldShape,
                                colors = medicineFilterChipColors(),
                                border = medicineFilterChipBorder(isSelected),
                                modifier = Modifier.testTag("medicine_form_${index}_type_chip_${type.name.lowercase()}")
                            )
                        }
                    }

                    if (isTypeMissing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.error_type_required),
                            style = MaterialTheme.typography.labelMedium,
                            color = MediciTheme.colors.warning,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_form_${index}_error_type")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. মাত্রা (Numeric Strength + Unit Dropdown synced with Type)
                    FormSectionHeader(
                        text = stringResource(R.string.section_header_strength),
                        modifier = Modifier.testTag("medicine_form_${index}_header_strength")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = medicine.strength,
                            onValueChange = onStrengthChanged,
                            label = {
                                Text(
                                    text = stringResource(R.string.field_strength),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.field_strength_hint),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = fieldShape,
                            colors = medicineOutlinedFieldColors(isError = false),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("medicine_form_${index}_strength_input")
                        )

                        ExposedDropdownMenuBox(
                            expanded = strengthUnitDropdownExpanded && availableStrengthUnits.isNotEmpty(),
                            onExpandedChange = {
                                if (availableStrengthUnits.isNotEmpty()) {
                                    strengthUnitDropdownExpanded = it
                                }
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("medicine_form_${index}_strength_unit_box")
                        ) {
                            OutlinedTextField(
                                value = medicine.strengthUnit,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = {
                                    Text(
                                        text = stringResource(R.string.field_strength_unit),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.field_strength_unit),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = strengthUnitDropdownExpanded && availableStrengthUnits.isNotEmpty()
                                    )
                                },
                                isError = isStrengthUnitMissing,
                                shape = fieldShape,
                                colors = medicineOutlinedFieldColors(isError = isStrengthUnitMissing),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("medicine_form_${index}_strength_unit_dropdown")
                            )

                            ExposedDropdownMenu(
                                expanded = strengthUnitDropdownExpanded && availableStrengthUnits.isNotEmpty(),
                                onDismissRequest = { strengthUnitDropdownExpanded = false },
                                containerColor = MediciTheme.colors.card,
                                modifier = Modifier.testTag("medicine_form_${index}_strength_unit_menu")
                            ) {
                                availableStrengthUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = unit,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (medicine.strengthUnit == unit) {
                                                    MediciTheme.colors.primary
                                                } else {
                                                    MediciTheme.colors.textPrimary
                                                },
                                                fontWeight = if (medicine.strengthUnit == unit) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        onClick = {
                                            onStrengthUnitChanged(unit)
                                            strengthUnitDropdownExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                        modifier = Modifier.testTag("medicine_form_${index}_strength_unit_item_$unit")
                                    )
                                }
                            }
                        }
                    }

                    if (isStrengthUnitMissing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.error_strength_unit_required),
                            style = MaterialTheme.typography.labelMedium,
                            color = MediciTheme.colors.warning,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_form_${index}_error_strength_unit")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. সময়সূচি ও ডোজ (Slot FilterChips + Time + Dose Amount + Dose Unit + Food Rule Chips)
                    FormSectionHeader(
                        text = stringResource(R.string.section_header_schedule_dose),
                        modifier = Modifier.testTag("medicine_form_${index}_header_schedule_dose")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_form_${index}_slots_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DoseSlot.entries.forEach { slot ->
                            val isSelected = medicine.schedules.any { it.slot == slot }
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
                                shape = fieldShape,
                                colors = medicineFilterChipColors(),
                                border = medicineFilterChipBorder(isSelected),
                                modifier = Modifier.testTag("medicine_form_${index}_slot_chip_${slot.name.lowercase()}")
                            )
                        }
                    }

                    if (isSlotsMissing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.error_slot_required),
                            style = MaterialTheme.typography.labelMedium,
                            color = MediciTheme.colors.warning,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_form_${index}_error_slots")
                        )
                    }

                    if (medicine.schedules.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            medicine.schedules.forEach { schedule ->
                                key(schedule.id) {
                                    MedicineFormSlotItem(
                                        medicineIndex = index,
                                        schedule = schedule,
                                        medicineType = medicine.type,
                                        availableDoseUnits = availableDoseUnits,
                                        is24Hour = is24Hour,
                                        showError = showErrors,
                                        onTimeClick = {
                                            if (onRequestTimePicker != null) {
                                                onRequestTimePicker(schedule.slot)
                                            } else {
                                                activeTimePickerSlot = schedule.slot
                                            }
                                        },
                                        onDoseAmountChanged = { amt -> onSlotDoseAmountChanged(schedule.slot, amt) },
                                        onDoseUnitChanged = { unit -> onSlotDoseUnitChanged(schedule.slot, unit) },
                                        onFoodRuleChanged = { rule -> onSlotFoodRuleChanged(schedule.slot, rule) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Interval FilterChips (প্রতিদিন, প্রতি ২ দিন, প্রতি ৩ দিন, সাপ্তাহিক, কাস্টম)
                    Text(
                        text = stringResource(R.string.field_interval_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val currentInterval = maxOf(1, medicine.intervalDays)
                    val standardIntervals = listOf(1, 2, 3, 7)
                    val isCustomIntervalActive = currentInterval !in standardIntervals
                    val customIntervalLabel = if (isCustomIntervalActive) {
                        stringResource(
                            R.string.interval_custom_badge,
                            toBengaliDigits(currentInterval.toString())
                        )
                    } else {
                        stringResource(R.string.interval_custom)
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_form_${index}_interval_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val intervalChips = listOf(
                            1 to R.string.interval_everyday,
                            2 to R.string.interval_every_2_days,
                            3 to R.string.interval_every_3_days,
                            7 to R.string.interval_weekly
                        )
                        intervalChips.forEach { (days, labelRes) ->
                            val isSelected = currentInterval == days
                            FilterChip(
                                selected = isSelected,
                                onClick = { onIntervalChanged(days) },
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                shape = fieldShape,
                                colors = medicineFilterChipColors(),
                                border = medicineFilterChipBorder(isSelected),
                                modifier = Modifier.testTag("medicine_form_${index}_interval_chip_$days")
                            )
                        }

                        FilterChip(
                            selected = isCustomIntervalActive,
                            onClick = {
                                if (onRequestCustomInterval != null) {
                                    onRequestCustomInterval()
                                } else {
                                    customIntervalInitial = if (isCustomIntervalActive) {
                                        currentInterval.toString()
                                    } else {
                                        ""
                                    }
                                    showCustomIntervalDialog = true
                                }
                            },
                            label = {
                                Text(
                                    text = customIntervalLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            shape = fieldShape,
                            colors = medicineFilterChipColors(),
                            border = medicineFilterChipBorder(isCustomIntervalActive),
                            modifier = Modifier.testTag("medicine_form_${index}_interval_chip_custom")
                        )
                    }

                    if (isIntervalExceeded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.error_interval_greater_than_duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MediciTheme.colors.warning,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_form_${index}_error_interval")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6 & 7. মেয়াদ ও স্টক (Duration & Stock)
                    FormSectionHeader(
                        text = stringResource(R.string.section_header_duration_stock),
                        modifier = Modifier.testTag("medicine_form_${index}_header_duration_stock")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = medicine.durationDays,
                            onValueChange = { input ->
                                onDurationChanged(input.filter { it.isDigit() })
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.field_duration_days),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                                    tint = if (isDurationInvalid) MediciTheme.colors.warning else MediciTheme.colors.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = isDurationInvalid,
                            shape = fieldShape,
                            colors = medicineOutlinedFieldColors(isError = isDurationInvalid),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("medicine_form_${index}_duration_input")
                        )

                        OutlinedTextField(
                            value = if (medicine.stockCount > 0) medicine.stockCount.toString() else "",
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                onStockChanged(digits.toIntOrNull() ?: 0)
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.field_stock_quantity),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                            shape = fieldShape,
                            colors = medicineOutlinedFieldColors(isError = false),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("medicine_form_${index}_stock_input")
                        )
                    }

                    if (isDurationInvalid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.error_duration_required),
                            style = MaterialTheme.typography.labelMedium,
                            color = MediciTheme.colors.warning,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_form_${index}_error_duration")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 8. নোট (Multiline Note)
                    FormSectionHeader(
                        text = stringResource(R.string.section_header_note),
                        modifier = Modifier.testTag("medicine_form_${index}_header_note")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = medicine.note,
                        onValueChange = onNoteChanged,
                        label = {
                            Text(
                                text = stringResource(R.string.field_notes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
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
                        shape = fieldShape,
                        colors = medicineOutlinedFieldColors(isError = false),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_form_${index}_note_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 9. ওষুধের ছবি (ঐচ্ছিক)
                    FormSectionHeader(
                        text = stringResource(R.string.field_medicine_image),
                        modifier = Modifier.testTag("medicine_form_${index}_header_image")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val currentImagePath = medicine.imagePath
                    if (currentImagePath.isNullOrBlank()) {
                        val outlineColor = MediciTheme.colors.outline
                        val stroke = Stroke(
                            width = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .drawBehind {
                                    drawRoundRect(
                                        color = outlineColor,
                                        style = stroke,
                                        cornerRadius = CornerRadius(20.dp.toPx())
                                    )
                                }
                                .clickable { imageLauncher.launch("image/*") }
                                .testTag("medicine_form_${index}_upload_image_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = MediciTheme.colors.primaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = stringResource(R.string.action_upload_image),
                                        tint = MediciTheme.colors.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.action_upload_image),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MediciTheme.colors.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        val imageFile = File(File(context.filesDir, "medicine_images"), currentImagePath)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, MediciTheme.colors.outline, RoundedCornerShape(20.dp))
                                    .background(MediciTheme.colors.surface)
                                    .testTag("medicine_form_${index}_image_preview_box")
                            ) {
                                AsyncImage(
                                    model = imageFile,
                                    contentDescription = stringResource(R.string.medicine_image_preview),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("medicine_form_${index}_image_preview"),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { imageLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, MediciTheme.colors.primary),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MediciTheme.colors.primary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("medicine_form_${index}_replace_image_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.action_replace_photo),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onImagePathChanged(null) },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, MediciTheme.colors.warning),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MediciTheme.colors.warning
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("medicine_form_${index}_remove_image_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.action_remove_photo),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val editingSchedule = activeTimePickerSlot?.let { slot ->
        medicine.schedules.firstOrNull { it.slot == slot }
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
                onSlotTimeChanged(editingSchedule.slot, formatted)
                activeTimePickerSlot = null
            },
            onDismiss = { activeTimePickerSlot = null }
        )
    }

    if (showCustomIntervalDialog) {
        var localInput by remember { mutableStateOf(customIntervalInitial) }
        var inputError by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { showCustomIntervalDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MediciTheme.colors.card
                ),
                border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.interval_custom_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    OutlinedTextField(
                        value = localInput,
                        onValueChange = {
                            localInput = it.filter { ch -> ch.isDigit() }
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
                        shape = fieldShape,
                        colors = medicineOutlinedFieldColors(isError = inputError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_form_${index}_custom_interval_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCustomIntervalDialog = false },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MediciTheme.colors.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MediciTheme.colors.textSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("medicine_form_${index}_custom_interval_cancel")
                        ) {
                            Text(
                                text = stringResource(R.string.interval_custom_cancel),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
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
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("medicine_form_${index}_custom_interval_confirm")
                        ) {
                            Text(
                                text = stringResource(R.string.interval_custom_confirm),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MediciTheme.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun medicineOutlinedFieldColors(isError: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MediciTheme.colors.primary,
    unfocusedBorderColor = if (isError) {
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
)

@Composable
private fun medicineFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MediciTheme.colors.primaryContainer,
    selectedLabelColor = MediciTheme.colors.primary,
    containerColor = MediciTheme.colors.surface,
    labelColor = MediciTheme.colors.textPrimary
)

@Composable
private fun medicineFilterChipBorder(isSelected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = isSelected,
    borderColor = MediciTheme.colors.outline,
    selectedBorderColor = MediciTheme.colors.primary,
    borderWidth = 1.dp,
    selectedBorderWidth = 1.5.dp
)
