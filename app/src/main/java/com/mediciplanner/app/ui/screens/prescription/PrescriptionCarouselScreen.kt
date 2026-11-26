package com.mediciplanner.app.ui.screens.prescription

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.DoseAmountHelper
import com.mediciplanner.app.ui.components.BouncingOutlinedButton
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.components.Card1BaseInfo
import com.mediciplanner.app.ui.screens.prescription.components.Card2MedicineDetails
import com.mediciplanner.app.ui.screens.prescription.components.Card3ScheduleDose
import com.mediciplanner.app.ui.screens.prescription.components.Card4DurationNotes
import com.mediciplanner.app.ui.screens.prescription.components.CarouselFooter
import kotlinx.coroutines.launch

@Composable
fun PrescriptionCarouselScreen(
    viewModel: PrescriptionCarouselViewModel,
    onDismiss: () -> Unit,
    initialPage: Int = 0,
    medicineId: String? = null,
    prescriptionId: String? = null,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val draftPrescription by viewModel.draftPrescription.collectAsStateWithLifecycle()
    val draftMedicine by viewModel.draftMedicine.collectAsStateWithLifecycle()
    val selectedMedicineIndex by viewModel.selectedMedicineIndex.collectAsStateWithLifecycle()
    val editingPrescriptionId by viewModel.editingPrescriptionId.collectAsStateWithLifecycle()
    val vmInitialPage by viewModel.initialPage.collectAsStateWithLifecycle()
    val isEditMode = editingPrescriptionId != null || prescriptionId != null
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val durationError by viewModel.durationError.collectAsStateWithLifecycle()
    val intervalError by viewModel.intervalError.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val extractedText by viewModel.extractedText.collectAsStateWithLifecycle()
    val parsedMedicines by viewModel.parsedMedicines.collectAsStateWithLifecycle()
    val isScanningOcr by viewModel.isScanningOcr.collectAsStateWithLifecycle()
    val showOcrReview by viewModel.showOcrReview.collectAsStateWithLifecycle()
    val reviewMedicines by viewModel.reviewMedicines.collectAsStateWithLifecycle()
    val ocrReviewErrorRes by viewModel.ocrReviewErrorRes.collectAsStateWithLifecycle()
    val pendingOcrImageUri by viewModel.pendingOcrImageUri.collectAsStateWithLifecycle()
    val timeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()
    val is24Hour = timeFormat == com.mediciplanner.app.core.data.preferences.ThemePreferences.TIME_FORMAT_24H

    androidx.compose.runtime.LaunchedEffect(medicineId) {
        if (medicineId != null && viewModel.editingMedicineId.value != medicineId) {
            viewModel.loadMedicineForEdit(medicineId)
        }
    }

    androidx.compose.runtime.LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    val resolvedInitialPage = when {
        medicineId != null -> 1
        (initialPage != 0 || vmInitialPage != 0) &&
            draftPrescription.date.isNullOrBlank() &&
            editingPrescriptionId == null -> 0
        initialPage != 0 -> initialPage
        else -> vmInitialPage
    }
    val pagerState = rememberPagerState(initialPage = resolvedInitialPage, pageCount = { 4 })
    val scope = rememberCoroutineScope()
    var showCard1Error by remember { mutableStateOf(false) }
    var showCard2Error by remember { mutableStateOf(false) }
    var showCard3Error by remember { mutableStateOf(false) }
    var showCard4Error by remember { mutableStateOf(false) }
    var showScanDatePrompt by remember { mutableStateOf(false) }

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
                viewModel.confirmAddAllReviewedMedicines {
                    if (viewModel.draftPrescription.value.date.isNullOrBlank()) {
                        showScanDatePrompt = true
                    }
                    scope.launch {
                        pagerState.animateScrollToPage(1)
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

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.clearErrorMessage()
    }

    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        viewModel.clearErrorMessage()
    }

    val currentDate = draftPrescription.date
    val isCard1Valid = currentDate != null && currentDate.isNotBlank()
    val isCard2Valid = draftMedicine.name.isNotBlank() && draftMedicine.type != null
    val isCard3Valid = draftMedicine.schedules.isNotEmpty() &&
            draftMedicine.schedules.all {
                DoseAmountHelper.parseDoseAmount(it.doseAmount) != null && it.doseUnit.isNotBlank()
            }

    // Dynamic Card 2 / medicine validation state as requested
    val isNextEnabled = draftMedicine.name.isNotBlank() &&
            draftMedicine.type != null &&
            draftMedicine.schedules.isNotEmpty() &&
            draftMedicine.schedules.all {
                DoseAmountHelper.parseDoseAmount(it.doseAmount) != null && it.doseUnit.isNotBlank()
            }

    // Next button click handler: validates current card via ViewModel, updates dynamic errorMessage
    val onNextClick: () -> Unit = {
        val currentPage = pagerState.currentPage
        if (viewModel.validateCard(currentPage)) {
            when (currentPage) {
                0 -> showCard1Error = false
                1 -> showCard2Error = false
                2 -> showCard3Error = false
                3 -> showCard4Error = false
            }
            if (currentPage < 3) {
                scope.launch {
                    pagerState.animateScrollToPage(currentPage + 1)
                }
            }
        } else {
            when (currentPage) {
                0 -> showCard1Error = true
                1 -> showCard2Error = true
                2 -> showCard3Error = true
                3 -> showCard4Error = true
            }
            val activeMsg = viewModel.errorMessage.value
            if (!activeMsg.isNullOrBlank()) {
                Toast.makeText(context, activeMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val currentStep = pagerState.currentPage + 1
    val animatedProgress by animateFloatAsState(
        targetValue = currentStep / 4f,
        animationSpec = tween(300),
        label = "carousel_progress"
    )

    // Main layout: Column(fillMaxSize().padding(16.dp)) -> HorizontalPager(weight(1f)) -> CarouselFooter()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediciTheme.colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 16.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .testTag("prescription_carousel_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Close, Title, Step counter badge (Page X/4), and dynamic scrollbar progress bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("carousel_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_carousel),
                        tint = MediciTheme.colors.textPrimary
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.carousel_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isEditMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MediciTheme.colors.warning,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("carousel_edit_mode_badge")
                        ) {
                            Text(
                                text = stringResource(R.string.carousel_edit_mode_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.onPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (pagerState.currentPage > 0) {
                        MediciTheme.colors.primaryContainer
                    } else {
                        MediciTheme.colors.surface
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(
                                R.string.carousel_step_bengali,
                                toBengaliDigits(currentStep.toString())
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = pagerState.currentPage > 0) {
                                    viewModel.clearErrorMessage()
                                    scope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("carousel_step_badge")
                        )
                        if (pagerState.currentPage > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        viewModel.clearErrorMessage()
                                        scope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("carousel_card1_shortcut")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.carousel_card1_shortcut_desc),
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.carousel_card1_shortcut),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MediciTheme.colors.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Progress bar at the top (Page 2/4) updating fill based on pagerState.currentPage
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("carousel_progress_bar"),
                color = MediciTheme.colors.primary,
                trackColor = MediciTheme.colors.primaryContainer.copy(alpha = 0.5f)
            )

            // Dynamic Error Message banner (updates based on missing field or save error)
            val activeError = errorMessage ?: saveError
            val dateRequiredMessage = stringResource(R.string.followup_error_date)
            val isMissingDateError = activeError == dateRequiredMessage &&
                pagerState.currentPage != 0 &&
                !isCard1Valid

            if (activeError != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MediciTheme.colors.warning.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MediciTheme.colors.warning.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("carousel_error_container")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = activeError,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MediciTheme.colors.warning,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("carousel_error_banner")
                        )

                        if (isMissingDateError) {
                            BouncingOutlinedButton(
                                onClick = {
                                    showCard1Error = true
                                    viewModel.clearErrorMessage()
                                    scope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MediciTheme.colors.warning
                                ),
                                border = BorderStroke(1.dp, MediciTheme.colors.warning),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("go_to_card1_from_error_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.error_go_to_card1_for_date),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else if (showScanDatePrompt && !isCard1Valid && pagerState.currentPage != 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MediciTheme.colors.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            showCard1Error = true
                            scope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                        .testTag("scan_date_prompt_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.scan_prompt_set_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MediciTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.scan_prompt_go_card1),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Horizontal Pager taking weight(1f) to eliminate awkward empty gaps
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .testTag("prescription_carousel_pager")
        ) { page ->
            when (page) {
                0 -> {
                    Card1BaseInfo(
                        draftPrescription = draftPrescription,
                        onDateChanged = {
                            showCard1Error = false
                            showScanDatePrompt = false
                            viewModel.updateDate(it)
                        },
                        onDoctorNameChanged = { viewModel.updateDoctorName(it) },
                        onLocationChanged = { viewModel.updateLocation(it) },
                        onImagesAdded = { paths ->
                            viewModel.addPrescriptionImagePaths(paths)
                            if (paths.isNotEmpty()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.msg_image_added_removed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onImageRemoved = { path ->
                            viewModel.removePrescriptionImagePath(path)
                            Toast.makeText(
                                context,
                                context.getString(R.string.msg_image_added_removed),
                                Toast.LENGTH_SHORT
                            ).show()
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
                            if (viewModel.draftPrescription.value.date.isNullOrBlank()) {
                                showScanDatePrompt = true
                            }
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        showError = showCard1Error
                    )
                }
                1 -> {
                    Card2MedicineDetails(
                        draftMedicine = draftMedicine,
                        addedMedicines = draftPrescription.medicines,
                        selectedMedicineIndex = selectedMedicineIndex,
                        onSelectMedicine = { viewModel.selectMedicineForEditing(it) },
                        onDeleteMedicine = { viewModel.removeDraftMedicineAt(it) },
                        onNameChanged = { viewModel.updateMedicineName(it) },
                        onTypeChanged = {
                            viewModel.updateMedicineType(it)
                            viewModel.clearStrengthUnit()
                        },
                        onStrengthChanged = { viewModel.updateStrength(it) },
                        onStrengthUnitChanged = { viewModel.updateStrengthUnit(it) },
                        onImagePathChanged = { path ->
                            viewModel.updateMedicineImagePath(path)
                            Toast.makeText(
                                context,
                                context.getString(R.string.msg_image_added_removed),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        showError = showCard2Error
                    )
                }
                2 -> {
                    Card3ScheduleDose(
                        draftMedicine = draftMedicine,
                        onToggleSlot = {
                            showCard3Error = false
                            viewModel.toggleSlot(it)
                        },
                        onToggleExpanded = { viewModel.toggleScheduleExpanded(it) },
                        onTimeChanged = { slot, time -> viewModel.updateScheduleTime(slot, time) },
                        onAmountChanged = { slot, amt -> viewModel.updateScheduleDoseAmount(slot, amt) },
                        onUnitChanged = { slot, unit -> viewModel.updateScheduleDoseUnit(slot, unit) },
                        onSlotFoodRuleChanged = { slotId, rule -> viewModel.updateSlotFoodRule(slotId, rule) },
                        addedMedicines = draftPrescription.medicines,
                        selectedMedicineIndex = selectedMedicineIndex,
                        onSelectMedicine = {
                            showCard2Error = false
                            showCard3Error = false
                            showCard4Error = false
                            viewModel.selectMedicineForEditing(it)
                        },
                        showError = showCard3Error,
                        is24Hour = is24Hour
                    )
                }
                3 -> {
                    val totalMedicineSummary = remember(
                        draftMedicine.durationDays,
                        draftMedicine.intervalDays,
                        draftMedicine.schedules
                    ) {
                        val duration = draftMedicine.durationDays.toIntOrNull() ?: 0
                        viewModel.calculateTotalMedicine(
                            duration = duration,
                            interval = draftMedicine.intervalDays,
                            schedules = draftMedicine.schedules
                        )
                    }
                    Card4DurationNotes(
                        draftMedicine = draftMedicine,
                        draftPrescription = draftPrescription,
                        onDurationChanged = { viewModel.updateDurationDays(it) },
                        onIncrementDuration = { viewModel.incrementDuration() },
                        onDecrementDuration = { viewModel.decrementDuration() },
                        onIntervalChanged = { viewModel.updateIntervalDays(it) },
                        onNoteChanged = { viewModel.updateMedicineNote(it) },
                        onStockCountChanged = { viewModel.updateStockCount(it) },
                        onIncrementStock = { viewModel.incrementStockCount() },
                        onDecrementStock = { viewModel.decrementStockCount() },
                        selectedMedicineIndex = selectedMedicineIndex,
                        onSelectMedicine = {
                            showCard2Error = false
                            showCard3Error = false
                            showCard4Error = false
                            viewModel.selectMedicineForEditing(it)
                        },
                        totalMedicineSummary = totalMedicineSummary,
                        showError = showCard4Error,
                        durationError = durationError,
                        intervalError = intervalError
                    )
                }
            }
        }

        // CarouselFooter placed at the bottom of the column
        val hasNextMedicine = draftPrescription.medicines.size > 1 &&
            selectedMedicineIndex != null &&
            selectedMedicineIndex in 0 until draftPrescription.medicines.lastIndex

        CarouselFooter(
            currentPage = pagerState.currentPage,
            onPreviousClick = {
                if (pagerState.currentPage > 0) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            },
            onNextClick = onNextClick,
            onAddMoreClick = {
                if (viewModel.validateCard(3)) {
                    viewModel.onAddMoreClicked(
                        onNavigateToCard2 = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                } else {
                    showCard4Error = true
                    val activeMsg = viewModel.errorMessage.value
                    if (!activeMsg.isNullOrBlank()) {
                        Toast.makeText(context, activeMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onReviewAndSaveClick = {
                if (viewModel.validateCard(3)) {
                    showCard4Error = false
                    viewModel.reviewAndSave(
                        onSuccess = onDismiss,
                        onNoChanges = {
                            viewModel.clearToastMessage()
                            Toast.makeText(context, context.getString(R.string.prescription_no_changes), Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        onSuccessWithType = { saveType ->
                            val msgRes = when (saveType) {
                                PrescriptionSaveType.SAVE_NEW -> R.string.msg_prescription_saved_with_dest
                                PrescriptionSaveType.EDIT_PRESCRIPTION -> R.string.msg_prescription_updated_with_dest
                                PrescriptionSaveType.ADD_MEDICINE -> R.string.msg_medicine_added_with_dest
                                PrescriptionSaveType.EDIT_MEDICINE -> R.string.msg_medicine_updated_with_dest
                            }
                            Toast.makeText(
                                context,
                                context.getString(msgRes),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                } else {
                    showCard4Error = true
                    val activeMsg = viewModel.errorMessage.value
                    if (!activeMsg.isNullOrBlank()) {
                        Toast.makeText(context, activeMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            isNextEnabled = isNextEnabled,
            isSaving = isSaving,
            isCard1Valid = isCard1Valid,
            isCard2Valid = isCard2Valid,
            isCard3Valid = isCard3Valid,
            hasNextMedicine = hasNextMedicine,
            onNextMedicineClick = {
                showCard4Error = false
                viewModel.goToNextMedicine()
            },
            pagerState = pagerState,
            scope = scope,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
        )
    }
}
