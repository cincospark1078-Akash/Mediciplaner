package com.mediciplanner.app.ui.screens.prescription.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.ParsedMedicine

@Composable
fun OcrPreviewCard(
    extractedText: String,
    parsedMedicines: List<ParsedMedicine> = emptyList(),
    onAddMedicineFromText: () -> Unit,
    onAddParsedMedicine: (ParsedMedicine) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFullTextExpanded by remember(extractedText) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ocr_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MediciTheme.colors.primaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MediciTheme.colors.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = stringResource(R.string.ocr_scanned_text_title),
                    tint = MediciTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.ocr_scanned_text_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = extractedText,
                style = MaterialTheme.typography.bodySmall,
                color = MediciTheme.colors.textSecondary,
                maxLines = if (isFullTextExpanded) 50 else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ocr_extracted_text_body")
            )

            OutlinedButton(
                onClick = { isFullTextExpanded = !isFullTextExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("ocr_view_full_text_button"),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MediciTheme.colors.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MediciTheme.colors.primary
                )
            ) {
                Icon(
                    imageVector = if (isFullTextExpanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = stringResource(
                        if (isFullTextExpanded) {
                            R.string.ocr_hide_full_text
                        } else {
                            R.string.ocr_view_full_text
                        }
                    ),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(MediciTheme.spacing.extraSmall))
                Text(
                    text = stringResource(
                        if (isFullTextExpanded) {
                            R.string.ocr_hide_full_text
                        } else {
                            R.string.ocr_view_full_text
                        }
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onAddMedicineFromText,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("ocr_add_medicine_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediciTheme.colors.primary,
                    contentColor = MediciTheme.colors.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = stringResource(R.string.ocr_add_medicine_from_text),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(MediciTheme.spacing.small))
                Text(
                    text = stringResource(R.string.ocr_add_medicine_from_text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (parsedMedicines.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = MediciTheme.spacing.extraSmall),
                    color = MediciTheme.colors.outline.copy(alpha = 0.4f)
                )

                Text(
                    text = stringResource(R.string.ocr_detected_medicines_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("ocr_detected_medicines_header")
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ocr_parsed_medicines_section"),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    parsedMedicines.forEachIndexed { index, parsedMedicine ->
                        val summaryLine = buildString {
                            append(parsedMedicine.name)
                            if (!parsedMedicine.dose.isNullOrBlank()) {
                                append(" - ${parsedMedicine.dose}")
                            }
                            if (!parsedMedicine.duration.isNullOrBlank()) {
                                append(" - ${parsedMedicine.duration}")
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_parsed_medicine_item_$index"),
                            shape = RoundedCornerShape(20.dp),
                            color = MediciTheme.colors.card,
                            border = BorderStroke(1.dp, MediciTheme.colors.outline.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MediciTheme.spacing.small),
                                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = parsedMedicine.name,
                                        tint = MediciTheme.colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = summaryLine,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MediciTheme.colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ocr_parsed_medicine_text_$index")
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onAddParsedMedicine(parsedMedicine) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("ocr_add_to_prescription_button_$index"),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, MediciTheme.colors.primary),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MediciTheme.colors.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.ocr_add_to_prescription),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(MediciTheme.spacing.extraSmall))
                                    Text(
                                        text = stringResource(R.string.ocr_add_to_prescription),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
