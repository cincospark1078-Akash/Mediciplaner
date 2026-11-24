package com.mediciplanner.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibleTimeInputDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = is24Hour
    )

    val largeInputTypography = MaterialTheme.typography.copy(
        displayMedium = MaterialTheme.typography.displayMedium.copy(
            fontSize = 44.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("time_input_dialog"),
        containerColor = MediciTheme.colors.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.time_input_dialog_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MediciTheme.colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
            ) {
                Text(
                    text = stringResource(R.string.time_input_dialog_helper),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MediciTheme.colors.textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                MaterialTheme(typography = largeInputTypography) {
                    TimeInput(
                        state = timePickerState,
                        modifier = Modifier.testTag("time_input_field"),
                        colors = TimePickerDefaults.colors(
                            timeSelectorSelectedContainerColor = MediciTheme.tokens.primaryContainer,
                            timeSelectorUnselectedContainerColor = MediciTheme.colors.surface,
                            timeSelectorSelectedContentColor = MediciTheme.tokens.primary,
                            timeSelectorUnselectedContentColor = MediciTheme.colors.textPrimary,
                            periodSelectorBorderColor = MediciTheme.tokens.outline,
                            periodSelectorSelectedContainerColor = MediciTheme.tokens.primaryContainer,
                            periodSelectorUnselectedContainerColor = MediciTheme.colors.card,
                            periodSelectorSelectedContentColor = MediciTheme.tokens.primary,
                            periodSelectorUnselectedContentColor = MediciTheme.colors.textPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("time_input_confirm_button")
            ) {
                Text(
                    text = stringResource(R.string.time_input_confirm),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MediciTheme.colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("time_input_cancel_button")
            ) {
                Text(
                    text = stringResource(R.string.time_input_cancel),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MediciTheme.colors.textSecondary
                )
            }
        }
    )
}
