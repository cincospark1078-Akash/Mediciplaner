package com.mediciplanner.app.ui.screens.followup

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.mediciplanner.app.core.util.formatTime
import com.mediciplanner.app.ui.components.BouncingExtendedFab
import com.mediciplanner.app.ui.components.BouncingOutlinedButton
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.ui.components.EmptyStateIllustrationStyle
import com.mediciplanner.app.ui.components.FrostedCard
import com.mediciplanner.app.ui.components.FullScreenImageDialog
import com.mediciplanner.app.ui.components.IllustratedEmptyState
import com.mediciplanner.app.ui.components.StaggeredEntryItem
import java.io.File
import com.mediciplanner.app.ui.screens.home.formatBengaliDate
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun parseAndFormatDate(dateStr: String): String {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowupScreen(
    modifier: Modifier = Modifier,
    followUpViewModel: FollowUpViewModel = run {
        val app = LocalContext.current.applicationContext as MediciApplication
        viewModel(
            factory = FollowUpViewModelFactory(
                followUpRepository = app.followUpRepository,
                reminderScheduler = app.reminderScheduler,
                themePreferences = app.themePreferences
            )
        )
    }
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val uiState by followUpViewModel.uiState.collectAsStateWithLifecycle()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedFollowUpForDetails by remember { mutableStateOf<FollowUpEntity?>(null) }
    var viewingDocuments by remember { mutableStateOf<List<String>?>(null) }
    var viewingDocumentIndex by remember { mutableIntStateOf(0) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MediciTheme.colors.primaryContainer.copy(alpha = 0.45f),
            MediciTheme.colors.surface,
            MediciTheme.colors.surface
        )
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .testTag("followup_screen"),
        containerColor = MediciTheme.colors.surface.copy(alpha = 0f),
        floatingActionButton = {
            // FAB positioned safely above bottom navigation bar
            BouncingExtendedFab(
                onClick = {
                    followUpViewModel.resetDrafts()
                    showBottomSheet = true
                },
                containerColor = MediciTheme.colors.primary,
                contentColor = MediciTheme.colors.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("add_followup_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.followup_add_action),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.followup_add_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = MediciTheme.spacing.medium)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // 1. Screen Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MediciTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
            ) {
                Text(
                    text = stringResource(R.string.followup_header_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.followup_header_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 1.5. Incomplete Draft Section (shown when hasDraft == true)
            if (uiState.hasDraft) {
                FollowUpDraftBannerCard(
                    onClick = { showBottomSheet = true }
                )
            }

            // 2. TabRow: "আসন্ন" (Upcoming) and "ইতিহাস" (History)
            TabRow(
                selectedTabIndex = if (uiState.selectedTab == FollowUpTab.UPCOMING) 0 else 1,
                containerColor = MediciTheme.colors.surface,
                contentColor = MediciTheme.colors.primary,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty()) {
                        val currentPos = if (uiState.selectedTab == FollowUpTab.UPCOMING) tabPositions[0] else tabPositions[1]
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(currentPos),
                            color = MediciTheme.colors.primary,
                            height = 3.dp
                        )
                    }
                },
                divider = {
                    HorizontalDivider(
                        color = MediciTheme.colors.outlineVariant,
                        thickness = 1.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("followup_tabs")
            ) {
                Tab(
                    selected = uiState.selectedTab == FollowUpTab.UPCOMING,
                    onClick = { followUpViewModel.selectTab(FollowUpTab.UPCOMING) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.followup_tab_upcoming),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (uiState.selectedTab == FollowUpTab.UPCOMING) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.selectedTab == FollowUpTab.UPCOMING) MediciTheme.colors.primary else MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.upcomingFollowUps.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (uiState.selectedTab == FollowUpTab.UPCOMING) MediciTheme.colors.primary else MediciTheme.colors.primaryContainer,
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = toBengaliDigits(uiState.upcomingFollowUps.size.toString()),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.selectedTab == FollowUpTab.UPCOMING) MediciTheme.colors.onPrimary else MediciTheme.colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("tab_upcoming")
                )

                Tab(
                    selected = uiState.selectedTab == FollowUpTab.HISTORY,
                    onClick = { followUpViewModel.selectTab(FollowUpTab.HISTORY) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.followup_tab_history),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (uiState.selectedTab == FollowUpTab.HISTORY) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.selectedTab == FollowUpTab.HISTORY) MediciTheme.colors.primary else MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.historyFollowUps.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (uiState.selectedTab == FollowUpTab.HISTORY) MediciTheme.colors.primary else MediciTheme.colors.primaryContainer,
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = toBengaliDigits(uiState.historyFollowUps.size.toString()),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.selectedTab == FollowUpTab.HISTORY) MediciTheme.colors.onPrimary else MediciTheme.colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("tab_history")
                )
            }

            // 3. Content List or Empty State depending on selected tab
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MediciTheme.colors.primary,
                        strokeWidth = 3.dp
                    )
                }
            } else if (uiState.selectedTab == FollowUpTab.UPCOMING) {
                // Upcoming List
                if (uiState.upcomingFollowUps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = MediciTheme.spacing.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        IllustratedEmptyState(
                            icon = Icons.Default.CalendarToday,
                            title = stringResource(R.string.empty_followup_upcoming_title),
                            subtitle = stringResource(R.string.empty_followup_upcoming_desc),
                            illustrationStyle = EmptyStateIllustrationStyle.PLANT_WELLNESS,
                            testTag = "followup_empty_state"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                    ) {
                        itemsIndexed(
                            items = uiState.upcomingFollowUps,
                            key = { _, it -> it.id }
                        ) { index, followUp ->
                            StaggeredEntryItem(index = index) {
                                UpcomingFollowUpCardItem(
                                    followUp = followUp,
                                    is24Hour = uiState.is24Hour,
                                    onClick = {
                                        selectedFollowUpForDetails = followUp
                                    },
                                    onMarkCompleted = {
                                        followUpViewModel.markAsCompleted(followUp) {
                                            val msgRes = when (followUp.type) {
                                                FollowUpType.DOCTOR_VISIT -> R.string.msg_followup_doctor_completed_with_dest
                                                FollowUpType.TEST -> R.string.msg_followup_test_completed_with_dest
                                            }
                                            Toast.makeText(
                                                context,
                                                context.getString(msgRes),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onViewDocuments = { docs ->
                                        viewingDocuments = docs
                                        viewingDocumentIndex = 0
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            } else {
                // History List (Read-only cards with Mint Green Checkmark & Completion Date)
                if (uiState.historyFollowUps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = MediciTheme.spacing.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        IllustratedEmptyState(
                            icon = Icons.Default.History,
                            title = stringResource(R.string.empty_followup_history_title),
                            subtitle = stringResource(R.string.empty_followup_history_desc),
                            illustrationStyle = EmptyStateIllustrationStyle.PLANT_WELLNESS,
                            testTag = "followup_history_empty_state"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                    ) {
                        itemsIndexed(
                            items = uiState.historyFollowUps,
                            key = { _, it -> it.id }
                        ) { index, followUp ->
                            StaggeredEntryItem(index = index) {
                                HistoryFollowUpCardItem(
                                    followUp = followUp,
                                    onClick = {
                                        selectedFollowUpForDetails = followUp
                                    },
                                    onViewDocuments = { docs ->
                                        viewingDocuments = docs
                                        viewingDocumentIndex = 0
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }

    // 4. FOLLOW-UP DETAILS BOTTOM SHEET
    val activeDetailsFollowUp = selectedFollowUpForDetails?.let { selected ->
        uiState.followUps.find { it.id == selected.id } ?: selected
    }
    if (activeDetailsFollowUp != null) {
        FollowUpDetailsBottomSheet(
            followUp = activeDetailsFollowUp,
            is24Hour = uiState.is24Hour,
            onDismiss = { selectedFollowUpForDetails = null },
            onEditClick = { target ->
                selectedFollowUpForDetails = null
                followUpViewModel.loadFollowUpForEdit(target)
                showBottomSheet = true
            },
            onDeleteConfirm = { target ->
                followUpViewModel.deleteFollowUp(target) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_followup_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                selectedFollowUpForDetails = null
            }
        )
    }

    // 5. ADD / EDIT SMART MODAL BOTTOM SHEET
    if (showBottomSheet) {
        FollowUpAddEditSheet(
            viewModel = followUpViewModel,
            onDismiss = {
                showBottomSheet = false
            },
            onDelete = { followUpToDelete ->
                followUpViewModel.deleteFollowUp(followUpToDelete) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_followup_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                followUpViewModel.clearDraft()
                showBottomSheet = false
            }
        )
    }

    // 5. Full-Screen Document Viewer Dialog
    if (!viewingDocuments.isNullOrEmpty()) {
        val docFiles = remember(viewingDocuments) {
            viewingDocuments!!.map { filename ->
                if (filename.startsWith("/")) File(filename)
                else {
                    val f1 = File(context.filesDir, "followup_documents/$filename")
                    if (f1.exists()) f1 else File(context.filesDir, "prescription_images/$filename")
                }
            }
        }
        FullScreenImageDialog(
            imageModels = docFiles,
            initialIndex = viewingDocumentIndex,
            onDismiss = { viewingDocuments = null },
            title = stringResource(R.string.followup_documents_label)
        )
    }
}

/**
 * Small Draft Card displayed when an incomplete follow-up draft exists in SavedStateHandle.
 * Clicking it re-opens the Add/Edit sheet with the draft data loaded.
 */
@Composable
private fun FollowUpDraftBannerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("followup_draft_card"),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MediciTheme.colors.primaryContainer.copy(alpha = 0.65f),
        borderColor = MediciTheme.colors.primary.copy(alpha = 0.35f),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediciTheme.spacing.medium, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MediciTheme.colors.primary.copy(alpha = 0.14f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.EditNote,
                    contentDescription = stringResource(R.string.followup_draft_card_title),
                    tint = MediciTheme.colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.followup_draft_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.followup_draft_card_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.followup_draft_card_subtitle),
                tint = MediciTheme.colors.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Upcoming Follow-Up Card:
 * - FrostedCard with 20.dp radius and elevation
 * - Left: Circular primaryContainer background with MedicalServices (for Doctor Visit) or Science (for Test)
 * - Middle: Title, Location, Date, Custom Reminder Time, Note, Reminder badge
 * - "সম্পন্ন করুন" (Mark as Completed) action button with minimum touch target of 48dp
 * - Tapping card opens Edit dialog
 */
@Composable
private fun UpcomingFollowUpCardItem(
    followUp: FollowUpEntity,
    is24Hour: Boolean = false,
    onClick: () -> Unit,
    onMarkCompleted: () -> Unit,
    onViewDocuments: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDoctor = followUp.type == FollowUpType.DOCTOR_VISIT
    val iconVector = if (isDoctor) Icons.Default.MedicalServices else Icons.Default.Science

    FrostedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("followup_item_${followUp.id}"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = MediciTheme.colors.card.copy(alpha = 0.9f),
        borderColor = MediciTheme.colors.outlineVariant.copy(alpha = 0.5f),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
            ) {
                // Left Circular Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MediciTheme.colors.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = MediciTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Middle: Title, Location, Date, Time, Note, Reminder
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Labeled Title (Doctor Name or Test Name)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDoctor) Icons.Default.Person else Icons.Default.Science,
                            contentDescription = null,
                            tint = MediciTheme.colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(
                                if (isDoctor) R.string.followup_card_label_doctor else R.string.followup_card_label_test
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = followUp.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    // Labeled Location (Clinic or Test Center)
                    if (!followUp.location.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isDoctor) Icons.Default.LocalHospital else Icons.Default.Place,
                                contentDescription = null,
                                tint = MediciTheme.colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isDoctor) R.string.followup_card_label_clinic else R.string.followup_card_label_center
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1
                            )
                            Text(
                                text = followUp.location,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MediciTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }

                    // Scheduled Date and Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MediciTheme.colors.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = parseAndFormatDate(followUp.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MediciTheme.colors.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = formatDisplayTime(followUp.reminderTime, is24Hour),
                                style = MaterialTheme.typography.bodySmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Note
                    if (!followUp.note.isNullOrBlank()) {
                        Text(
                            text = followUp.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Attached Documents Badge
                    if (followUp.documentPaths.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MediciTheme.colors.primaryContainer.copy(alpha = 0.5f))
                                .clickable { onViewDocuments(followUp.documentPaths) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("followup_documents_button_${followUp.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = stringResource(R.string.followup_documents_label),
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.followup_documents_count, followUp.documentPaths.size),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MediciTheme.colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Reminder Active Badge
                    if (followUp.reminderEnabled) {
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${stringResource(R.string.followup_reminder_on)} (${formatDisplayTime(followUp.reminderTime, is24Hour)})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MediciTheme.colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Right Chevron
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MediciTheme.colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Bottom Action: "সম্পন্ন করুন" (Mark as Completed) Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                BouncingOutlinedButton(
                    onClick = onMarkCompleted,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MediciTheme.colors.success
                    ),
                    border = BorderStroke(1.dp, MediciTheme.colors.success),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("complete_followup_button_${followUp.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MediciTheme.colors.success,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.followup_action_mark_completed),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.success,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Read-only History Card:
 * - FrostedCard with 20.dp radius
 * - Left: Circular mint green container with Mint Green Checkmark icon (Icons.Default.CheckCircle)
 * - Middle: Title, Location, Scheduled Date + Time, Completion Date (in Mint Green), and Note
 * - Completed badge ("সম্পন্ন ✓")
 */
@Composable
private fun HistoryFollowUpCardItem(
    followUp: FollowUpEntity,
    onClick: () -> Unit = {},
    onViewDocuments: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDoctor = followUp.type == FollowUpType.DOCTOR_VISIT

    FrostedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${followUp.id}"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = MediciTheme.colors.card.copy(alpha = 0.85f),
        borderColor = MediciTheme.colors.outlineVariant.copy(alpha = 0.4f),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // Left Mint Green Checkmark Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MediciTheme.colors.success.copy(alpha = 0.14f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.followup_status_completed),
                    tint = MediciTheme.colors.success,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Middle: Title, Completion date, location, scheduled date
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDoctor) Icons.Default.Person else Icons.Default.Science,
                            contentDescription = null,
                            tint = MediciTheme.colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(
                                if (isDoctor) R.string.followup_card_label_doctor else R.string.followup_card_label_test
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = followUp.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Mint Green Completed Badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = MediciTheme.colors.success.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.followup_status_completed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.success,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Location if present
                if (!followUp.location.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDoctor) Icons.Default.LocalHospital else Icons.Default.Place,
                            contentDescription = null,
                            tint = MediciTheme.colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(
                                if (isDoctor) R.string.followup_card_label_clinic else R.string.followup_card_label_center
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = followUp.location,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MediciTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                // Scheduled Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MediciTheme.colors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = parseAndFormatDate(followUp.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Completion Date display
                val completionDateText = parseAndFormatDate(followUp.completedAt ?: followUp.date)
                Text(
                    text = stringResource(R.string.followup_completed_on, completionDateText),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MediciTheme.colors.success,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Attached Documents Badge
                if (followUp.documentPaths.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MediciTheme.colors.primaryContainer.copy(alpha = 0.5f))
                            .clickable { onViewDocuments(followUp.documentPaths) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("history_documents_button_${followUp.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = stringResource(R.string.followup_documents_label),
                            tint = MediciTheme.colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.followup_documents_count, followUp.documentPaths.size),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MediciTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Note if present
                if (!followUp.note.isNullOrBlank()) {
                    Text(
                        text = followUp.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
