package com.mediciplanner.app.ui.screens.prescription.components

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.mediciplanner.app.ui.components.BouncingButton
import com.mediciplanner.app.ui.components.FullScreenImageDialog
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediciplanner.app.MediciApplication
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.TimeFormatHelper
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.relation.MedicineWithSchedules
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import com.mediciplanner.app.ui.components.SlotFilterRow
import com.mediciplanner.app.ui.screens.home.components.MedicineCard
import com.mediciplanner.app.ui.screens.home.components.MedicineDetailBottomSheet
import com.mediciplanner.app.ui.screens.home.formatBengaliDate
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.PrescriptionDetailsUiState
import com.mediciplanner.app.ui.screens.prescription.PrescriptionDetailsViewModel
import com.mediciplanner.app.ui.screens.prescription.PrescriptionDetailsViewModelFactory
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

private fun toAsciiDigits(input: String): String {
    return input.map { ch ->
        if (ch in '০'..'৯') ('0' + (ch - '০')) else ch
    }.joinToString("")
}

private fun parseMonthTokenToNumber(rawToken: String): Int? {
    val token = rawToken.trim().trimEnd('.', ',').lowercase(Locale.US)
    if (token.isEmpty()) return null
    return when {
        token.startsWith("জানু") || token.startsWith("jan") -> 1
        token.startsWith("ফেব") || token.startsWith("feb") -> 2
        token.startsWith("মার") || token.startsWith("mar") -> 3
        token.startsWith("এপ্রি") || token.startsWith("apr") -> 4
        token == "মে" || token == "may" -> 5
        token == "জুন" || token.startsWith("jun") -> 6
        token.startsWith("জুল") || token.startsWith("jul") -> 7
        token.startsWith("আগ") || token.startsWith("aug") -> 8
        token.startsWith("সেপ") || token.startsWith("sep") -> 9
        token.startsWith("অক") || token.startsWith("oct") -> 10
        token.startsWith("নভে") || token.startsWith("nov") -> 11
        token.startsWith("ডিসে") || token.startsWith("dec") -> 12
        else -> null
    }
}

internal fun parsePrescriptionDateToLocalDate(dateStr: String?): LocalDate? {
    if (dateStr.isNullOrBlank()) return null
    val trimmed = dateStr.trim()
    val ascii = toAsciiDigits(trimmed)

    // 1. ISO date prefix (yyyy-MM-dd or yyyy-MM-dd'T'...)
    if (ascii.length >= 10 && ascii[4] == '-' && ascii[7] == '-') {
        try {
            return LocalDate.parse(ascii.take(10), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
        }
    }

    // 2. "dd MMMM, yyyy" or "d MMM, yyyy" (Bengali or English month names/abbreviations)
    val dayMonthYearMatch = Regex("""^(\d{1,2})\s+([^\d,\s]+?),?\s+(\d{4})$""").matchEntire(ascii)
    if (dayMonthYearMatch != null) {
        val day = dayMonthYearMatch.groupValues[1].toIntOrNull()
        val monthToken = dayMonthYearMatch.groupValues[2]
        val year = dayMonthYearMatch.groupValues[3].toIntOrNull()
        val month = parseMonthTokenToNumber(monthToken)
        if (day != null && month != null && year != null) {
            try {
                return LocalDate.of(year, month, day)
            } catch (_: Exception) {
            }
        }
    }

    // 3. "MMM d, yyyy" (English month-first)
    val monthDayYearMatch = Regex("""^([^\d,\s]+?)\s+(\d{1,2}),?\s+(\d{4})$""").matchEntire(ascii)
    if (monthDayYearMatch != null) {
        val month = parseMonthTokenToNumber(monthDayYearMatch.groupValues[1])
        val day = monthDayYearMatch.groupValues[2].toIntOrNull()
        val year = monthDayYearMatch.groupValues[3].toIntOrNull()
        if (day != null && month != null && year != null) {
            try {
                return LocalDate.of(year, month, day)
            } catch (_: Exception) {
            }
        }
    }

    // 4. Slash-separated dd/MM/yyyy or yyyy/MM/dd
    val slashPatterns = listOf("dd/MM/yyyy", "d/M/yyyy", "yyyy/MM/dd")
    for (pattern in slashPatterns) {
        try {
            val parser = java.text.SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val parsed = parser.parse(ascii)
            if (parsed != null) {
                val cal = Calendar.getInstance().apply { time = parsed }
                return LocalDate.of(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
                )
            }
        } catch (_: Exception) {
        }
    }

    return null
}

/**
 * Formats a prescription date (ISO, full Bengali, or legacy abbreviated date)
 * into localized Bengali with full month name (`dd MMMM, yyyy`, e.g., "১৯ নভেম্বর, ২০২৬").
 */
fun formatBengaliPrescriptionDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    val localDate = parsePrescriptionDateToLocalDate(dateStr)
    if (localDate != null) {
        return formatBengaliDate(localDate)
    }
    return toBengaliDigits(dateStr)
}

