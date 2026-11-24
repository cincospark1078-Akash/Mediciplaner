package com.mediciplanner.app.ui.screens.prescription.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine

@Composable
fun MedicineSwitcher(
    addedMedicines: List<DraftMedicine>,
    selectedMedicineIndex: Int?,
    currentDraftMedicine: DraftMedicine,
    onSelectMedicine: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (addedMedicines.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medicine_switcher"),
        shape = RoundedCornerShape(20.dp),
        color = MediciTheme.colors.primaryContainer.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MediciTheme.colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = MediciTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.medicine_switcher_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("medicine_switcher_label")
                )
                Text(
                    text = stringResource(
                        R.string.medicine_switcher_count,
                        toBengaliDigits(addedMedicines.size.toString())
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MediciTheme.colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("medicine_switcher_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                addedMedicines.forEachIndexed { index, med ->
                    val isSelected = selectedMedicineIndex == index
                    val rawName = if (isSelected && currentDraftMedicine.name.isNotBlank()) {
                        currentDraftMedicine.name
                    } else {
                        med.name
                    }
                    val displayName = rawName.ifBlank {
                        stringResource(
                            R.string.card2_medicine_default_name,
                            toBengaliDigits((index + 1).toString())
                        )
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectMedicine(index) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            null
                        },
                        label = {
                            Text(
                                text = displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MediciTheme.colors.primary,
                            selectedLabelColor = MediciTheme.colors.onPrimary,
                            selectedLeadingIconColor = MediciTheme.colors.onPrimary,
                            containerColor = MediciTheme.colors.card,
                            labelColor = MediciTheme.colors.textPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MediciTheme.colors.outlineVariant,
                            selectedBorderColor = MediciTheme.colors.primary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("medicine_switcher_chip_$index")
                    )
                }
            }
        }
    }
}
