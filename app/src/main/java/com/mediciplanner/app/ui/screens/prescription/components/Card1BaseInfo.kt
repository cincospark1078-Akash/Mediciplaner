package com.mediciplanner.app.ui.screens.prescription.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.ParsedMedicine
import com.mediciplanner.app.ui.screens.home.formatBengaliDate
import com.mediciplanner.app.ui.screens.prescription.DraftPrescription
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun Card1BaseInfo(
    draftPrescription: DraftPrescription,
    onDateChanged: (String) -> Unit,
    onDoctorNameChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onImagesAdded: (List<String>) -> Unit = {},
    onImageRemoved: (String) -> Unit = {},
    onScanPrescriptionImage: (Uri) -> Unit = {},
    onScanPrescriptionImages: (List<Uri>) -> Unit = {},
    extractedText: String = "",
    parsedMedicines: List<ParsedMedicine> = emptyList(),
    isScanningOcr: Boolean = false,
    onAddMedicineFromText: () -> Unit = {},
    onAddParsedMedicine: (ParsedMedicine) -> Unit = {},
    showError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddPrescriptionSheet by remember { mutableStateOf(false) }

    val scanPrescriptionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onScanPrescriptionImage(it) }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            try {
                val imagesDir = File(context.filesDir, "prescription_images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val newFileNames = mutableListOf<String>()
                val savedUris = mutableListOf<Uri>()
                for (uri in uris) {
                    val fileName = "${UUID.randomUUID()}.jpg"
                    val destFile = File(imagesDir, fileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    newFileNames.add(fileName)
                    savedUris.add(
                        if (destFile.exists() && destFile.length() > 0L) {
                            Uri.fromFile(destFile)
                        } else {
                            uri
                        }
                    )
                }
                onImagesAdded(newFileNames)
                if (savedUris.isNotEmpty()) {
                    onScanPrescriptionImages(savedUris)
                }
            } catch (_: Exception) {}
        }
    }

    val initialSelectedMillis = remember(draftPrescription.date) {
        val localDate = parsePrescriptionDateToLocalDate(draftPrescription.date) ?: LocalDate.now()
        localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis
        )
        val datePickerColors = DatePickerDefaults.colors(
            containerColor = MediciTheme.colors.card,
            titleContentColor = MediciTheme.colors.textPrimary,
            headlineContentColor = MediciTheme.colors.textPrimary,
            weekdayContentColor = MediciTheme.colors.textSecondary,
            subheadContentColor = MediciTheme.colors.textSecondary,
            navigationContentColor = MediciTheme.colors.textPrimary,
            yearContentColor = MediciTheme.colors.textPrimary,
            currentYearContentColor = MediciTheme.colors.primary,
            selectedYearContentColor = MediciTheme.colors.onPrimary,
            selectedYearContainerColor = MediciTheme.colors.primary,
            dayContentColor = MediciTheme.colors.textPrimary,
            selectedDayContainerColor = MediciTheme.colors.primary,
            selectedDayContentColor = MediciTheme.colors.onPrimary,
            todayContentColor = MediciTheme.colors.primary,
            todayDateBorderColor = MediciTheme.colors.primary
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(20.dp),
            colors = datePickerColors,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onDateChanged(formatBengaliDate(selectedLocalDate))
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("prescription_date_picker_confirm")
                ) {
                    Text(
                        text = stringResource(R.string.settings_ok_btn),
                        color = MediciTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.testTag("prescription_date_picker_cancel")
                ) {
                    Text(
                        text = stringResource(R.string.settings_cancel_btn),
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = datePickerColors
            )
        }
    }

    if (showAddPrescriptionSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddPrescriptionSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = MediciTheme.colors.card,
            modifier = Modifier.testTag("add_prescription_options_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MediciTheme.spacing.medium,
                        end = MediciTheme.spacing.medium,
                        bottom = MediciTheme.spacing.large
                    ),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.action_add_prescription_unified),
                    style = MaterialTheme.typography.titleMedium,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Button(
                    onClick = {
                        showAddPrescriptionSheet = false
                        scanPrescriptionLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("sheet_scan_prescription_option"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.primary,
                        contentColor = MediciTheme.colors.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = stringResource(R.string.ocr_scan_with_camera),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(MediciTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.ocr_scan_with_camera),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        showAddPrescriptionSheet = false
                        launcher.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("sheet_upload_gallery_option"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MediciTheme.colors.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MediciTheme.colors.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = stringResource(R.string.ocr_upload_from_gallery),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(MediciTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.ocr_upload_from_gallery),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = stringResource(R.string.ocr_upload_drive_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MediciTheme.spacing.extraSmall)
                        .testTag("sheet_upload_drive_helper")
                )
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .testTag("carousel_card_1"),
        colors = CardDefaults.cardColors(
            containerColor = MediciTheme.colors.card
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = MediciTheme.spacing.medium,
                    top = MediciTheme.spacing.medium,
                    end = MediciTheme.spacing.medium,
                    bottom = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
            ) {
                Text(
                    text = stringResource(R.string.card1_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MediciTheme.tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.card1_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.tokens.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Date Picker Field (Required)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val formattedDisplayDate = remember(draftPrescription.date) {
                        formatBengaliPrescriptionDate(draftPrescription.date)
                    }
                    OutlinedTextField(
                        value = formattedDisplayDate,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = {
                            Text(
                                text = stringResource(R.string.field_date),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.field_date_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.field_date),
                                tint = MediciTheme.tokens.primary
                            )
                        },
                        isError = showError && draftPrescription.date.isNullOrBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prescription_date_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MediciTheme.tokens.textPrimary,
                            disabledBorderColor = if (showError && draftPrescription.date.isNullOrBlank()) {
                                MediciTheme.tokens.warning
                            } else {
                                MediciTheme.tokens.outline
                            },
                            disabledLabelColor = MediciTheme.tokens.textSecondary,
                            disabledLeadingIconColor = MediciTheme.tokens.primary,
                            disabledPlaceholderColor = MediciTheme.tokens.textSecondary
                        )
                    )

                    // Touch overlay covering the entire field so clicks always show the DatePickerDialog
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                showDatePicker = true
                            }
                    )
                }

                if (showError && draftPrescription.date.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.error_date_required),
                        style = MaterialTheme.typography.labelMedium,
                        color = MediciTheme.tokens.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = MediciTheme.spacing.small)
                            .testTag("error_date_required")
                    )
                }
            }

            // Doctor Name Field (Optional)
            OutlinedTextField(
                value = draftPrescription.doctorName,
                onValueChange = onDoctorNameChanged,
                label = {
                    Text(
                        text = stringResource(R.string.field_doctor_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.field_doctor_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.field_doctor_name),
                        tint = MediciTheme.tokens.primary
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doctor_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediciTheme.tokens.primary,
                    unfocusedBorderColor = MediciTheme.tokens.outline,
                    focusedTextColor = MediciTheme.tokens.textPrimary,
                    unfocusedTextColor = MediciTheme.tokens.textPrimary,
                    focusedLabelColor = MediciTheme.tokens.primary,
                    unfocusedLabelColor = MediciTheme.tokens.textSecondary
                )
            )

            // Location Field (Optional)
            OutlinedTextField(
                value = draftPrescription.location,
                onValueChange = onLocationChanged,
                label = {
                    Text(
                        text = stringResource(R.string.field_location),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.field_location_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = stringResource(R.string.field_location),
                        tint = MediciTheme.tokens.primary
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("location_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediciTheme.tokens.primary,
                    unfocusedBorderColor = MediciTheme.tokens.outline,
                    focusedTextColor = MediciTheme.tokens.textPrimary,
                    unfocusedTextColor = MediciTheme.tokens.textPrimary,
                    focusedLabelColor = MediciTheme.tokens.primary,
                    unfocusedLabelColor = MediciTheme.tokens.textSecondary
                )
            )

            // Unified Add Prescription (Scan / Upload) Dashed Box
            val primaryColor = MediciTheme.tokens.primary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MediciTheme.colors.primaryContainer.copy(alpha = 0.18f))
                    .drawBehind {
                        drawRoundRect(
                            color = primaryColor,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx(), 20.dp.toPx())
                        )
                    }
                    .clickable(enabled = !isScanningOcr) {
                        showAddPrescriptionSheet = true
                    }
                    .padding(horizontal = MediciTheme.spacing.medium, vertical = 14.dp)
                    .testTag("add_prescription_unified_box"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = stringResource(R.string.action_add_prescription_unified),
                        tint = MediciTheme.tokens.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.action_add_prescription_unified),
                        style = MaterialTheme.typography.titleSmall,
                        color = MediciTheme.tokens.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isScanningOcr) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ocr_scanning_indicator"),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MediciTheme.colors.primary,
                        trackColor = MediciTheme.colors.primaryContainer
                    )
                    Text(
                        text = stringResource(R.string.ocr_scanning_in_progress),
                        style = MaterialTheme.typography.labelMedium,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("ocr_scanning_text")
                    )
                }
            }

            if (extractedText.isNotBlank() || parsedMedicines.isNotEmpty()) {
                OcrPreviewCard(
                    extractedText = extractedText,
                    parsedMedicines = parsedMedicines,
                    onAddMedicineFromText = onAddMedicineFromText,
                    onAddParsedMedicine = onAddParsedMedicine
                )
            }

            if (draftPrescription.prescriptionImagePaths.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prescription_images_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(draftPrescription.prescriptionImagePaths) { fileName ->
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, MediciTheme.tokens.outline, RoundedCornerShape(20.dp))
                                .testTag("prescription_thumb_$fileName")
                        ) {
                            val imageFile = File(File(context.filesDir, "prescription_images"), fileName)
                            AsyncImage(
                                model = imageFile,
                                contentDescription = stringResource(R.string.prescription_images_header),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .background(MediciTheme.colors.textPrimary.copy(alpha = 0.65f), CircleShape)
                                    .clickable { onImageRemoved(fileName) }
                                    .testTag("remove_image_$fileName"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_remove_photo),
                                    tint = MediciTheme.colors.cardBackground,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
