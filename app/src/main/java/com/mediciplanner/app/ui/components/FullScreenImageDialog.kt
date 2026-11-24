package com.mediciplanner.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.screens.home.toBengaliDigits

/**
 * Full-screen zoomable and pannable image viewer dialog.
 * - Uses Dialog with DialogProperties(usePlatformDefaultWidth = false) for edge-to-edge full screen
 * - Shows image with Coil AsyncImage using contentScale = ContentScale.Fit
 * - Zoom & Pan using Modifier.pointerInput with detectTransformGestures (scale + pan)
 * - Close "X" IconButton at the top-right corner with min 48.dp touch target
 * - Carousel previous/next navigation when multiple images are passed
 */
@Composable
fun FullScreenImageDialog(
    imageModels: List<Any>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    if (imageModels.isEmpty()) return

    var currentIndex by remember(imageModels, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, imageModels.lastIndex))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        var scale by remember(currentIndex) { mutableFloatStateOf(1f) }
        var offset by remember(currentIndex) { mutableStateOf(Offset.Zero) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MediciTheme.colors.textPrimary.copy(alpha = 0.96f))
                .testTag("full_screen_image_dialog")
        ) {
            // Main Zoomable & Pannable Image Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            if (newScale > 1f) {
                                val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(currentIndex) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val currentModel = imageModels.getOrNull(currentIndex)
                AsyncImage(
                    model = currentModel,
                    contentDescription = title ?: stringResource(R.string.image_fullscreen_title),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .testTag("full_screen_image")
                )
            }

            // Top Header: Title / Page Indicator & Close "X" Button (Top-Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title or Index Counter badge
                Box(
                    modifier = Modifier
                        .background(MediciTheme.colors.textPrimary.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val countText = if (imageModels.size > 1) {
                        val currentStr = toBengaliDigits((currentIndex + 1).toString())
                        val totalStr = toBengaliDigits(imageModels.size.toString())
                        "$currentStr / $totalStr"
                    } else {
                        title ?: stringResource(R.string.image_fullscreen_title)
                    }
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MediciTheme.colors.cardBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("image_counter_text")
                    )
                }

                // Close "X" IconButton at top-right corner with 48.dp minimum touch target
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MediciTheme.colors.textPrimary.copy(alpha = 0.6f),
                        contentColor = MediciTheme.colors.cardBackground
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_fullscreen_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.image_fullscreen_close),
                        tint = MediciTheme.colors.cardBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Navigation Controls if multiple images
            if (imageModels.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp, start = 24.dp, end = 24.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    if (currentIndex > 0) {
                        IconButton(
                            onClick = {
                                currentIndex--
                                scale = 1f
                                offset = Offset.Zero
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MediciTheme.colors.textPrimary.copy(alpha = 0.6f),
                                contentColor = MediciTheme.colors.cardBackground
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("prev_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_previous),
                                tint = MediciTheme.colors.cardBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.size(48.dp))
                    }

                    // Next Button
                    if (currentIndex < imageModels.lastIndex) {
                        IconButton(
                            onClick = {
                                currentIndex++
                                scale = 1f
                                offset = Offset.Zero
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MediciTheme.colors.textPrimary.copy(alpha = 0.6f),
                                contentColor = MediciTheme.colors.cardBackground
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("next_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.action_next),
                                tint = MediciTheme.colors.cardBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

/**
 * Convenience single-image overload.
 */
@Composable
fun FullScreenImageDialog(
    imageModel: Any,
    onDismiss: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    FullScreenImageDialog(
        imageModels = listOf(imageModel),
        initialIndex = 0,
        onDismiss = onDismiss,
        title = title,
        modifier = modifier
    )
}
