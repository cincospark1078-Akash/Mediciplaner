package com.mediciplanner.app.ui.screens.prescription

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.OcrParser
import com.mediciplanner.app.core.util.ParsedMedicine
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.ui.components.FullScreenImageDialog
import com.mediciplanner.app.ui.screens.home.toBengaliDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    imageUri: Uri?,
    medicines: List<ParsedMedicine>,
    onUpdateMedicine: (index: Int, name: String, dose: String?, foodRule: FoodRule) -> Unit,
    onDeleteMedicine: (index: Int) -> Unit,
    onAddNewMedicine: () -> Unit,
    onAddAll: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancel)

    val focusManager = LocalFocusManager.current
    var showFullScreenImage by remember { mutableStateOf(false) }

    val visibleMedicines = remember(medicines) {
        medicines.mapIndexedNotNull { originalIndex, item ->
            val isManualEntry = item.rawText.isBlank()
            if (isManualEntry || OcrParser.isValidParsedMedicine(item)) {
                originalIndex to item
            } else {
                null
            }
        }
    }

    if (showFullScreenImage && imageUri != null) {
        FullScreenImageDialog(
            imageModel = imageUri,
            onDismiss = { showFullScreenImage = false },
            title = stringResource(R.string.ocr_review_original_image)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediciTheme.colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .testTag("ocr_review_screen")
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("ocr_review_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.ocr_review_cancel),
                    tint = MediciTheme.colors.textPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.ocr_review_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.ocr_review_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MediciTheme.colors.primaryContainer
            ) {
                Text(
                    text = stringResource(
                        R.string.ocr_review_medicine_count,
                        toBengaliDigits(visibleMedicines.size.toString())
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MediciTheme.colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("ocr_review_count_badge")
                )
            }
        }

        // Scrollable Content: Original Image Thumbnail + Editable Medicine Cards + Add New Medicine Button
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("ocr_review_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (imageUri != null) {
                item(key = "ocr_original_image_thumbnail") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFullScreenImage = true }
                            .testTag("ocr_review_image_thumbnail"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MediciTheme.colors.card
                        ),
                        border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, MediciTheme.colors.outline, RoundedCornerShape(16.dp))
                            ) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = stringResource(R.string.ocr_review_original_image),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.ocr_review_original_image),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MediciTheme.colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.prescription_images_header),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MediciTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MediciTheme.colors.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = stringResource(R.string.ocr_review_original_image),
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (visibleMedicines.isEmpty()) {
                item(key = "ocr_review_empty_state") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ocr_review_empty_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MediciTheme.colors.card
                        ),
                        border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MediciTheme.colors.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = stringResource(R.string.ocr_review_empty_title),
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.ocr_review_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MediciTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.ocr_review_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(visibleMedicines) { displayIndex, (originalIndex, item) ->
                    OcrReviewMedicineCard(
                        index = displayIndex,
                        medicine = item,
                        onNameChanged = { newName ->
                            onUpdateMedicine(originalIndex, newName, item.dose, item.foodRule)
                        },
                        onDoseChanged = { newDose ->
                            onUpdateMedicine(originalIndex, item.name, newDose, item.foodRule)
                        },
                        onFoodRuleChanged = { newRule ->
                            onUpdateMedicine(originalIndex, item.name, item.dose, newRule)
                        },
                        onDelete = {
                            onDeleteMedicine(originalIndex)
                        }
                    )
                }
            }

            item(key = "ocr_review_add_new_medicine_btn") {
                OutlinedButton(
                    onClick = onAddNewMedicine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(top = 4.dp, bottom = 8.dp)
                        .testTag("ocr_review_add_new_button"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MediciTheme.colors.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MediciTheme.colors.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.ocr_review_add_new_medicine),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ocr_review_add_new_medicine),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bottom Actions: Cancel & Add All
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MediciTheme.colors.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("ocr_review_cancel_button"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MediciTheme.colors.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MediciTheme.colors.textSecondary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.ocr_review_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onAddAll,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("ocr_review_add_all_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.primary,
                        contentColor = MediciTheme.colors.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.ocr_review_add_all),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ocr_review_add_all),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrReviewMedicineCard(
    index: Int,
    medicine: ParsedMedicine,
    onNameChanged: (String) -> Unit,
    onDoseChanged: (String) -> Unit,
    onFoodRuleChanged: (FoodRule) -> Unit,
    onDelete: () -> Unit
) {
    var foodDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ocr_review_item_$index"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MediciTheme.colors.card
        ),
        border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header with Delete (X) IconButton
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MediciTheme.colors.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MediciTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.card2_medicine_default_name,
                            toBengaliDigits((index + 1).toString())
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!medicine.strength.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MediciTheme.colors.primaryContainer
                        ) {
                            Text(
                                text = medicine.strength,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MediciTheme.colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("ocr_review_strength_badge_$index")
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ocr_review_delete_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.ocr_review_delete_medicine),
                        tint = MediciTheme.colors.warning
                    )
                }
            }

            // Medicine Name Field
            OutlinedTextField(
                value = medicine.name,
                onValueChange = onNameChanged,
                label = {
                    Text(
                        text = stringResource(R.string.ocr_review_name_label),
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
                        contentDescription = stringResource(R.string.ocr_review_name_label),
                        tint = MediciTheme.colors.primary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediciTheme.colors.primary,
                    unfocusedBorderColor = MediciTheme.colors.outline,
                    focusedTextColor = MediciTheme.colors.textPrimary,
                    unfocusedTextColor = MediciTheme.colors.textPrimary,
                    focusedLabelColor = MediciTheme.colors.primary,
                    unfocusedLabelColor = MediciTheme.colors.textSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ocr_review_name_input_$index")
            )

            // Dose Field
            OutlinedTextField(
                value = medicine.dose.orEmpty(),
                onValueChange = onDoseChanged,
                label = {
                    Text(
                        text = stringResource(R.string.ocr_review_dose_label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.ocr_review_dose_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = stringResource(R.string.ocr_review_dose_label),
                        tint = MediciTheme.colors.primary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediciTheme.colors.primary,
                    unfocusedBorderColor = MediciTheme.colors.outline,
                    focusedTextColor = MediciTheme.colors.textPrimary,
                    unfocusedTextColor = MediciTheme.colors.textPrimary,
                    focusedLabelColor = MediciTheme.colors.primary,
                    unfocusedLabelColor = MediciTheme.colors.textSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ocr_review_dose_input_$index")
            )

            // Food Rule Dropdown
            ExposedDropdownMenuBox(
                expanded = foodDropdownExpanded,
                onExpandedChange = { foodDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = stringResource(getFoodRuleResId(medicine.foodRule)),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(R.string.field_food_rule),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = stringResource(R.string.field_food_rule),
                            tint = MediciTheme.colors.primary
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = foodDropdownExpanded)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MediciTheme.colors.primary,
                        unfocusedBorderColor = MediciTheme.colors.outline,
                        focusedTextColor = MediciTheme.colors.textPrimary,
                        unfocusedTextColor = MediciTheme.colors.textPrimary,
                        focusedLabelColor = MediciTheme.colors.primary,
                        unfocusedLabelColor = MediciTheme.colors.textSecondary
                    ),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("ocr_review_food_rule_dropdown_$index")
                )

                ExposedDropdownMenu(
                    expanded = foodDropdownExpanded,
                    onDismissRequest = { foodDropdownExpanded = false },
                    modifier = Modifier.testTag("ocr_review_food_rule_menu_$index")
                ) {
                    FoodRule.entries.forEach { rule ->
                        val isSelected = medicine.foodRule == rule
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(getFoodRuleResId(rule)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) {
                                        MediciTheme.colors.primary
                                    } else {
                                        MediciTheme.colors.textPrimary
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onFoodRuleChanged(rule)
                                foodDropdownExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            modifier = Modifier.testTag("ocr_review_food_rule_option_${index}_${rule.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
}