/**
 * Formats an ISO-8601 timestamp (or epoch millis string) using full month name (`dd MMMM, yyyy`)
 * and the user's global 12H/24H time preference via [TimeFormatHelper]
 * (e.g., "১৯ নভেম্বর, ২০২৬ - 09:50 AM" in 12H or "১৯ নভেম্বর, ২০২৬ - 09:50" in 24H).
 */
fun formatVersionTimestamp(
    createdAt: String?,
    is24Hour: Boolean = false
): String {
    if (createdAt.isNullOrBlank()) return ""
    val trimmed = createdAt.trim()

    fun formatDateAndTime(date: java.util.Date, includeTime: Boolean): String {
        val cal = Calendar.getInstance().apply { time = date }
        val localDate = LocalDate.of(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val formattedDate = formatBengaliDate(localDate)
        if (!includeTime) return formattedDate
        val rawTime = String.format(
            Locale.US,
            "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        )
        val formattedTime = TimeFormatHelper.formatTime(rawTime, is24Hour)
        return "$formattedDate - $formattedTime"
    }

    val millis = trimmed.toLongOrNull()
    if (millis != null) {
        val localDateTime = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val formattedDate = formatBengaliDate(localDateTime.toLocalDate())
        val rawTime = String.format(Locale.US, "%02d:%02d", localDateTime.hour, localDateTime.minute)
        val formattedTime = TimeFormatHelper.formatTime(rawTime, is24Hour)
        return "$formattedDate - $formattedTime"
    }

    val utcPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (pattern in utcPatterns) {
        try {
            val parser = java.text.SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val parsed = parser.parse(trimmed)
            if (parsed != null) return formatDateAndTime(parsed, includeTime = true)
        } catch (_: Exception) {
        }
    }

    val localPatternsWithTime = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm"
    )
    for (pattern in localPatternsWithTime) {
        try {
            val parser = java.text.SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
            }
            val parsed = parser.parse(trimmed)
            if (parsed != null) return formatDateAndTime(parsed, includeTime = true)
        } catch (_: Exception) {
        }
    }

    try {
        val dateOnlyParser = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val parsed = dateOnlyParser.parse(trimmed)
        if (parsed != null) return formatDateAndTime(parsed, includeTime = false)
    } catch (_: Exception) {
    }

    return ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionDetailBottomSheet(
    prescriptionWithMedicines: PrescriptionWithMedicines,
    onDismiss: () -> Unit,
    onEditClick: (PrescriptionWithMedicines) -> Unit,
    onDeleteClick: (String) -> Unit,
    onAddNewMedicineClick: (PrescriptionWithMedicines) -> Unit = {},
    onEditMedicineClick: (PrescriptionWithMedicines, MedicineEntity) -> Unit = { _, _ -> },
    onDeleteMedicineClick: (MedicineEntity) -> Unit = {},
    detailsViewModel: PrescriptionDetailsViewModel? = null,
    modifier: Modifier = Modifier,
    is24HourOverride: Boolean? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MediciApplication
    val repository = app?.prescriptionRepository
    val timeFormat by (app?.themePreferences?.timeFormatFlow
        ?: flowOf(com.mediciplanner.app.core.data.preferences.ThemePreferences.TIME_FORMAT_12H))
        .collectAsStateWithLifecycle(initialValue = com.mediciplanner.app.core.data.preferences.ThemePreferences.TIME_FORMAT_12H)
    val is24Hour = is24HourOverride
        ?: (timeFormat == com.mediciplanner.app.core.data.preferences.ThemePreferences.TIME_FORMAT_24H)

    val resolvedDetailsViewModel: PrescriptionDetailsViewModel? = detailsViewModel ?: if (repository != null) {
        viewModel(factory = PrescriptionDetailsViewModelFactory(repository))
    } else {
        null
    }

    // State of the currently active prescription version being displayed
    var activeItem by remember(prescriptionWithMedicines) { mutableStateOf(prescriptionWithMedicines) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showVersionHistorySheet by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var fullScreenInitialIndex by remember { mutableIntStateOf(0) }
    var selectedMedicineForDetail by remember { mutableStateOf<MedicineWithSchedules?>(null) }

    val parentId = prescriptionWithMedicines.prescription.parentId
    val allVersions by (repository?.getPrescriptionVersionsWithMedicines(parentId)
        ?: flowOf(listOf(prescriptionWithMedicines))).collectAsStateWithLifecycle(initialValue = listOf(prescriptionWithMedicines))

    // Real-time synchronization if underlying medicines or prescription change
    val syncItem = allVersions.find { it.prescription.id == activeItem.prescription.id } ?: activeItem
    val prescription = syncItem.prescription
    val medicines = syncItem.medicines
    val isViewingLatest = prescription.isLatest
    var selectedSlot by remember(prescription.id) { mutableStateOf<DoseSlot?>(null) }
    val filteredMedicines = remember(medicines, selectedSlot) {
        val slot = selectedSlot
        if (slot == null) {
            medicines
        } else {
            medicines.filter { medWithSched ->
                medWithSched.doseSchedules.any { it.doseSchedule.slot == slot }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(syncItem.prescription.id, syncItem.medicines) {
        resolvedDetailsViewModel?.loadVersion(syncItem)
    }

    val detailsUiState by (resolvedDetailsViewModel?.uiState
        ?: flowOf(PrescriptionDetailsUiState(currentPrescription = syncItem)))
        .collectAsStateWithLifecycle(initialValue = PrescriptionDetailsUiState(currentPrescription = syncItem))

    val showVersionDiffBadges = prescription.versionNumber != 1 &&
        prescription.versionNumber >= 2 &&
        detailsUiState.currentPrescription?.prescription?.id == prescription.id &&
        !detailsUiState.isLoading

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formattedDate = formatBengaliPrescriptionDate(prescription.date)
    val doctorName = if (!prescription.doctorName.isNullOrBlank()) {
        prescription.doctorName
    } else {
        stringResource(R.string.prescription_no_doctor)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MediciTheme.colors.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier.testTag("prescription_detail_bottom_sheet")
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 80.dp)
            ) {
            // === 1. TOP BAR & DOT MENU ===
            // Left: "প্রেসক্রিপশনের বিবরণ" + Version Chip (V1)
            // Right: A "⋮" (MoreVert) IconButton opening DropdownMenu with Edit and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Title + Version Chip (V1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_prescription_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.prescription_cancel),
                            tint = MediciTheme.colors.textSecondary
                        )
                    }

                    Text(
                        text = stringResource(R.string.prescription_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("prescription_detail_title")
                    )

                    // Version Chip (V1)
                    Box(
                        modifier = Modifier
                            .background(
                                color = MediciTheme.colors.primaryContainer,
                                shape = CircleShape
                            )
                            .clickable { showVersionHistorySheet = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("prescription_version_chip")
                    ) {
                        Text(
                            text = "V${prescription.versionNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: A "⋮" (MoreVert) IconButton (only visible on latest version)
                if (isViewingLatest) {
                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier.testTag("prescription_more_options_button")
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
                                .testTag("prescription_top_menu")
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.prescription_menu_edit),
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
                                    onEditClick(syncItem)
                                },
                                modifier = Modifier.testTag("menu_edit_prescription")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.prescription_menu_delete),
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
                                modifier = Modifier.testTag("menu_delete_prescription")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Version Indicator & History CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MediciTheme.colors.primaryContainer.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.prescription_version_current, prescription.versionNumber),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("prescription_version_text")
                    )
                }

                AssistChip(
                    onClick = { showVersionHistorySheet = true },
                    label = {
                        Text(
                            text = stringResource(R.string.prescription_version_history),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MediciTheme.colors.surface,
                        labelColor = MediciTheme.colors.primary,
                        leadingIconContentColor = MediciTheme.colors.primary
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = MediciTheme.colors.primary.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("version_history_button")
                )
            }

            // Banner for older versions (read-only mode)
            if (!isViewingLatest) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MediciTheme.colors.warning.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, MediciTheme.colors.warning.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("prescription_readonly_warning_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MediciTheme.colors.warning,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.prescription_version_readonly_notice),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MediciTheme.colors.warning,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("prescription_readonly_warning_text")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prescription Header Card (Date, Doctor, Location)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MediciTheme.colors.surface,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.prescription_date_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MediciTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("detail_prescription_date")
                                )
                                if (showVersionDiffBadges && detailsUiState.isDateChanged) {
                                    HeaderEditBadge(testTag = "header_date_edit_badge")
                                }
                            }
                        }
                    }

                    // Doctor Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MediciTheme.colors.primaryContainer.copy(alpha = 0.5f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.prescription_doctor_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = doctorName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MediciTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("detail_prescription_doctor")
                                )
                                if (showVersionDiffBadges && detailsUiState.isDoctorChanged) {
                                    HeaderEditBadge(testTag = "header_doctor_edit_badge")
                                }
                            }
                        }
                    }

                    // Location (if present)
                    if (!prescription.location.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MediciTheme.colors.surface,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = prescription.location,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MediciTheme.colors.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (showVersionDiffBadges && detailsUiState.isLocationChanged) {
                                    HeaderEditBadge(testTag = "header_location_edit_badge")
                                }
                            }
                        }
                    }
                }
            }

            // Multi-Image Display (LazyRow of thumbnails below Date/Doctor header)
            if (prescription.prescriptionImagePaths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.prescription_images_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                val imageFiles = remember(prescription.prescriptionImagePaths) {
                    prescription.prescriptionImagePaths.map { filename ->
                        if (filename.startsWith("/")) File(filename)
                        else File(context.filesDir, "prescription_images/$filename")
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prescription_detail_images_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(imageFiles) { index, imageFile ->
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MediciTheme.colors.outlineVariant, RoundedCornerShape(12.dp))
                                .background(MediciTheme.colors.surface)
                                .clickable {
                                    fullScreenInitialIndex = index
                                    showFullScreenImage = true
                                }
                                .testTag("prescription_detail_thumb_$index")
                        ) {
                            AsyncImage(
                                model = imageFile,
                                contentDescription = stringResource(R.string.prescription_images_header),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Medicines Header & Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.prescription_medicines_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val countStr = toBengaliDigits(filteredMedicines.size.toString())
                Box(
                    modifier = Modifier
                        .background(
                            color = MediciTheme.colors.primaryContainer,
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.prescription_medicines_count, countStr),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SlotFilterRow(
                selectedSlot = selectedSlot,
                onSlotSelected = { selectedSlot = it },
                fadeColor = MediciTheme.colors.card,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // === 3. MEDICINE CARD CONSISTENCY ===
            // In the medicine list inside the Bottom Sheet, ensure the cards look EXACTLY like the MedicineCard on Home Screen.
            // Right Side: "⋮" (MoreVert) IconButton opening DropdownMenu with Edit and Delete options.
            if (medicines.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_prescription_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else if (filteredMedicines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MediciTheme.colors.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.filter_slot_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("detail_filtered_empty_text")
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("detail_medicines_list")
                ) {
                    filteredMedicines.forEachIndexed { index, medWithSched ->
                        val activeSchedule = if (selectedSlot != null) {
                            medWithSched.doseSchedules.firstOrNull {
                                it.doseSchedule.slot == selectedSlot
                            }?.doseSchedule ?: medWithSched.doseSchedules.firstOrNull()?.doseSchedule
                        } else {
                            medWithSched.doseSchedules.firstOrNull()?.doseSchedule
                        }
                        MedicineCard(
                            medicine = medWithSched.medicine,
                            schedule = activeSchedule,
                            isTaken = false,
                            staggerIndex = index,
                            isReadOnly = !isViewingLatest,
                            showTakeButton = false,
                            changeType = if (prescription.versionNumber == 1 || !showVersionDiffBadges) {
                                null
                            } else {
                                detailsUiState.medicineChangeTypes[medWithSched.medicine.id]
                            },
                            onClick = {
                                selectedMedicineForDetail = medWithSched
                            },
                            onEditClick = {
                                if (isViewingLatest) {
                                    onDismiss()
                                    onEditMedicineClick(syncItem, medWithSched.medicine)
                                }
                            },
                            onDeleteClick = {
                                if (isViewingLatest) {
                                    onDeleteMedicineClick(medWithSched.medicine)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

            // === 2. FIXED BOTTOM CTA (Add Medicine) ===
            // Only visible for latest version, fixed outside scrollable Column
            if (isViewingLatest) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MediciTheme.colors.surface)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("prescription_detail_bottom_cta_container")
                ) {
                    BouncingButton(
                        onClick = {
                            onDismiss()
                            onAddNewMedicineClick(syncItem)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MediciTheme.colors.primary,
                            contentColor = MediciTheme.colors.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_medicine_bottom_button")
                    ) {
                        Text(
                            text = stringResource(R.string.prescription_add_medicine_cta),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Version History Dialog / Sheet
    if (showVersionHistorySheet) {
        AlertDialog(
            onDismissRequest = { showVersionHistorySheet = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MediciTheme.colors.primary
                    )
                    Text(
                        text = stringResource(R.string.prescription_version_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (allVersions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.prescription_no_previous_versions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        allVersions.forEach { versionItem ->
                            val vPres = versionItem.prescription
                            val isSelected = vPres.id == syncItem.prescription.id
                            val versionLabel = "V${vPres.versionNumber} - ${formatBengaliPrescriptionDate(vPres.date)}"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeItem = versionItem
                                        showVersionHistorySheet = false
                                    }
                                    .testTag("version_item_${vPres.versionNumber}"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MediciTheme.colors.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MediciTheme.colors.surface
                                    }
                                ),
                                border = if (isSelected) {
                                    BorderStroke(1.5.dp, MediciTheme.colors.primary)
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = versionLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MediciTheme.colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (vPres.isLatest) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = MediciTheme.colors.primary,
                                                            shape = CircleShape
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.prescription_version_latest),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MediciTheme.colors.onPrimary,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        val countText = stringResource(R.string.prescription_medicines_count, toBengaliDigits(versionItem.medicines.size.toString()))
                                        Text(
                                            text = countText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MediciTheme.colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        val formattedTimestamp = formatVersionTimestamp(vPres.createdAt, is24Hour = is24Hour)
                                        if (formattedTimestamp.isNotBlank()) {
                                            Text(
                                                text = formattedTimestamp,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = MediciTheme.colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .testTag("version_item_timestamp_${vPres.versionNumber}")
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MediciTheme.colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showVersionHistorySheet = false },
                    modifier = Modifier.testTag("close_version_history_button")
                ) {
                    Text(
                        text = stringResource(R.string.prescription_cancel),
                        color = MediciTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            containerColor = MediciTheme.colors.card,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Confirmation Dialog for Deleting Prescription
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.prescription_delete_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.prescription_delete_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDismiss()
                        onDeleteClick(prescription.id)
                    },
                    modifier = Modifier.testTag("confirm_delete_prescription_button")
                ) {
                    Text(
                        text = stringResource(R.string.prescription_delete),
                        color = MediciTheme.colors.warning,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_delete_prescription_button")
                ) {
                    Text(
                        text = stringResource(R.string.prescription_cancel),
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            containerColor = MediciTheme.colors.card,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Full-Screen Image Dialog
    if (showFullScreenImage && prescription.prescriptionImagePaths.isNotEmpty()) {
        val imageFiles = remember(prescription.prescriptionImagePaths) {
            prescription.prescriptionImagePaths.map { filename ->
                if (filename.startsWith("/")) File(filename)
                else File(context.filesDir, "prescription_images/$filename")
            }
        }
        FullScreenImageDialog(
            imageModels = imageFiles,
            initialIndex = fullScreenInitialIndex,
            onDismiss = { showFullScreenImage = false },
            title = stringResource(R.string.prescription_images_header)
        )
    }

    // Medicine Detail Bottom Sheet (Read-Only)
    val medForDetail = selectedMedicineForDetail
    if (medForDetail != null) {
        MedicineDetailBottomSheet(
            medicine = medForDetail.medicine,
            schedules = medForDetail.doseSchedules.map { it.doseSchedule },
            isTaken = false,
            isReadOnly = true,
            is24Hour = is24Hour,
            onDismiss = { selectedMedicineForDetail = null }
        )
    }
}

@Composable
private fun HeaderEditBadge(
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MediciTheme.colors.warning,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Text(
            text = stringResource(R.string.badge_edit),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MediciTheme.colors.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

