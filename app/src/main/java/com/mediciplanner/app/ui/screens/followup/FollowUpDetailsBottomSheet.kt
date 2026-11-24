package com.mediciplanner.app.ui.screens.followup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.formatTime
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.ui.components.FullScreenImageDialog
import com.mediciplanner.app.ui.screens.home.formatBengaliDate
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpDetailsBottomSheet(
    followUp: FollowUpEntity,
    onDismiss: () -> Unit,
    onEditClick: (FollowUpEntity) -> Unit,
    onDeleteConfirm: (FollowUpEntity) -> Unit,
    is24Hour: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDoctor = followUp.type == FollowUpType.DOCTOR_VISIT

    var showTopMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var viewingDocumentIndex by remember { mutableIntStateOf(-1) }

    val docFiles = remember(followUp.documentPaths) {
        followUp.documentPaths.map { filename ->
            if (filename.startsWith("/")) File(filename)
            else {
                val f1 = File(context.filesDir, "followup_documents/$filename")
                if (f1.exists()) f1 else File(context.filesDir, "prescription_images/$filename")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MediciTheme.colors.card,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.testTag("followup_details_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // === 1. TOP BAR WITH CLOSE & MOREVERT DOT MENU ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_followup_details_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.followup_cancel_action),
                            tint = MediciTheme.colors.textSecondary
                        )
                    }

                    Text(
                        text = stringResource(R.string.followup_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("followup_details_header_title")
                    )
                }

                Box {
                    IconButton(
                        onClick = { showTopMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("followup_details_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more_options),
                            tint = MediciTheme.colors.textPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                        modifier = Modifier
                            .background(MediciTheme.colors.card)
                            .testTag("followup_details_dropdown_menu")
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.medicine_menu_edit),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MediciTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showTopMenu = false
                                onDismiss()
                                onEditClick(followUp)
                            },
                            modifier = Modifier.testTag("followup_menu_edit")
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.medicine_menu_delete),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MediciTheme.colors.warning,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.warning,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showTopMenu = false
                                showDeleteConfirmDialog = true
                            },
                            modifier = Modifier.testTag("followup_menu_delete")
                        )
                    }
                }
            }

            // === 2. TYPE & STATUS BADGES ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = MediciTheme.colors.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isDoctor) Icons.Default.MedicalServices else Icons.Default.Science,
                        contentDescription = null,
                        tint = MediciTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(
                            if (isDoctor) R.string.followup_type_doctor else R.string.followup_type_test
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (followUp.isCompleted) {
                    Row(
                        modifier = Modifier
                            .background(
                                color = MediciTheme.colors.success.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MediciTheme.colors.success,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.followup_status_completed),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.success,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // === 3. MAIN INFO CARD ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MediciTheme.colors.surface
                ),
                border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title Row
                    DetailInfoRow(
                        icon = if (isDoctor) Icons.Default.Person else Icons.Default.Science,
                        label = stringResource(
                            if (isDoctor) R.string.followup_card_label_doctor else R.string.followup_card_label_test
                        ),
                        value = followUp.title,
                        isPrimaryValue = true
                    )

                    // Location Row (Clinic / Test Center)
                    if (!followUp.location.isNullOrBlank()) {
                        DetailInfoRow(
                            icon = if (isDoctor) Icons.Default.LocalHospital else Icons.Default.Place,
                            label = stringResource(
                                if (isDoctor) R.string.followup_card_label_clinic else R.string.followup_card_label_center
                            ),
                            value = followUp.location
                        )
                    }

                    // Date Row
                    DetailInfoRow(
                        icon = Icons.Default.CalendarToday,
                        label = stringResource(R.string.followup_detail_date_label),
                        value = formatFollowUpDetailDate(followUp.date)
                    )

                    // Time Row
                    DetailInfoRow(
                        icon = Icons.Default.AccessTime,
                        label = stringResource(R.string.followup_detail_time_label),
                        value = formatFollowUpDetailTime(followUp.reminderTime, is24Hour)
                    )

                    // Reminder Status Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (followUp.reminderEnabled) {
                                        MediciTheme.colors.primaryContainer
                                    } else {
                                        MediciTheme.colors.outlineVariant.copy(alpha = 0.3f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (followUp.reminderEnabled) {
                                    Icons.Default.NotificationsActive
                                } else {
                                    Icons.Default.NotificationsOff
                                },
                                contentDescription = null,
                                tint = if (followUp.reminderEnabled) {
                                    MediciTheme.colors.primary
                                } else {
                                    MediciTheme.colors.textSecondary
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (followUp.reminderEnabled) {
                                "${stringResource(R.string.followup_reminder_on)} (${formatFollowUpDetailTime(followUp.reminderTime, is24Hour)})"
                            } else {
                                stringResource(R.string.followup_reminder_off)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (followUp.reminderEnabled) {
                                MediciTheme.colors.primary
                            } else {
                                MediciTheme.colors.textSecondary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // === 4. NOTE CARD (IF PRESENT) ===
            if (!followUp.note.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MediciTheme.colors.surface
                    ),
                    border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = MediciTheme.colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(R.string.followup_detail_note_label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MediciTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = followUp.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MediciTheme.colors.textPrimary,
                            modifier = Modifier.testTag("followup_details_note")
                        )
                    }
                }
            }

            // === 5. ATTACHED DOCUMENTS (MULTI-IMAGE LIST) ===
            if (followUp.documentPaths.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MediciTheme.colors.surface
                    ),
                    border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.followup_documents_label),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.textPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MediciTheme.colors.primaryContainer,
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.followup_documents_count, followUp.documentPaths.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MediciTheme.colors.primary
                                )
                            }
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("followup_details_documents_row"),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(docFiles) { index, file ->
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(
                                            width = 1.dp,
                                            color = MediciTheme.colors.outlineVariant,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable { viewingDocumentIndex = index }
                                        .testTag("followup_detail_doc_thumb_$index")
                                ) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = stringResource(R.string.followup_documents_label),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // === DELETE CONFIRMATION DIALOG (SOFT AMBER WARNING - NO RED) ===
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = MediciTheme.colors.card,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = stringResource(R.string.followup_delete_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.followup_delete_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteConfirm(followUp)
                        onDismiss()
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("confirm_delete_followup_button")
                ) {
                    Text(
                        text = stringResource(R.string.followup_delete_action),
                        color = MediciTheme.colors.warning,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("cancel_delete_followup_button")
                ) {
                    Text(
                        text = stringResource(R.string.followup_cancel_action),
                        color = MediciTheme.colors.textSecondary
                    )
                }
            }
        )
    }

    // === FULL-SCREEN IMAGE VIEWER ===
    if (viewingDocumentIndex >= 0 && docFiles.isNotEmpty()) {
        FullScreenImageDialog(
            imageModels = docFiles,
            initialIndex = viewingDocumentIndex,
            onDismiss = { viewingDocumentIndex = -1 },
            title = stringResource(R.string.followup_documents_label)
        )
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isPrimaryValue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = MediciTheme.colors.primaryContainer.copy(alpha = 0.65f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MediciTheme.colors.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MediciTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = if (isPrimaryValue) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                fontWeight = if (isPrimaryValue) FontWeight.Bold else FontWeight.Medium,
                color = MediciTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatFollowUpDetailDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val clean = dateStr.trim().take(10)
        val localDate = LocalDate.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE)
        formatBengaliDate(localDate)
    } catch (_: Exception) {
        toBengaliDigits(dateStr)
    }
}

private fun formatFollowUpDetailTime(timeStr: String, is24Hour: Boolean): String {
    val clean = timeStr.ifBlank { "09:00" }
    return formatTime(clean, is24Hour)
}
