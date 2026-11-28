package com.mediciplanner.app.ui.screens.prescription

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.DoseAmountHelper
import com.mediciplanner.app.core.util.UnitHelper
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.ui.components.AccessibleTimeInputDialog
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.components.Card1BaseInfo
import com.mediciplanner.app.ui.screens.prescription.components.MedicineFormCard
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrescriptionListScreen(
    viewModel: PrescriptionCarouselViewModel,
    onDismiss: () -> Unit,
    medicineId: String? = null,
    prescriptionId: String? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val draftPrescription by viewModel.draftPrescription.collectAsStateWithLifecycle()
    val selectedMedicineIndex by viewModel.selectedMedicineIndex.collectAsStateWithLifecycle()
    val editingPrescriptionId by viewModel.editingPrescriptionId.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val extractedText by viewModel.extractedText.collectAsStateWithLifecycle()
    val parsedMedicines by viewModel.parsedMedicines.collectAsStateWithLifecycle()
    val isScanningOcr by viewModel.isScanningOcr.collectAsStateWithLifecycle()
    val showOcrReview by viewModel.showOcrReview.collectAsStateWithLifecycle()
    val reviewMedicines by viewModel.reviewMedicines.collectAsStateWithLifecycle()
    val ocrReviewErrorRes by viewModel.ocrReviewErrorRes.collectAsStateWithLifecycle()
    val pendingOcrImageUri by viewModel.pendingOcrImageUri.collectAsStateWithLifecycle()

    val timeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()
    val is24Hour = timeFormat == ThemePreferences.TIME_FORMAT_24H

    val isEditMode = editingPrescriptionId != null || prescriptionId != null || medicineId != null

    var showDateError by remember { mutableStateOf(false) }
    var showFormErrors by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<Pair<Int, DoseSlot>?>(null) }
    var customIntervalTargetIndex by remember { mutableStateOf<Int?>(null) }
    val expandedMap = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(medicineId, prescriptionId) {
        if (medicineId != null && viewModel.editingMedicineId.value != medicineId) {
            viewModel.loadMedicineForEdit(medicineId)
        } else if (medicineId == null && prescriptionId != null && viewModel.editingPrescriptionId.value != prescriptionId) {
            viewModel.loadPrescriptionByIdForEditing(prescriptionId)
        }
    }

    LaunchedEffect(selectedMedicineIndex, draftPrescription.medicines.size) {
        val sel = selectedMedicineIndex
        if (sel != null && sel in draftPrescription.medicines.indices && expandedMap[sel] == null) {
            expandedMap[sel] = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.clearErrorMessage()
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    if (showOcrReview) {
        OcrReviewScreen(
            imageUri = pendingOcrImageUri,
            medicines = reviewMedicines,
            onUpdateMedicine = { index, name, dose, foodRule ->
                viewModel.updateReviewMedicine(index, name, dose, foodRule)
            },
            onDeleteMedicine = { index ->
                viewModel.removeReviewMedicine(index)
            },
            onAddNewMedicine = {
                viewModel.addEmptyReviewMedicine()
            },
            onAddAll = {
                val prevCount = viewModel.draftPrescription.value.medicines.size
                viewModel.confirmAddAllReviewedMedicines {
                    val newSize = viewModel.draftPrescription.value.medicines.size
                    for (i in prevCount until newSize) {
                        expandedMap[i] = true
                    }
                    if (newSize > prevCount) {
                        scope.launch {
                            listState.animateScrollToItem(prevCount + 2)
                        }
                    }
                }
            },
            onCancel = {
                viewModel.cancelOcrReview()
            },
            errorResId = ocrReviewErrorRes,
            modifier = modifier
        )
        return
    }

    val activeTimeTarget = timePickerTarget
    if (activeTimeTarget != null) {
        val (targetIdx, targetSlot) = activeTimeTarget
        val targetMedicine = draftPrescription.medicines.getOrNull(targetIdx)
        val targetSchedule = targetMedicine?.schedules?.firstOrNull { it.slot == targetSlot }
        if (targetSchedule != null) {
            val parts = targetSchedule.time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            AccessibleTimeInputDialog(
                initialHour = hour,
                initialMinute = minute,
                is24Hour = is24Hour,
                onConfirm = { h, m ->
                    val formatted = String.format(Locale.US, "%02d:%02d", h, m)
                    viewModel.updateScheduleTimeAt(targetIdx, targetSlot, formatted)
                    timePickerTarget = null
                },
                onDismiss = { timePickerTarget = null }
            )
        } else {
            timePickerTarget = null
        }
    }

    val activeIntervalIdx = customIntervalTargetIndex
    if (activeIntervalIdx != null) {
        val targetMedicine = draftPrescription.medicines.getOrNull(activeIntervalIdx)
        if (targetMedicine != null) {
            val currentInterval = maxOf(1, targetMedicine.intervalDays)
            val standardIntervals = listOf(1, 2, 3, 7)
            val initialInput = if (currentInterval !in standardIntervals) currentInterval.toString() else ""
            var localInput by remember(activeIntervalIdx) { mutableStateOf(initialInput) }
            var inputError by remember(activeIntervalIdx) { mutableStateOf(false) }

            Dialog(
                onDismissRequest = { customIntervalTargetIndex = null },
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
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MediciTheme.colors.primary,
                                unfocusedBorderColor = if (inputError) MediciTheme.colors.warning else MediciTheme.colors.outline,
                                errorBorderColor = MediciTheme.colors.warning,
                                focusedTextColor = MediciTheme.colors.textPrimary,
                                unfocusedTextColor = MediciTheme.colors.textPrimary,
                                focusedContainerColor = MediciTheme.colors.surface,
                                unfocusedContainerColor = MediciTheme.colors.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("medicine_form_${activeIntervalIdx}_custom_interval_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { customIntervalTargetIndex = null },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MediciTheme.colors.outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MediciTheme.colors.textSecondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("medicine_form_${activeIntervalIdx}_custom_interval_cancel")
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
                                        viewModel.updateIntervalDaysAt(activeIntervalIdx, parsed)
                                        customIntervalTargetIndex = null
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
                                    .testTag("medicine_form_${activeIntervalIdx}_custom_interval_confirm")
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
        } else {
            customIntervalTargetIndex = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediciTheme.colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .testTag("add_prescription_list_screen")
    ) {
        // Fixed Top Bar: Close (X) icon, Title "প্রেস্ক্রিপশন তৈরি করুন", optional Edit badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MediciTheme.colors.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("add_prescription_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.add_prescription_close_desc),
                        tint = MediciTheme.colors.textPrimary
                    )
                }

                Text(
                    text = stringResource(R.string.add_prescription_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_prescription_title")
                )

                if (isEditMode) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MediciTheme.colors.warning.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MediciTheme.colors.warning),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("add_prescription_edit_badge")
                    ) {
                        Text(
                            text = stringResource(R.string.carousel_edit_mode_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.warning,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        val activeError = errorMessage ?: saveError
        if (activeError != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MediciTheme.colors.warning.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MediciTheme.colors.warning),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("add_prescription_error_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = activeError,
                        tint = MediciTheme.colors.warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = activeError,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MediciTheme.colors.warning,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Scrollable Content: Card1BaseInfo (Date/Doctor/Location + Photos & Scan OCR) + Expandable Medicine Cards
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val hasOcrOrImages = extractedText.isNotBlank() ||
                parsedMedicines.isNotEmpty() ||
                draftPrescription.prescriptionImagePaths.isNotEmpty() ||
                isScanningOcr
            val baseCardHeight = if (hasOcrOrImages) {
                minOf(620.dp, (maxHeight * 0.92f).coerceAtLeast(420.dp))
            } else {
                minOf(460.dp, (maxHeight * 0.85f).coerceAtLeast(360.dp))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("add_prescription_lazy_column"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Prescription Base Info Section (Card1BaseInfo: Date, Doctor, Location, Photos & Scan/OCR)
                item(key = "prescription_base_info") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(baseCardHeight)
                            .testTag("add_prescription_base_info_card")
                    ) {
                        Card1BaseInfo(
                            draftPrescription = draftPrescription,
                            onDateChanged = {
                                showDateError = false
                                viewModel.updateDate(it)
                            },
                            onDoctorNameChanged = { viewModel.updateDoctorName(it) },
                            onLocationChanged = { viewModel.updateLocation(it) },
                            onImagesAdded = { paths ->
                                viewModel.addPrescriptionImagePaths(paths)
                                if (paths.isNotEmpty()) {
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.msg_image_added_removed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onImageRemoved = { path ->
                                viewModel.removePrescriptionImagePath(path)
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.msg_image_added_removed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onScanPrescriptionImage = { uri ->
                                viewModel.processOcrImage(uri)
                            },
                            onScanPrescriptionImages = { uris ->
                                viewModel.processOcrImages(uris)
                            },
                            extractedText = extractedText,
                            parsedMedicines = parsedMedicines,
                            isScanningOcr = isScanningOcr,
                            onAddMedicineFromText = {
                                viewModel.parseMedicinesFromExtractedText()
                                viewModel.openOcrReview()
                            },
                            onAddParsedMedicine = { parsed ->
                                viewModel.addParsedMedicineToDraft(parsed)
                                val addedIdx = viewModel.draftPrescription.value.medicines.lastIndex
                                if (addedIdx >= 0) {
                                    expandedMap[addedIdx] = true
                                    scope.launch {
                                        listState.animateScrollToItem(addedIdx + 2)
                                    }
                                }
                            },
                            showError = showDateError || showFormErrors
                        )
                    }
                }

            // Section Header for Medicines List
            item(key = "medicines_section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.add_prescription_medicines_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MediciTheme.colors.primaryContainer
                    ) {
                        Text(
                            text = stringResource(
                                R.string.prescription_medicines_count,
                                toBengaliDigits(draftPrescription.medicines.size.toString())
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MediciTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("add_prescription_medicines_count_badge")
                        )
                    }
                }
            }

            if (draftPrescription.medicines.isEmpty()) {
                item(key = "empty_medicines_hint") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_prescription_empty_medicines_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MediciTheme.colors.card
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (showFormErrors) {
                                    MediciTheme.colors.warning
                                } else {
                                    MediciTheme.colors.outlineVariant
                                }
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.add_prescription_empty_medicines_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }

                        if (showFormErrors) {
                            Text(
                                text = stringResource(R.string.error_at_least_one_medicine),
                                style = MaterialTheme.typography.labelMedium,
                                color = MediciTheme.colors.warning,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .testTag("error_at_least_one_medicine")
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = draftPrescription.medicines,
                    key = { idx, med -> "${idx}_${med.lineageId.orEmpty()}" }
                ) { index, medicine ->
                    val isExpanded = expandedMap[index] == true
                    MedicineFormCard(
                        index = index,
                        medicine = medicine,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedMap[index] = !isExpanded
                        },
                        onDelete = {
                            val updatedExpanded = mutableMapOf<Int, Boolean>()
                            expandedMap.forEach { (key, value) ->
                                when {
                                    key < index -> updatedExpanded[key] = value
                                    key > index -> updatedExpanded[key - 1] = value
                                }
                            }
                            expandedMap.clear()
                            expandedMap.putAll(updatedExpanded)
                            viewModel.removeDraftMedicineAt(index)
                        },
                        onNameChanged = { viewModel.updateMedicineNameAt(index, it) },
                        onTypeChanged = { viewModel.updateMedicineTypeAt(index, it) },
                        onStrengthChanged = { viewModel.updateStrengthAt(index, it) },
                        onStrengthUnitChanged = { viewModel.updateStrengthUnitAt(index, it) },
                        onToggleSlot = { viewModel.toggleSlotAt(index, it) },
                        onSlotTimeChanged = { slot, time -> viewModel.updateScheduleTimeAt(index, slot, time) },
                        onSlotDoseAmountChanged = { slot, amt -> viewModel.updateScheduleDoseAmountAt(index, slot, amt) },
                        onSlotDoseUnitChanged = { slot, unit -> viewModel.updateScheduleDoseUnitAt(index, slot, unit) },
                        onSlotFoodRuleChanged = { slot, rule -> viewModel.updateSlotFoodRuleAt(index, slot, rule) },
                        onIntervalChanged = { viewModel.updateIntervalDaysAt(index, it) },
                        onDurationChanged = { viewModel.updateDurationDaysAt(index, it) },
                        onStockChanged = { viewModel.updateStockCountAt(index, it) },
                        onNoteChanged = { viewModel.updateMedicineNoteAt(index, it) },
                        onImagePathChanged = { viewModel.updateMedicineImagePathAt(index, it) },
                        is24Hour = is24Hour,
                        showErrors = showFormErrors,
                        onRequestTimePicker = { slot ->
                            focusManager.clearFocus()
                            timePickerTarget = index to slot
                        },
                        onRequestCustomInterval = {
                            focusManager.clearFocus()
                            customIntervalTargetIndex = index
                        }
                    )
                }
            }
        }
        }

        // Fixed Bottom Action Buttons: "নতুন ওষুধ যোগ করুন" and "সংরক্ষণ করুন"
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MediciTheme.colors.surface,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val newIndex = viewModel.addEmptyDraftMedicine()
                        expandedMap[newIndex] = true
                        scope.launch {
                            listState.animateScrollToItem(newIndex + 2)
                        }
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MediciTheme.colors.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MediciTheme.colors.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("add_prescription_add_medicine_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_prescription_add_medicine),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_prescription_add_medicine),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        showFormErrors = true
                        val isDateBlank = draftPrescription.date.isNullOrBlank()
                        showDateError = isDateBlank

                        val isMedicinesEmpty = draftPrescription.medicines.isEmpty()
                        var firstInvalidMedIndex: Int? = null
                        draftPrescription.medicines.forEachIndexed { idx, med ->
                            val parsedDuration = med.durationDays.toIntOrNull()
                            val hasStrengthUnitError = med.strength.isNotBlank() &&
                                UnitHelper.getStrengthUnitsForType(med.type).isNotEmpty() &&
                                med.strengthUnit.isBlank()
                            val hasSlotError = med.schedules.isEmpty() || med.schedules.any { sched ->
                                DoseAmountHelper.parseDoseAmount(sched.doseAmount) == null ||
                                    sched.doseUnit.isBlank()
                            }
                            val hasDurationError = parsedDuration == null || parsedDuration < 1
                            val hasIntervalError = parsedDuration != null &&
                                maxOf(1, med.intervalDays) > parsedDuration
                            val hasAnyMedError = med.name.isBlank() ||
                                med.type == null ||
                                hasStrengthUnitError ||
                                hasSlotError ||
                                hasDurationError ||
                                hasIntervalError
                            if (hasAnyMedError) {
                                expandedMap[idx] = true
                                if (firstInvalidMedIndex == null) {
                                    firstInvalidMedIndex = idx
                                }
                            }
                        }

                        if (isDateBlank || isMedicinesEmpty || firstInvalidMedIndex != null) {
                            val scrollTarget = when {
                                isDateBlank -> 0
                                isMedicinesEmpty -> 2
                                else -> (firstInvalidMedIndex ?: 0) + 2
                            }
                            scope.launch {
                                listState.animateScrollToItem(scrollTarget)
                            }
                            return@Button
                        }

                        viewModel.reviewAndSave(
                            onSuccess = onDismiss,
                            onNoChanges = {
                                viewModel.clearToastMessage()
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.prescription_no_changes),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onDismiss()
                            },
                            onSuccessWithType = { saveType ->
                                val msgRes = when (saveType) {
                                    PrescriptionSaveType.SAVE_NEW -> R.string.msg_prescription_saved_with_dest
                                    PrescriptionSaveType.EDIT_PRESCRIPTION -> R.string.msg_prescription_updated_with_dest
                                    PrescriptionSaveType.ADD_MEDICINE -> R.string.msg_medicine_added_with_dest
                                    PrescriptionSaveType.EDIT_MEDICINE -> R.string.msg_medicine_updated_with_dest
                                }
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        context,
                                        context.getString(msgRes),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.primary,
                        contentColor = MediciTheme.colors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("add_prescription_save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = MediciTheme.colors.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_saving),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.add_prescription_save),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.add_prescription_save),
                            style = MaterialTheme.typography.labelLarge,
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
