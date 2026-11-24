package com.mediciplanner.app.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import com.mediciplanner.app.core.util.SlotActivationHelper
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.ui.components.BouncingButton
import com.mediciplanner.app.ui.components.FrostedCard
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.ChangeType
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotResId
import com.mediciplanner.app.ui.screens.prescription.getFoodRuleResId
import com.mediciplanner.app.ui.screens.prescription.getMedicineTypeResId
import kotlinx.coroutines.delay

@Composable
fun MedicineCard(
    medicine: MedicineEntity,
    schedule: DoseScheduleEntity?,
    isTaken: Boolean,
    slotName: String? = null,
    staggerIndex: Int = 0,
    isReadOnly: Boolean = false,
    changeType: ChangeType? = null,
    isSlotActive: Boolean = SlotActivationHelper.isSlotActive(schedule?.time),
    showTakeButton: Boolean = !isReadOnly,
    onMarkAsTaken: () -> Unit = {},
    onClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isEntered by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(staggerIndex) {
        delay(staggerIndex * 50L)
        isEntered = true
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )

    // Resolve localized slot title (e.g., "সকাল" or "দুপুর")
    val parsedSlotEnum = if (!slotName.isNullOrBlank()) {
        try {
            DoseSlot.valueOf(slotName)
        } catch (e: Exception) {
            null
        }
    } else {
        schedule?.slot
    }

    val slotTitle = when {
        parsedSlotEnum != null -> stringResource(getDoseSlotResId(parsedSlotEnum))
        !slotName.isNullOrBlank() -> slotName
        else -> ""
    }

    // Resolve strength (e.g. "৫০০ mg")
    val strengthText = if (medicine.strength != null && !medicine.strengthUnit.isNullOrBlank()) {
        val cleanVal = toBengaliDigits(medicine.strength.toString().removeSuffix(".0"))
        "$cleanVal ${medicine.strengthUnit}"
    } else null

    // Resolve dose amount and food rule
    val foodRuleRes = getFoodRuleResId(schedule?.foodRuleOverride ?: schedule?.foodRule ?: medicine.foodRule)
    val foodRuleText = stringResource(foodRuleRes)
    val defaultUnitTi = stringResource(R.string.unit_ti)
    val doseAmountText = if (schedule != null) {
        val rawAmt = toBengaliDigits(schedule.doseAmount.toString().removeSuffix(".0"))
        val unitStr = if (schedule.doseUnit.isNotBlank()) schedule.doseUnit else defaultUnitTi
        "$rawAmt $unitStr"
    } else {
        "${toBengaliDigits("1")} $defaultUnitTi"
    }

    AnimatedVisibility(
        visible = isEntered,
        enter = fadeIn(animationSpec = tween(durationMillis = 400)) +
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                )
    ) {
        // Uniform height card with RoundedCornerShape(20.dp) and FrostedCard styling
        FrostedCard(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .defaultMinSize(minHeight = 110.dp)
                .scale(cardScale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .testTag("medicine_card_${medicine.id}"),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = MediciTheme.colors.card,
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 110.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // === 1. HEADER ROW: Medicine Name and Strength on the SAME LINE ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Inline ChangeType badge ("New"/"Edit") + Name (Bold, 18.sp) + Strength (textSecondary, 14.sp) and Type (14.sp) below
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (changeType == ChangeType.NEW || changeType == ChangeType.EDITED) {
                                val badgeText = if (changeType == ChangeType.NEW) {
                                    stringResource(R.string.badge_new)
                                } else {
                                    stringResource(R.string.badge_edit)
                                }
                                val badgeColor = if (changeType == ChangeType.NEW) {
                                    MediciTheme.colors.success
                                } else {
                                    MediciTheme.colors.warning
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = badgeColor,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .testTag("medicine_change_badge_${medicine.id}")
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MediciTheme.colors.onPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("medicine_change_badge_text_${medicine.id}")
                                    )
                                }
                            }

                            Text(
                                text = medicine.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MediciTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .testTag("medicine_card_name_${medicine.id}")
                            )
                            if (!strengthText.isNullOrBlank()) {
                                Text(
                                    text = strengthText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = MediciTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("medicine_card_strength_${medicine.id}")
                                )
                            }
                        }

                        // Type (14.sp)
                        Text(
                            text = stringResource(getMedicineTypeResId(medicine.type)),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_card_type_${medicine.id}")
                        )
                    }

                    // Right Side: Slot Name & Status Checkmark or Dot-Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (slotTitle.isNotBlank()) {
                            Text(
                                text = slotTitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp
                                ),
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isReadOnly) {
                            // Read-only mode
                        } else if (isTaken) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MediciTheme.colors.success.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.status_taken),
                                    tint = MediciTheme.colors.success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("medicine_more_button_${medicine.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.action_more_options),
                                        tint = MediciTheme.colors.textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier
                                        .background(MediciTheme.colors.card)
                                        .testTag("medicine_dropdown_menu_${medicine.id}")
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
                                            showMenu = false
                                            onEditClick()
                                        },
                                        modifier = Modifier.testTag("menu_edit_medicine_${medicine.id}")
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
                                            showMenu = false
                                            showDeleteConfirmDialog = true
                                        },
                                        modifier = Modifier.testTag("menu_delete_medicine_${medicine.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // === 2. CHIPS ROW: Unified design (same surface background, subtle border, textSecondary) ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dose Chip
                    Box(
                        modifier = Modifier
                            .background(
                                color = MediciTheme.colors.surface,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MediciTheme.colors.outline.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = doseAmountText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_card_dose_chip_${medicine.id}")
                        )
                    }

                    // Food Rule Chip
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .background(
                                color = MediciTheme.colors.surface,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MediciTheme.colors.outline.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = foodRuleText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_card_food_chip_${medicine.id}")
                        )
                    }
                }

                // === 2b. STOCK CHIP (Shown only if stockCount > 0) ===
                if (medicine.stockCount > 0) {
                    val isLowStock = medicine.stockCount <= 5
                    val stockTextColor = if (isLowStock) {
                        MediciTheme.colors.warning
                    } else {
                        MediciTheme.colors.textSecondary
                    }
                    val stockBackgroundColor = if (isLowStock) {
                        MediciTheme.colors.warning.copy(alpha = 0.12f)
                    } else {
                        MediciTheme.colors.surface
                    }
                    val stockBorderColor = if (isLowStock) {
                        MediciTheme.colors.warning.copy(alpha = 0.4f)
                    } else {
                        MediciTheme.colors.outline.copy(alpha = 0.25f)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = stockBackgroundColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = stockBorderColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("medicine_card_stock_chip_${medicine.id}")
                    ) {
                        Text(
                            text = stringResource(
                                R.string.medicine_stock_badge,
                                toBengaliDigits(medicine.stockCount.toString())
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = if (isLowStock) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = stockTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicine_card_stock_${medicine.id}")
                        )
                    }
                }

                // === 3. SPECIAL NOTE (Optional) ===
                if (!medicine.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MediciTheme.colors.surface.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MediciTheme.colors.outline.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MediciTheme.colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = medicine.note,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MediciTheme.colors.textSecondary
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("medicine_card_note_${medicine.id}")
                            )
                        }
                    }
                }

                // === 4. TAKE ACTION BUTTON WITH 30-MIN SLOT ACTIVATION ===
                if (showTakeButton && !isReadOnly && !isTaken) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BouncingButton(
                            onClick = onMarkAsTaken,
                            enabled = isSlotActive,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MediciTheme.colors.primary,
                                contentColor = MediciTheme.colors.onPrimary,
                                disabledContainerColor = MediciTheme.colors.textSecondary.copy(alpha = 0.18f),
                                disabledContentColor = MediciTheme.colors.textSecondary.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("medicine_card_take_button_${medicine.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_mark_taken),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!isSlotActive) {
                            Text(
                                text = stringResource(R.string.slot_not_time_yet),
                                style = MaterialTheme.typography.labelMedium,
                                color = MediciTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .testTag("medicine_card_not_time_yet_${medicine.id}")
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Deleting
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.medicine_delete_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.medicine_delete_confirm_with_name, medicine.name),
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
                        onDeleteClick()
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(
                        text = stringResource(R.string.medicine_menu_delete),
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
                    modifier = Modifier.testTag("cancel_delete_button")
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
}
