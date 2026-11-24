package com.mediciplanner.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.entity.DoseSlot

private data class SlotFilterOption(
    val slot: DoseSlot?,
    @StringRes val labelResId: Int,
    val chipTag: String
)

private val defaultSlotFilterOptions = listOf(
    SlotFilterOption(null, R.string.filter_slot_all, "slot_filter_chip_all"),
    SlotFilterOption(DoseSlot.MORNING, R.string.slot_morning, "slot_filter_chip_morning"),
    SlotFilterOption(DoseSlot.NOON, R.string.slot_noon, "slot_filter_chip_noon"),
    SlotFilterOption(DoseSlot.AFTERNOON, R.string.slot_afternoon, "slot_filter_chip_afternoon"),
    SlotFilterOption(DoseSlot.EVENING, R.string.slot_evening, "slot_filter_chip_evening"),
    SlotFilterOption(DoseSlot.NIGHT, R.string.slot_night, "slot_filter_chip_night")
)

/**
 * Reusable horizontal row of slot FilterChips:
 * "সব" (All / null), "সকাল", "দুপুর", "বিকেল", "সন্ধ্যা", "রাত".
 * Includes left/right gradient fade & scroll indicators when overflowing horizontally.
 */
@Composable
fun SlotFilterRow(
    selectedSlot: DoseSlot?,
    onSlotSelected: (DoseSlot?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fadeColor: Color = MediciTheme.colors.surface,
    testTag: String = "slot_filter_row"
) {
    val listState = rememberLazyListState()
    val canScrollBackward by remember { derivedStateOf { listState.canScrollBackward } }
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
    val transparentFade = fadeColor.copy(alpha = 0f)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(
                items = defaultSlotFilterOptions,
                key = { it.chipTag }
            ) { option ->
                val isSelected = selectedSlot == option.slot
                FilterChip(
                    selected = isSelected,
                    onClick = { onSlotSelected(option.slot) },
                    label = {
                        Text(
                            text = stringResource(option.labelResId),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MediciTheme.colors.primary,
                        selectedLabelColor = MediciTheme.colors.onPrimary,
                        selectedLeadingIconColor = MediciTheme.colors.onPrimary,
                        containerColor = MediciTheme.colors.surface,
                        labelColor = MediciTheme.colors.textPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MediciTheme.colors.outlineVariant,
                        selectedBorderColor = MediciTheme.colors.primary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag(option.chipTag)
                )
            }
        }

        // Left gradient fade & arrow when scrolled right
        AnimatedVisibility(
            visible = canScrollBackward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(40.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(fadeColor, transparentFade)
                        )
                    )
                    .testTag("${testTag}_fade_left"),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MediciTheme.colors.textSecondary.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Right gradient fade & arrow when more chips exist to the right
        AnimatedVisibility(
            visible = canScrollForward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(40.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(transparentFade, fadeColor)
                        )
                    )
                    .testTag("${testTag}_fade_right"),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MediciTheme.colors.textSecondary.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
