package com.mediciplanner.app.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.SlotActivationHelper
import com.mediciplanner.app.core.util.formatTime
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.ui.components.BouncingButton
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotResId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NextReminderCard(
    reminder: ReminderEntity?,
    medicine: MedicineEntity?,
    schedule: DoseScheduleEntity?,
    hasMedicinesToday: Boolean = true,
    onMarkAsTaken: (String) -> Unit,
    onNavigateToTrack: () -> Unit = {},
    is24Hour: Boolean = false,
    isSlotActive: Boolean = SlotActivationHelper.isSlotActive(schedule?.time),
    modifier: Modifier = Modifier
) {
    if (reminder == null || medicine == null || schedule == null) {
        // Only show success state if there were medicines today
        if (!hasMedicinesToday) return

        // === 2. EMPTY STATE (When All Doses are Taken) ===
        // Design: A Mint Green (MediciTheme.colors.success with 0.15f alpha) background card
        // Content: CheckCircle icon (Mint Green) + "আজকের সব ডোজ সম্পন্ন!" + "আপনি সময়মতো সব ওষুধ খেয়েছেন।"
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 },
            exit = fadeOut(tween(300))
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("next_reminder_empty_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MediciTheme.colors.success.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MediciTheme.colors.success.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MediciTheme.colors.success,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.empty_state_c_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.empty_state_c_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    BouncingButton(
                        onClick = onNavigateToTrack,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MediciTheme.colors.success.copy(alpha = 0.25f),
                            contentColor = MediciTheme.colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("track_history_button")
                    ) {
                        Text(
                            text = stringResource(R.string.empty_state_c_cta),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } else {
        // === 1. NEXT REMINDER CARD DESIGN ===
        var isMarkedCompleted by remember(reminder.id) { mutableStateOf(false) }
        var isCardVisible by remember(reminder.id) { mutableStateOf(true) }

        val coroutineScope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val slotLabel = stringResource(getDoseSlotResId(schedule.slot))
        val formattedTime = "$slotLabel ${toBengaliDigits(formatTime(schedule.time, is24Hour))}"

        // Background: Soft gradient using MediciTheme.colors.primary and MediciTheme.colors.primaryContainer
        val cardGradient = Brush.linearGradient(
            colors = listOf(
                MediciTheme.colors.primary,
                MediciTheme.colors.primaryContainer
            )
        )

        AnimatedVisibility(
            visible = isCardVisible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 3 }
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("next_reminder_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MediciTheme.colors.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardGradient)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top row: Small label "পরবর্তী ডোজ" in semi-transparent pill + Time pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Label: "পরবর্তী ডোজ" using semi-transparent pill shape
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MediciTheme.colors.onPrimary.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.next_dose_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.onPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Time chip in subtle semi-transparent pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(
                                        color = MediciTheme.colors.onPrimary.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.onPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Middle: Medicine Name (High contrast White text) and Strength
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = medicine.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MediciTheme.colors.onPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val strengthVal = medicine.strength
                            val strengthUnit = medicine.strengthUnit
                            val strengthText = if (strengthVal != null && !strengthUnit.isNullOrBlank()) {
                                "${toBengaliDigits(strengthVal.toString().removeSuffix(".0"))} $strengthUnit"
                            } else ""

                            if (strengthText.isNotBlank()) {
                                Text(
                                    text = strengthText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MediciTheme.colors.onPrimary.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Bottom: CTA Button "গ্রহণ করেছি"
                        val buttonTextColor = if (isSlotActive) {
                            MediciTheme.colors.primary
                        } else {
                            MediciTheme.colors.textSecondary.copy(alpha = 0.7f)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BouncingButton(
                                onClick = {
                                    if (!isMarkedCompleted && isSlotActive) {
                                        isMarkedCompleted = true
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                        coroutineScope.launch {
                                            // Animate smoothly out before triggering state update
                                            isCardVisible = false
                                            delay(250)
                                            onMarkAsTaken(reminder.id)
                                        }
                                    }
                                },
                                enabled = isSlotActive && !isMarkedCompleted,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MediciTheme.colors.cardBackground,
                                    contentColor = MediciTheme.colors.primary,
                                    disabledContainerColor = MediciTheme.colors.surface.copy(alpha = 0.55f),
                                    disabledContentColor = MediciTheme.colors.textSecondary.copy(alpha = 0.7f)
                                ),
                                shadowElevation = if (isSlotActive) 3.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("mark_as_taken_button")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = buttonTextColor,
                                        modifier = Modifier
                                            .padding(end = MediciTheme.spacing.small)
                                            .size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_mark_taken),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = buttonTextColor,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!isSlotActive) {
                                Text(
                                    text = stringResource(R.string.slot_not_time_yet),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MediciTheme.colors.onPrimary.copy(alpha = 0.9f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("next_reminder_not_time_yet")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
