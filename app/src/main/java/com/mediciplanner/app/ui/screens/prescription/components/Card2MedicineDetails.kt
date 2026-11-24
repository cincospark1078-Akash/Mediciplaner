package com.mediciplanner.app.ui.screens.prescription.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
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
import coil.compose.AsyncImage
import java.io.File
import java.util.UUID
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.UnitHelper
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine
import com.mediciplanner.app.ui.screens.prescription.getMedicineTypeResId
import com.mediciplanner.app.ui.screens.prescription.getUnitResId

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Surface
import com.mediciplanner.app.ui.screens.home.toBengaliDigits

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Card2MedicineDetails(
    draftMedicine: DraftMedicine,
    onNameChanged: (String) -> Unit,
    onTypeChanged: (MedicineType) -> Unit,
    onStrengthChanged: (String) -> Unit,
    onStrengthUnitChanged: (String) -> Unit,
    onImagePathChanged: (String?) -> Unit = {},
    addedMedicines: List<DraftMedicine> = emptyList(),
    selectedMedicineIndex: Int? = null,
    onSelectMedicine: (Int) -> Unit = {},
    onDeleteMedicine: (Int) -> Unit = {},
    showError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
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
    val medicineTypes = MedicineType.entries
    val availableUnits = UnitHelper.getStrengthUnitsForType(draftMedicine.type)
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var isMedicinesExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .fillMaxSize()
            .testTag("carousel_card_2"),
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
                    text = stringResource(R.string.card2_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MediciTheme.tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.card2_subtitle),
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

            // === 2. ADDED MEDICINES ACCORDION IN CARD 2 ===
            if (addedMedicines.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { isMedicinesExpanded = !isMedicinesExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("added_medicines_accordion_button"),
                        shape = RoundedCornerShape(20.dp),
                        color = MediciTheme.colors.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MediciTheme.colors.primary.copy(alpha = 0.35f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(
                                        R.string.card2_added_medicines_accordion,
                                        toBengaliDigits(addedMedicines.size.toString())
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MediciTheme.colors.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = if (isMedicinesExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = stringResource(
                                    if (isMedicinesExpanded) {
                                        R.string.card2_accordion_collapse_desc
                                    } else {
                                        R.string.card2_accordion_expand_desc
                                    }
                                ),
                                tint = MediciTheme.colors.primary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isMedicinesExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("added_medicines_accordion_list"),
                            shape = RoundedCornerShape(20.dp),
                            color = MediciTheme.colors.surface,
                            border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant),
                            shadowElevation = 3.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                addedMedicines.forEachIndexed { index, med ->
                                    val isSelected = selectedMedicineIndex == index
                                    val displayName = med.name.ifBlank {
                                        stringResource(
                                            R.string.card2_medicine_default_name,
                                            toBengaliDigits((index + 1).toString())
                                        )
                                    }
                                    Surface(
                                        onClick = { onSelectMedicine(index) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("added_medicine_item_$index"),
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) {
                                            MediciTheme.colors.primaryContainer
                                        } else {
                                            MediciTheme.colors.card
                                        },
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) {
                                                MediciTheme.colors.primary
                                            } else {
                                                MediciTheme.colors.outlineVariant
                                            }
                                        ),
                                        shadowElevation = if (isSelected) 2.dp else 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.card2_edit_medicine_desc),
                                                tint = if (isSelected) {
                                                    MediciTheme.colors.primary
                                                } else {
                                                    MediciTheme.colors.textSecondary
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) {
                                                    MediciTheme.colors.primary
                                                } else {
                                                    MediciTheme.colors.textPrimary
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("added_medicine_name_$index")
                                            )
                                            IconButton(
                                                onClick = { onDeleteMedicine(index) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("added_medicine_delete_$index")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.card2_delete_medicine_desc),
                                                    tint = MediciTheme.colors.warning,
                                                    modifier = Modifier.size(18.dp)
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

            // Medicine Name (Required)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
            ) {
                OutlinedTextField(
                    value = draftMedicine.name,
                    onValueChange = onNameChanged,
                    label = {
                        Text(
                            text = stringResource(R.string.field_medicine_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.field_medicine_placeholder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = stringResource(R.string.field_medicine_name),
                            tint = MediciTheme.tokens.primary
                        )
                    },
                    isError = showError && draftMedicine.name.isBlank(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MediciTheme.tokens.primary,
                        unfocusedBorderColor = if (showError && draftMedicine.name.isBlank()) {
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

                if (showError && draftMedicine.name.isBlank()) {
                    Text(
                        text = stringResource(R.string.error_name_required),
                        style = MaterialTheme.typography.labelMedium,
                        color = MediciTheme.tokens.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = MediciTheme.spacing.small)
                            .testTag("error_name_required")
                    )
                }
            }

            // Medicine Type (Required)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.field_medicine_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MediciTheme.tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_type_row"),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(medicineTypes) { type ->
                            val isSelected = draftMedicine.type == type
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
                                modifier = Modifier.testTag("type_chip_${type.name.lowercase()}")
                            )
                        }
                    }

                    // Left Edge Gradient Fade & Navigation Arrow
                    if (listState.canScrollBackward) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .fillMaxHeight()
                                .align(Alignment.CenterStart)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(MediciTheme.colors.surface, MediciTheme.colors.surface.copy(alpha = 0f))
                                    )
                                )
                        )
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val targetIndex = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                    listState.animateScrollToItem(targetIndex)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(48.dp)
                                .background(
                                    color = MediciTheme.colors.surface.copy(alpha = 0.9f),
                                    shape = CircleShape
                                )
                                .testTag("medicine_type_scroll_left")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.scroll_types_left),
                                tint = MediciTheme.colors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Right Edge Gradient Fade & Navigation Arrow
                    if (listState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .fillMaxHeight()
                                .align(Alignment.CenterEnd)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(MediciTheme.colors.surface.copy(alpha = 0f), MediciTheme.colors.surface)
                                    )
                                )
                        )
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val targetIndex = (listState.firstVisibleItemIndex + 1).coerceAtMost(medicineTypes.size - 1)
                                    listState.animateScrollToItem(targetIndex)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(48.dp)
                                .background(
                                    color = MediciTheme.colors.surface.copy(alpha = 0.9f),
                                    shape = CircleShape
                                )
                                .testTag("medicine_type_scroll_right")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.scroll_types_right),
                                tint = MediciTheme.colors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                if (showError && draftMedicine.type == null) {
                    Text(
                        text = stringResource(R.string.error_type_required),
                        style = MaterialTheme.typography.labelMedium,
                        color = MediciTheme.tokens.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = MediciTheme.spacing.small)
                            .testTag("error_type_required")
                    )
                }
            }

            // Strength (Required) & Unit Dropdown (Inline Connected Row)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: OutlinedTextField for Strength (Numeric only, weight(1f))
                    OutlinedTextField(
                        value = draftMedicine.strength,
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
                        isError = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("strength_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.tokens.primary,
                            unfocusedBorderColor = MediciTheme.tokens.outline,
                            errorBorderColor = MediciTheme.tokens.warning,
                            errorLabelColor = MediciTheme.tokens.warning,
                            focusedTextColor = MediciTheme.tokens.textPrimary,
                            unfocusedTextColor = MediciTheme.tokens.textPrimary,
                            focusedLabelColor = MediciTheme.tokens.primary,
                            unfocusedLabelColor = MediciTheme.tokens.textSecondary
                        )
                    )

                    // Right: Dropdown box displaying the selected unit
                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded && availableUnits.isNotEmpty(),
                        onExpandedChange = {
                            if (availableUnits.isNotEmpty()) {
                                unitDropdownExpanded = it
                            }
                        },
                        modifier = Modifier.width(135.dp)
                    ) {
                        OutlinedTextField(
                            value = draftMedicine.strengthUnit,
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
                                    expanded = unitDropdownExpanded && availableUnits.isNotEmpty()
                                )
                            },
                            isError = showError && draftMedicine.strength.isNotBlank() && availableUnits.isNotEmpty() && draftMedicine.strengthUnit.isBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MediciTheme.tokens.primary,
                                unfocusedBorderColor = if (showError && draftMedicine.strength.isNotBlank() && availableUnits.isNotEmpty() && draftMedicine.strengthUnit.isBlank()) {
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
                                .testTag("strength_unit_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = unitDropdownExpanded && availableUnits.isNotEmpty(),
                            onDismissRequest = { unitDropdownExpanded = false },
                            modifier = Modifier.testTag("strength_unit_dropdown_menu")
                        ) {
                            availableUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = unit,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (draftMedicine.strengthUnit == unit) {
                                                MediciTheme.tokens.primary
                                            } else {
                                                MediciTheme.tokens.textPrimary
                                            },
                                            fontWeight = if (draftMedicine.strengthUnit == unit) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    onClick = {
                                        onStrengthUnitChanged(unit)
                                        unitDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    modifier = Modifier.testTag("strength_unit_item_$unit")
                                )
                            }
                        }
                    }
                }

                // Helper text below Strength field
                Text(
                    text = stringResource(R.string.strength_helper_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MediciTheme.tokens.textSecondary,
                    modifier = Modifier.padding(start = MediciTheme.spacing.small)
                )

                if (showError && draftMedicine.strength.isNotBlank() && availableUnits.isNotEmpty() && draftMedicine.strengthUnit.isBlank()) {
                    Text(
                        text = stringResource(R.string.error_strength_unit_required),
                        style = MaterialTheme.typography.labelMedium,
                        color = MediciTheme.tokens.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = MediciTheme.spacing.small)
                            .testTag("error_strength_unit_required")
                    )
                }
            }

            // Image Picker Section (Photo upload for medicine identification)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.field_medicine_image),
                    style = MaterialTheme.typography.labelLarge,
                    color = MediciTheme.tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (draftMedicine.imagePath.isNullOrBlank()) {
                    val outlineColor = MediciTheme.tokens.outline
                    val stroke = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .drawBehind {
                                drawRoundRect(
                                    color = outlineColor,
                                    style = stroke,
                                    cornerRadius = CornerRadius(12.dp.toPx())
                                )
                            }
                            .clickable { launcher.launch("image/*") }
                            .testTag("upload_medicine_image_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MediciTheme.tokens.primaryContainer,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.action_upload_image),
                                    tint = MediciTheme.tokens.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.action_upload_image),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MediciTheme.tokens.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    val imageFile = File(File(context.filesDir, "medicine_images"), draftMedicine.imagePath)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MediciTheme.tokens.outline, RoundedCornerShape(12.dp))
                            .testTag("medicine_image_preview_box")
                    ) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = stringResource(R.string.medicine_image_preview),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                try {
                                    if (imageFile.exists()) {
                                        imageFile.delete()
                                    }
                                } catch (_: Exception) {}
                                onImagePathChanged(null)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(48.dp)
                                .background(MediciTheme.colors.textPrimary.copy(alpha = 0.6f), CircleShape)
                                .testTag("remove_medicine_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_remove_photo),
                                tint = MediciTheme.colors.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
