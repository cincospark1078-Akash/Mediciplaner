package com.mediciplanner.app.ui.screens.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.components.FrostedCard
import com.mediciplanner.app.ui.screens.home.toBengaliDigits

@Composable
fun ProgressRing(
    progressTaken: Int,
    progressTotal: Int = 0,
    progressMissed: Int = 0,
    progressScheduled: Int = 0,
    onNavigateToTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Total doses scheduled for today (Taken + Missed + Pending)
    val totalDoses = if (progressScheduled > 0) {
        progressScheduled
    } else if (progressTotal > 0) {
        progressTotal
    } else {
        progressTaken + progressMissed
    }

    // Hide if 0 (as per strict specification)
    if (totalDoses <= 0) {
        return
    }

    val hasStarted = totalDoses > 0

    val pendingDoses = (totalDoses - progressTaken - progressMissed).coerceAtLeast(0)
    val progressFraction = if (hasStarted) {
        val frac = (progressTaken.toFloat() / totalDoses.toFloat()).coerceIn(0f, 1f)
        if (pendingDoses > 0 && frac >= 1f) 0.99f else frac
    } else {
        0f
    }

    val percentage = if (hasStarted) {
        val rawPercent = ((progressTaken * 100) / totalDoses)
        if (pendingDoses > 0 && rawPercent >= 100) 99 else rawPercent
    } else {
        0
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val animatedSweep by animateFloatAsState(
        targetValue = if (startAnimation && hasStarted) progressFraction * 360f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress_sweep"
    )

    val animatedPercentage by animateIntAsState(
        targetValue = if (startAnimation && hasStarted) percentage else 0,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress_percentage"
    )

    val primaryColor = MediciTheme.colors.primary
    val trackColor = MediciTheme.colors.textSecondary.copy(alpha = 0.12f)

    // FrostedCard Glassmorphism container with RoundedCornerShape(20.dp)
    FrostedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToTrack() }
            .testTag("progress_ring_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MediciTheme.colors.primaryContainer.copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.progress_title),
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Gauge Section: Perfectly centered circular gauge and breakdown pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Circular Gauge Box: 150.dp with Canvas 130.dp
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .testTag("progress_ring_canvas_box"),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasStarted) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            val strokeWidth = 12.dp.toPx()

                            // Background Track
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Progress Arc: Thick (12.dp) stroke using theme Primary color
                            if (animatedSweep > 0f) {
                                drawArc(
                                    color = primaryColor,
                                    startAngle = -90f,
                                    sweepAngle = animatedSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    // Perfectly centered text inside the ring using Alignment.Center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = MediciTheme.spacing.small)
                    ) {
                        if (!hasStarted) {
                            Text(
                                text = stringResource(R.string.progress_not_started),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MediciTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "${toBengaliDigits(animatedPercentage.toString())}%",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = MediciTheme.colors.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${toBengaliDigits(progressTaken.toString())}/${toBengaliDigits(totalDoses.toString())} ${stringResource(R.string.doses_completed)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Medium,
                                color = MediciTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Breakdown Pills
                Column(
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                    modifier = Modifier.padding(start = MediciTheme.spacing.small)
                ) {
                    // Taken dose badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = MediciTheme.colors.primaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.status_taken),
                                style = MaterialTheme.typography.labelSmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val unitTi = stringResource(R.string.unit_ti)
                            Text(
                                text = "${toBengaliDigits(progressTaken.toString())} $unitTi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Scheduled / remaining dose badge
                    val remaining = if (hasStarted) (totalDoses - progressTaken).coerceAtLeast(0) else 0
                    val unitTi = stringResource(R.string.unit_ti)
                    Box(
                        modifier = Modifier
                            .background(
                                color = MediciTheme.colors.surface,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.status_pending),
                                style = MaterialTheme.typography.labelSmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${toBengaliDigits(remaining.toString())} $unitTi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.textPrimary,
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
