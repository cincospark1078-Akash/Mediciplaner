package com.mediciplanner.app.ui.screens.prescription

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
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
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import com.mediciplanner.app.ui.components.BouncingExtendedFab
import com.mediciplanner.app.ui.components.EmptyStateIllustrationStyle
import com.mediciplanner.app.ui.components.EmptyStateView
import com.mediciplanner.app.ui.components.FrostedCard
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.components.PrescriptionDetailBottomSheet
import com.mediciplanner.app.ui.screens.prescription.components.formatBengaliPrescriptionDate

@Composable
fun PrescriptionListScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val app = context.applicationContext as MediciApplication
    val repository = app.prescriptionRepository

    val carouselViewModel: PrescriptionCarouselViewModel = viewModel(
        factory = PrescriptionCarouselViewModelFactory(
            repository = repository,
            reminderRepository = app.reminderRepository,
            reminderScheduler = app.reminderScheduler,
            themePreferences = app.themePreferences,
            ocrHelper = app.ocrHelper
        )
    )

    var showAddPrescriptionList by remember { mutableStateOf(false) }
    var selectedPrescriptionForDetail by remember { mutableStateOf<PrescriptionWithMedicines?>(null) }

    val prescriptionsWithMedicines by repository.allPrescriptionsWithMedicines.collectAsStateWithLifecycle(initialValue = emptyList())

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MediciTheme.colors.primaryContainer.copy(alpha = 0.45f),
            MediciTheme.colors.surface,
            MediciTheme.colors.surface
        )
    )

    if (showAddPrescriptionList) {
        AddPrescriptionListScreen(
            viewModel = carouselViewModel,
            onDismiss = {
                showAddPrescriptionList = false
                carouselViewModel.resetDraft()
            }
        )
    } else {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .statusBarsPadding()
                .testTag("prescription_list_screen"),
            containerColor = MediciTheme.colors.surface,
            floatingActionButton = {
                // Extended FAB at the bottom right with text "নতুন প্রেসক্রিপশন"
                // When tapped, resets DraftPrescription state in ViewModel to empty
                BouncingExtendedFab(
                    onClick = {
                        carouselViewModel.resetDraft()
                        showAddPrescriptionList = true
                    },
                    containerColor = MediciTheme.colors.primary,
                    contentColor = MediciTheme.colors.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_prescription_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.prescription_fab_new),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.prescription_fab_new),
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
                    .background(backgroundGradient)
                    .padding(innerPadding)
                    .padding(horizontal = MediciTheme.spacing.medium)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    },
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
            ) {
                // Screen Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MediciTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.extraSmall)
                ) {
                    Text(
                        text = stringResource(R.string.prescription_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.prescription_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (prescriptionsWithMedicines.isEmpty()) {
                    // Empty State
                    Box(modifier = Modifier.padding(top = MediciTheme.spacing.small)) {
                        EmptyStateView(
                            icon = Icons.Outlined.Medication,
                            title = stringResource(R.string.empty_prescription_title),
                            subtitle = stringResource(R.string.empty_prescription_desc),
                            illustrationStyle = EmptyStateIllustrationStyle.LINE_ART_RX,
                            actionLabel = stringResource(R.string.prescription_fab_new),
                            onActionClick = {
                                carouselViewModel.resetDraft()
                                showAddPrescriptionList = true
                            },
                            testTag = "prescription_empty_state"
                        )
                    }
                } else {
                    // LazyColumn showing Prescription Cards
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("prescriptions_list"),
                        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(prescriptionsWithMedicines, key = { it.prescription.id }) { item ->
                            PrescriptionCardItem(
                                item = item,
                                onClick = {
                                    selectedPrescriptionForDetail = item
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Prescription Details Bottom Sheet
    val detailItem = selectedPrescriptionForDetail
    if (detailItem != null) {
        PrescriptionDetailBottomSheet(
            prescriptionWithMedicines = detailItem,
            onDismiss = { selectedPrescriptionForDetail = null },
            onEditClick = { prescriptionToEdit ->
                selectedPrescriptionForDetail = null
                carouselViewModel.loadPrescriptionForEditing(prescriptionToEdit)
                showAddPrescriptionList = true
            },
            onDeleteClick = { prescriptionIdToDelete ->
                carouselViewModel.deletePrescription(prescriptionIdToDelete) {
                    selectedPrescriptionForDetail = null
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_prescription_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onAddNewMedicineClick = { prescriptionToEdit ->
                selectedPrescriptionForDetail = null
                carouselViewModel.loadPrescriptionForAddingMedicine(prescriptionToEdit)
                carouselViewModel.addEmptyDraftMedicine()
                showAddPrescriptionList = true
            },
            onEditMedicineClick = { prescriptionToEdit, medicineToEdit ->
                selectedPrescriptionForDetail = null
                carouselViewModel.loadPrescriptionForEditingMedicine(prescriptionToEdit, medicineToEdit.id)
                showAddPrescriptionList = true
            },
            onDeleteMedicineClick = { medicineToDelete ->
                carouselViewModel.deleteMedicine(medicineToDelete) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_medicine_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

/**
 * Item Design using FrostedCard as required:
 * - Date (Large, Bold, e.g., "১৫ নভেম্বর, ২০২৪")
 * - Doctor Name (if available, else "ডাক্তার উল্লেখ নেই")
 * - Medicine Count (e.g., "৩টি ওষুধ")
 * - Chevron Icon on the right.
 */
@Composable
fun PrescriptionCardItem(
    item: PrescriptionWithMedicines,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prescription = item.prescription
    val formattedDate = formatBengaliPrescriptionDate(prescription.date)
    val doctorName = if (!prescription.doctorName.isNullOrBlank()) {
        prescription.doctorName
    } else {
        stringResource(R.string.prescription_no_doctor)
    }
    val medCountStr = stringResource(R.string.prescription_medicines_count, toBengaliDigits(item.medicines.size.toString()))

    FrostedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("prescription_card_${prescription.id}"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = MediciTheme.colors.card,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calendar icon badge on left
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
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MediciTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Middle: Date (Large, Bold), Doctor Name, Medicine Count
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Date (Large, Bold, e.g., "১৫ নভেম্বর, ২০২৪")
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("prescription_item_date_${prescription.id}")
                )

                // Doctor Name (if available, else "ডাক্তার উল্লেখ নেই")
                Text(
                    text = doctorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("prescription_item_doctor_${prescription.id}")
                )

                // Medicine Count (e.g., "৩টি ওষুধ")
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .background(
                            color = MediciTheme.colors.primaryContainer.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = medCountStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MediciTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("prescription_item_med_count_${prescription.id}")
                    )
                }
            }

            // Chevron Icon on the right
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MediciTheme.colors.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.prescription_view_details),
                    tint = MediciTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
