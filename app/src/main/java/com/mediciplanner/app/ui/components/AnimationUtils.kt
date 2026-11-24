package com.mediciplanner.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.core.theme.MediciTheme
import kotlinx.coroutines.delay

/**
 * Reusable Modifier extension that scales down the element smoothly on press
 * using [animateFloatAsState] and [detectTapGestures], while preserving ripple
 * feedback and accessibility/testing click semantics.
 */
fun Modifier.bouncyClickable(
    onClick: () -> Unit
): Modifier = bouncyClickable(
    enabled = true,
    scaleDown = 0.96f,
    hapticType = HapticFeedbackType.TextHandleMove,
    onClick = onClick
)

/**
 * Configurable [bouncyClickable] modifier supporting disabled state, custom scale factor,
 * and optional haptic feedback.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean,
    scaleDown: Float = 0.96f,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    onClick: () -> Unit
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncy_clickable_scale"
    )
    val haptic = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    val indication = LocalIndication.current

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .indication(interactionSource = interactionSource, indication = indication)
        .semantics(mergeDescendants = true) {
            role = Role.Button
            if (enabled) {
                onClick(action = {
                    currentOnClick()
                    true
                })
            } else {
                disabled()
            }
        }
        .pointerInput(enabled) {
            detectTapGestures(
                onPress = { offset ->
                    if (enabled) {
                        isPressed = true
                        try {
                            haptic.performHapticFeedback(hapticType)
                        } catch (_: Exception) {
                        }
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        val released = tryAwaitRelease()
                        isPressed = false
                        interactionSource.emit(
                            if (released) {
                                PressInteraction.Release(press)
                            } else {
                                PressInteraction.Cancel(press)
                            }
                        )
                    }
                },
                onTap = {
                    if (enabled) {
                        currentOnClick()
                    }
                }
            )
        }
}

/**
 * Scale-down press animation button with visible Material ripple and haptic feedback.
 */
@Composable
fun BouncingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    scaleDown: Float = 0.96f,
    shadowElevation: Dp = if (elevation != null && enabled) 2.dp else 0.dp,
    content: @Composable RowScope.() -> Unit
) {
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncing_button_scale"
    )
    val haptic = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    val indication = LocalIndication.current

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (enabled) {
                    onClick(action = {
                        currentOnClick()
                        true
                    })
                } else {
                    disabled()
                }
            }
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = { offset ->
                        if (enabled) {
                            isPressed = true
                            try {
                                haptic.performHapticFeedback(hapticType)
                            } catch (_: Exception) {
                            }
                            val press = PressInteraction.Press(offset)
                            interactionSource.emit(press)
                            val released = tryAwaitRelease()
                            isPressed = false
                            interactionSource.emit(
                                if (released) {
                                    PressInteraction.Release(press)
                                } else {
                                    PressInteraction.Cancel(press)
                                }
                            )
                        }
                    },
                    onTap = {
                        if (enabled) {
                            currentOnClick()
                        }
                    }
                )
            },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.labelLarge
        ) {
            Row(
                modifier = Modifier
                    .indication(interactionSource = interactionSource, indication = indication)
                    .defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth,
                        minHeight = ButtonDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Outlined button with [Modifier.bouncyClickable] press scale animation.
 */
@Composable
fun BouncingOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    content: @Composable RowScope.() -> Unit
) {
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 48.dp)
            .clip(shape)
            .bouncyClickable(
                enabled = enabled,
                scaleDown = 0.96f,
                hapticType = hapticType,
                onClick = onClick
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.labelLarge
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth,
                        minHeight = ButtonDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Extended Floating Action Button with [Modifier.bouncyClickable] press scale animation.
 */
@Composable
fun BouncingExtendedFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MediciTheme.colors.primary,
    contentColor: Color = MediciTheme.colors.onPrimary,
    shape: Shape = CircleShape,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .bouncyClickable(
                enabled = true,
                scaleDown = 0.95f,
                onClick = onClick
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 6.dp
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.labelLarge
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Staggered entry animation (slide up + fade in) for list items.
 */
@Composable
fun StaggeredEntryItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(index) {
        delay(index * 45L)
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "stagger_alpha_$index"
    )

    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 35f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "stagger_trans_y_$index"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}
