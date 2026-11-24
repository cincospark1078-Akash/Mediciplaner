package com.mediciplanner.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.core.theme.MediciTheme

enum class EmptyStateIllustrationStyle {
    PLANT_WELLNESS,
    LINE_ART_RX,
    DEFAULT_ICON
}

/**
 * Reusable Global Empty State component matching the reference designs:
 * - Floating card with RoundedCornerShape(20.dp) and subtle shadow
 * - Soft gradient background
 * - Delicate line-art illustration or botanical wellness graphic
 * - Fully rounded pill-shaped action button
 * - Strict Bengali text constraints with maxLines & TextOverflow.Ellipsis
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    illustrationStyle: EmptyStateIllustrationStyle = EmptyStateIllustrationStyle.PLANT_WELLNESS,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    testTag: String = "empty_state_view"
) {
    val primaryColor = MediciTheme.colors.primary
    val primaryContainer = MediciTheme.colors.primaryContainer
    val cardBackground = MediciTheme.colors.card
    val successColor = MediciTheme.colors.success
    val textSecondary = MediciTheme.colors.textSecondary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryContainer.copy(alpha = 0.35f),
                            cardBackground
                        )
                    )
                )
                .padding(MediciTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // Delicate Illustration Container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = primaryContainer.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (illustrationStyle) {
                    EmptyStateIllustrationStyle.PLANT_WELLNESS -> {
                        // Delicate botanical line art like the "Tips for everyday" plant image
                        Canvas(modifier = Modifier.size(64.dp)) {
                            val w = size.width
                            val h = size.height

                            // Stem
                            val stemPath = Path().apply {
                                moveTo(w * 0.5f, h * 0.85f)
                                quadraticTo(w * 0.48f, h * 0.5f, w * 0.5f, h * 0.2f)
                            }
                            drawPath(
                                path = stemPath,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Left Leaf
                            val leftLeaf = Path().apply {
                                moveTo(w * 0.5f, h * 0.6f)
                                cubicTo(w * 0.25f, h * 0.55f, w * 0.22f, h * 0.35f, w * 0.42f, h * 0.42f)
                                close()
                            }
                            drawPath(
                                path = leftLeaf,
                                color = successColor.copy(alpha = 0.35f)
                            )
                            drawPath(
                                path = leftLeaf,
                                color = primaryColor,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Right Leaf
                            val rightLeaf = Path().apply {
                                moveTo(w * 0.5f, h * 0.45f)
                                cubicTo(w * 0.75f, h * 0.4f, w * 0.78f, h * 0.2f, w * 0.55f, h * 0.28f)
                                close()
                            }
                            drawPath(
                                path = rightLeaf,
                                color = primaryContainer.copy(alpha = 0.8f)
                            )
                            drawPath(
                                path = rightLeaf,
                                color = primaryColor,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Plant pot base
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.2f),
                                radius = 8.dp.toPx(),
                                center = Offset(w * 0.5f, h * 0.85f)
                            )
                        }
                    }
                    EmptyStateIllustrationStyle.LINE_ART_RX -> {
                        // Delicate prescription clipboard line-art
                        Canvas(modifier = Modifier.size(64.dp)) {
                            val w = size.width
                            val h = size.height

                            // Clipboard body
                            val padPath = Path().apply {
                                moveTo(w * 0.25f, h * 0.25f)
                                lineTo(w * 0.75f, h * 0.25f)
                                lineTo(w * 0.75f, h * 0.85f)
                                lineTo(w * 0.25f, h * 0.85f)
                                close()
                            }
                            drawPath(
                                path = padPath,
                                color = primaryContainer.copy(alpha = 0.6f)
                            )
                            drawPath(
                                path = padPath,
                                color = primaryColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Top clip
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(w * 0.38f, h * 0.16f),
                                size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.12f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Prescription lines
                            drawLine(
                                color = primaryColor.copy(alpha = 0.6f),
                                start = Offset(w * 0.35f, h * 0.42f),
                                end = Offset(w * 0.65f, h * 0.42f),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = primaryColor.copy(alpha = 0.6f),
                                start = Offset(w * 0.35f, h * 0.55f),
                                end = Offset(w * 0.65f, h * 0.55f),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = primaryColor.copy(alpha = 0.6f),
                                start = Offset(w * 0.35f, h * 0.68f),
                                end = Offset(w * 0.55f, h * 0.68f),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    EmptyStateIllustrationStyle.DEFAULT_ICON -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Title: Bold, MediciTheme.colors.textPrimary
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MediciTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTag}_title")
            )

            // Subtitle: Regular, MediciTheme.colors.textSecondary
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTag}_subtitle")
            )

            // Action Button: Pill-shaped (fully rounded corners)
            if (actionLabel != null && onActionClick != null) {
                BouncingButton(
                    onClick = onActionClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MediciTheme.colors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(min = 48.dp)
                        .padding(top = MediciTheme.spacing.small)
                        .testTag("${testTag}_action_button")
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Illustrated empty state component for empty lists with botanical line art or prescription graphics.
 */
@Composable
fun IllustratedEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    illustrationStyle: EmptyStateIllustrationStyle = EmptyStateIllustrationStyle.PLANT_WELLNESS,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    testTag: String = "illustrated_empty_state"
) {
    EmptyStateView(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        illustrationStyle = illustrationStyle,
        actionLabel = actionLabel,
        onActionClick = onActionClick,
        testTag = testTag
    )
}
