package com.mediciplanner.app.ui.screens.followup

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import java.util.UUID
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.formatTime
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.ui.components.AccessibleTimeInputDialog
import com.mediciplanner.app.ui.components.BouncingButton
import com.mediciplanner.app.ui.screens.home.formatBengaliDate
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private fun formatDisplayDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val clean = dateStr.trim().take(10)
        val localDate = LocalDate.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE)
        formatBengaliDate(localDate)
    } catch (_: Exception) {
        toBengaliDigits(dateStr)
    }
}

private fun formatDisplayTime(timeStr: String, is24Hour: Boolean): String {
    val clean = timeStr.ifBlank { "09:00" }
    return formatTime(clean, is24Hour)
}

/**
 * Smart Form Add / Edit Modal Bottom Sheet:
 * - Top: Type Switcher (FilterChips: "ডাক্তার ভিজিট" vs "টেস্ট")
 * - when(selectedType) renders completely separate form states to prevent data overlap:
 *   - Doctor Visit: Doctor Name (Req), Hospital/Clinic (Opt), Date (Req), Note (Opt), Reminder Switch
 *   - Test: Test Name (Req), Test Center (Opt), Date (Req), Note (Opt), Reminder Switch
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpAddEditSheet(
    viewModel: FollowUpViewModel,
    onDismiss: () -> Unit,
    onDelete: (FollowUpEntity) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draftFollowUp
    val selectedType = draft.type
    val editingFollowUp by viewModel.editingFollowUp.collectAsStateWithLifecycle()
    val isEditing = editingFollowUp != null

    var doctorNameError by remember { mutableStateOf(false) }
    var doctorDateError by remember { mutableStateOf(false) }
    var testNameError by remember { mutableStateOf(false) }
    var testDateError by remember { mutableStateOf(false) }

    // DatePickerDialog setup
    val calendar = remember { Calendar.getInstance() }
    val currentDateStr = draft.date

    val initialYear = try {
        if (currentDateStr.length >= 4) currentDateStr.take(4).toInt() else calendar.get(Calendar.YEAR)
    } catch (_: Exception) {
        calendar.get(Calendar.YEAR)
    }
    val initialMonth = try {
        if (currentDateStr.length >= 7) currentDateStr.substring(5, 7).toInt() - 1 else calendar.get(Calendar.MONTH)
    } catch (_: Exception) {
        calendar.get(Calendar.MONTH)
    }
    val initialDay = try {
        if (currentDateStr.length >= 10) currentDateStr.substring(8, 10).toInt() else calendar.get(Calendar.DAY_OF_MONTH)
    } catch (_: Exception) {
        calendar.get(Calendar.DAY_OF_MONTH)
    }

    val datePickerDialog = remember(selectedType, currentDateStr) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = LocalDate.of(year, month + 1, dayOfMonth)
                val isoDate = chosen.format(DateTimeFormatter.ISO_LOCAL_DATE)
                viewModel.updateDraft(FollowUpDraftField.DATE, isoDate)
                if (selectedType == FollowUpType.DOCTOR_VISIT) {
                    doctorDateError = false
                } else {
                    testDateError = false
                }
            },
            initialYear,
            initialMonth,
            initialDay
        )
    }

    val currentTimeStr = draft.time.ifBlank { "09:00" }
    val initialHour = try { currentTimeStr.substringBefore(":").toInt() } catch (_: Exception) { 9 }
    val initialMinute = try { currentTimeStr.substringAfter(":").take(2).toInt() } catch (_: Exception) { 0 }
    var showTimeInputDialog by remember { mutableStateOf(false) }

    val docLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            try {
                val docsDir = File(context.filesDir, "followup_documents")
                if (!docsDir.exists()) {
                    docsDir.mkdirs()
                }
                val newFileNames = mutableListOf<String>()
                for (uri in uris) {
                    val fileName = "${UUID.randomUUID()}.jpg"
                    val destFile = File(docsDir, fileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    newFileNames.add(fileName)
                }
                val updatedDocs = viewModel.draftFollowUp.value.documentPaths + newFileNames
                viewModel.updateDraft(FollowUpDraftField.DOCUMENT_PATHS, updatedDocs)
                if (newFileNames.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_followup_document_added),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (uiState.hasDraft) {
                Toast.makeText(
                    context,
                    context.getString(R.string.msg_followup_draft_saved),
                    Toast.LENGTH_SHORT
                ).show()
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MediciTheme.colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = MediciTheme.colors.textSecondary.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = MediciTheme.spacing.large)
                .padding(bottom = MediciTheme.spacing.large)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .testTag("followup_add_edit_sheet"),
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // Sheet Title
            Text(
                text = if (isEditing) {
                    stringResource(R.string.followup_edit_dialog_title)
                } else {
                    stringResource(R.string.followup_dialog_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MediciTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // === 1. TYPE SELECTOR (Segmented / FilterChips) ===
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.followup_type_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedType == FollowUpType.DOCTOR_VISIT,
                        onClick = {
                            viewModel.updateDraft(FollowUpDraftField.TYPE, FollowUpType.DOCTOR_VISIT)
                            doctorNameError = false
                            doctorDateError = false
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.followup_type_doctor),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primary,
                            selectedLabelColor = MediciTheme.colors.onPrimary,
                            selectedLeadingIconColor = MediciTheme.colors.onPrimary,
                            containerColor = MediciTheme.colors.card,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("type_doctor_chip")
                    )

                    FilterChip(
                        selected = selectedType == FollowUpType.TEST,
                        onClick = {
                            viewModel.updateDraft(FollowUpDraftField.TYPE, FollowUpType.TEST)
                            testNameError = false
                            testDateError = false
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.followup_type_test),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primary,
                            selectedLabelColor = MediciTheme.colors.onPrimary,
                            selectedLeadingIconColor = MediciTheme.colors.onPrimary,
                            containerColor = MediciTheme.colors.card,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("type_test_chip")
                    )
                }
            }

            // === 2. SMART FORM CONTENT: when(selectedType) ===
            when (selectedType) {
                FollowUpType.DOCTOR_VISIT -> {
                    // Doctor Name (Required)
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = {
                            viewModel.updateDraft(FollowUpDraftField.TITLE, it)
                            if (it.isNotBlank()) doctorNameError = false
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.followup_doctor_name_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.followup_doctor_name_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        isError = doctorNameError,
                        supportingText = if (doctorNameError) {
                            {
                                Text(
                                    text = stringResource(R.string.followup_error_doctor_name_required),
                                    color = MediciTheme.colors.warning,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("error_doctor_name")
                                )
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doctor_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = if (doctorNameError) MediciTheme.colors.warning else MediciTheme.colors.outlineVariant,
                            errorBorderColor = MediciTheme.colors.warning,
                            errorLabelColor = MediciTheme.colors.warning
                        )
                    )

                    // Hospital / Clinic Name (Optional)
                    OutlinedTextField(
                        value = draft.location,
                        onValueChange = { viewModel.updateDraft(FollowUpDraftField.LOCATION, it) },
                        label = {
                            Text(
                                text = stringResource(R.string.followup_hospital_clinic_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.followup_hospital_clinic_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hospital_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = MediciTheme.colors.outlineVariant
                        )
                    )

                    // Date Field (Required, DatePicker)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatDisplayDate(draft.date),
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = {
                                Text(
                                    text = stringResource(R.string.followup_input_date),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (doctorDateError) MediciTheme.colors.warning else MediciTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            isError = doctorDateError,
                            supportingText = if (doctorDateError) {
                                {
                                    Text(
                                        text = stringResource(R.string.followup_error_date),
                                        color = MediciTheme.colors.warning,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("error_doctor_date")
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("doctor_visit_date_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MediciTheme.colors.textPrimary,
                                disabledBorderColor = if (doctorDateError) MediciTheme.colors.warning else MediciTheme.colors.outlineVariant,
                                disabledLabelColor = if (doctorDateError) MediciTheme.colors.warning else MediciTheme.colors.textSecondary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    datePickerDialog.show()
                                }
                        )
                    }

                    // Time Picker Field (Reminder Time - Default 09:00 AM)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatDisplayTime(draft.time, uiState.is24Hour),
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = {
                                Text(
                                    text = stringResource(R.string.followup_time_label),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("doctor_visit_time_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MediciTheme.colors.textPrimary,
                                disabledBorderColor = MediciTheme.colors.outlineVariant,
                                disabledLabelColor = MediciTheme.colors.textSecondary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .testTag("doctor_visit_time_clickable")
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    showTimeInputDialog = true
                                }
                        )
                    }

                    // Note Field (Optional, Multiline)
                    OutlinedTextField(
                        value = draft.note,
                        onValueChange = { viewModel.updateDraft(FollowUpDraftField.NOTE, it) },
                        label = {
                            Text(
                                text = stringResource(R.string.followup_input_note),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 3,
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doctor_visit_note_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = MediciTheme.colors.outlineVariant
                        )
                    )

                    // Document Upload Section (Below Note Field)
                    DocumentUploadSection(
                        documentPaths = draft.documentPaths,
                        onAddDocuments = { docLauncher.launch("image/*") },
                        onRemoveDocument = { path ->
                            viewModel.updateDraft(
                                FollowUpDraftField.DOCUMENT_PATHS,
                                draft.documentPaths - path
                            )
                        },
                        context = context
                    )

                    // Reminder Toggle Switch
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MediciTheme.colors.card
                        ),
                        border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MediciTheme.spacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.followup_reminder_on),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${stringResource(R.string.followup_time_label)}: ${formatDisplayTime(draft.time, uiState.is24Hour)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MediciTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Switch(
                                checked = draft.reminderEnabled,
                                onCheckedChange = {
                                    viewModel.updateDraft(FollowUpDraftField.REMINDER_ENABLED, it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MediciTheme.colors.onPrimary,
                                    checkedTrackColor = MediciTheme.colors.primary,
                                    uncheckedThumbColor = MediciTheme.colors.textSecondary,
                                    uncheckedTrackColor = MediciTheme.colors.surface
                                ),
                                modifier = Modifier.testTag("doctor_visit_reminder_switch")
                            )
                        }
                    }
                }

                FollowUpType.TEST -> {
                    // Test Name (Required, with specific placeholder)
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = {
                            viewModel.updateDraft(FollowUpDraftField.TITLE, it)
                            if (it.isNotBlank()) testNameError = false
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.followup_test_name_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.followup_test_name_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        isError = testNameError,
                        supportingText = if (testNameError) {
                            {
                                Text(
                                    text = stringResource(R.string.followup_error_test_name_required),
                                    color = MediciTheme.colors.warning,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("error_test_name")
                                )
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = if (testNameError) MediciTheme.colors.warning else MediciTheme.colors.outlineVariant,
                            errorBorderColor = MediciTheme.colors.warning,
                            errorLabelColor = MediciTheme.colors.warning
                        )
                    )

                    // Test Center Name (Optional)
                    OutlinedTextField(
                        value = draft.location,
                        onValueChange = { viewModel.updateDraft(FollowUpDraftField.LOCATION, it) },
                        label = {
                            Text(
                                text = stringResource(R.string.followup_test_center_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.followup_test_center_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_center_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = MediciTheme.colors.outlineVariant
                        )
                    )

                    // Date Field (Required, DatePicker)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatDisplayDate(draft.date),
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = {
                                Text(
                                    text = stringResource(R.string.followup_input_date),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (testDateError) MediciTheme.colors.warning else MediciTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            isError = testDateError,
                            supportingText = if (testDateError) {
                                {
                                    Text(
                                        text = stringResource(R.string.followup_error_date),
                                        color = MediciTheme.colors.warning,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("error_test_date")
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_date_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MediciTheme.colors.textPrimary,
                                disabledBorderColor = if (testDateError) MediciTheme.colors.warning else MediciTheme.colors.outlineVariant,
                                disabledLabelColor = if (testDateError) MediciTheme.colors.warning else MediciTheme.colors.textSecondary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    datePickerDialog.show()
                                }
                        )
                    }

                    // Time Picker Field (Reminder Time - Default 09:00 AM)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatDisplayTime(draft.time, uiState.is24Hour),
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = {
                                Text(
                                    text = stringResource(R.string.followup_time_label),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_time_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MediciTheme.colors.textPrimary,
                                disabledBorderColor = MediciTheme.colors.outlineVariant,
                                disabledLabelColor = MediciTheme.colors.textSecondary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .testTag("test_time_clickable")
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    showTimeInputDialog = true
                                }
                        )
                    }

                    // Note Field (Optional, Multiline)
                    OutlinedTextField(
                        value = draft.note,
                        onValueChange = { viewModel.updateDraft(FollowUpDraftField.NOTE, it) },
                        label = {
                            Text(
                                text = stringResource(R.string.followup_input_note),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 3,
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_note_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = MediciTheme.colors.outlineVariant
                        )
                    )

                    // Document Upload Section (Below Note Field)
                    DocumentUploadSection(
                        documentPaths = draft.documentPaths,
                        onAddDocuments = { docLauncher.launch("image/*") },
                        onRemoveDocument = { path ->
                            viewModel.updateDraft(
                                FollowUpDraftField.DOCUMENT_PATHS,
                                draft.documentPaths - path
                            )
                        },
                        context = context
                    )

                    // Reminder Toggle Switch
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MediciTheme.colors.card
                        ),
                        border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MediciTheme.spacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.followup_reminder_on),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${stringResource(R.string.followup_time_label)}: ${formatDisplayTime(draft.time, uiState.is24Hour)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MediciTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Switch(
                                checked = draft.reminderEnabled,
                                onCheckedChange = {
                                    viewModel.updateDraft(FollowUpDraftField.REMINDER_ENABLED, it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MediciTheme.colors.onPrimary,
                                    checkedTrackColor = MediciTheme.colors.primary,
                                    uncheckedThumbColor = MediciTheme.colors.textSecondary,
                                    uncheckedTrackColor = MediciTheme.colors.surface
                                ),
                                modifier = Modifier.testTag("test_reminder_switch")
                            )
                        }
                    }
                }
            }

            // Bottom Action Buttons: Cancel ("বাতিল") & Save ("সেভ করুন")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearDraft()
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MediciTheme.colors.textSecondary
                    ),
                    border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("followup_cancel_button")
                ) {
                    Text(
                        text = stringResource(R.string.followup_cancel_action),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BouncingButton(
                    onClick = {
                        val isNameBlank = draft.title.trim().isBlank()
                        val isDateBlank = draft.date.trim().isBlank()
                        if (selectedType == FollowUpType.DOCTOR_VISIT) {
                            doctorNameError = isNameBlank
                            doctorDateError = isDateBlank
                        } else {
                            testNameError = isNameBlank
                            testDateError = isDateBlank
                        }
                        if (!isNameBlank && !isDateBlank) {
                            viewModel.saveFollowUp(
                                onSaved = onDismiss,
                                onSavedWithEditFlag = { wasEdit ->
                                    val msgRes = if (wasEdit) {
                                        R.string.msg_followup_updated_with_dest
                                    } else {
                                        R.string.msg_followup_saved_with_dest
                                    }
                                    Toast.makeText(
                                        context,
                                        context.getString(msgRes),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.primary,
                        contentColor = MediciTheme.colors.onPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("followup_save_button")
                ) {
                    Text(
                        text = stringResource(R.string.followup_save_action),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // If Editing: Delete Button ("মুছে ফেলুন") in Soft Amber Warning (Strictly NO RED)
            if (isEditing && editingFollowUp != null) {
                val deleteInteraction = remember { MutableInteractionSource() }
                val isDeletePressed by deleteInteraction.collectIsPressedAsState()
                val deleteScale by animateFloatAsState(
                    targetValue = if (isDeletePressed) 0.96f else 1f,
                    label = "delete_scale"
                )
                val haptic = LocalHapticFeedback.current
                LaunchedEffect(isDeletePressed) {
                    if (isDeletePressed) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    }
                }
                OutlinedButton(
                    onClick = { onDelete(editingFollowUp!!) },
                    interactionSource = deleteInteraction,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MediciTheme.colors.warning
                    ),
                    border = BorderStroke(1.dp, MediciTheme.colors.warning),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .scale(deleteScale)
                        .testTag("followup_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.followup_delete_action),
                        tint = MediciTheme.colors.warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.followup_delete_action),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showTimeInputDialog) {
        AccessibleTimeInputDialog(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = uiState.is24Hour,
            onConfirm = { hourOfDay, minute ->
                val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                viewModel.updateDraft(FollowUpDraftField.TIME, formatted)
                showTimeInputDialog = false
            },
            onDismiss = { showTimeInputDialog = false }
        )
    }
}

@Composable
private fun DocumentUploadSection(
    documentPaths: List<String>,
    onAddDocuments: () -> Unit,
    onRemoveDocument: (String) -> Unit,
    context: android.content.Context
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.followup_documents_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MediciTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.followup_documents_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MediciTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("followup_documents_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(documentPaths) { fileName ->
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MediciTheme.colors.outlineVariant, RoundedCornerShape(14.dp))
                        .testTag("document_thumb_$fileName")
                ) {
                    val file = File(File(context.filesDir, "followup_documents"), fileName)
                    AsyncImage(
                        model = file,
                        contentDescription = stringResource(R.string.followup_documents_label),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .background(MediciTheme.colors.textPrimary.copy(alpha = 0.65f), CircleShape)
                            .clickable { onRemoveDocument(fileName) }
                            .testTag("remove_document_$fileName"),
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

            item {
                val primaryColor = MediciTheme.colors.primary
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MediciTheme.colors.card)
                        .drawBehind {
                            drawRoundRect(
                                color = primaryColor,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx())
                            )
                        }
                        .clickable { onAddDocuments() }
                        .testTag("add_followup_document_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.action_add),
                            tint = MediciTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.followup_documents_add_btn),
                            style = MaterialTheme.typography.labelSmall,
                            color = MediciTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
