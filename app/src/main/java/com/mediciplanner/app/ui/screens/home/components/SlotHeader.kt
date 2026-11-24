package com.mediciplanner.app.ui.screens.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotEmojiResId
import com.mediciplanner.app.ui.screens.prescription.getDoseSlotResId

@Composable
fun SlotHeader(
    slot: DoseSlot,
    medicineCount: Int,
    modifier: Modifier = Modifier,
    isExpanded: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    SlotHeader(
        slotName = slot.name,
        medicineCount = medicineCount,
        modifier = modifier,
        isExpanded = isExpanded,
        onClick = onClick
    )
}

@Composable
fun SlotHeader(
    slotName: String,
    medicineCount: Int,
    modifier: Modifier = Modifier,
    isExpanded: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    val slotEnum = try {
        DoseSlot.valueOf(slotName)
    } catch (e: Exception) {
        DoseSlot.CUSTOM
    }

    val slotTitle = stringResource(getDoseSlotResId(slotEnum))
    val slotEmoji = stringResource(getDoseSlotEmojiResId(slotEnum))

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded == true) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "slot_chevron_rotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MediciTheme.spacing.small)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("slot_header_${slotName.lowercase()}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = MediciTheme.colors.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = slotEmoji,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("slot_emoji_${slotName.lowercase()}")
                )
            }
            Text(
                text = slotTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MediciTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
        ) {
            val unitTi = stringResource(R.string.unit_ti)
            Box(
                modifier = Modifier
                    .background(
                        color = MediciTheme.colors.surface,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = MediciTheme.spacing.small, vertical = 2.dp)
            ) {
                Text(
                    text = "${toBengaliDigits(medicineCount.toString())} $unitTi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isExpanded != null) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.slot_collapse else R.string.slot_expand
                    ),
                    tint = MediciTheme.colors.textSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevronRotation)
                        .testTag("slot_chevron_${slotName.lowercase()}")
                )
            }
        }
    }
}
