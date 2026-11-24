package com.mediciplanner.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.core.theme.MediciTheme

/**
 * AppShell composable that wraps the primary layout Scaffold with the Floating Pill Navigation Bar.
 */
@Composable
fun AppShell(
    currentRoute: String?,
    shouldShowBottomBar: Boolean,
    onNavigateToDestination: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MediciTheme.colors.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (shouldShowBottomBar) {
                FloatingBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigateToDestination = onNavigateToDestination
                )
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

/**
 * Floating Pill Bottom Navigation Bar:
 * - Outer capsule: 24.dp horizontal & 16.dp vertical padding, RoundedCornerShape(50), 8.dp shadow, card background
 * - Selected item: Expands smoothly into a pill (RoundedCornerShape(50)) with primary background,
 *   horizontal = 16.dp, vertical = 8.dp padding, showing Icon + Label side-by-side
 * - Unselected items: 48.dp circular box with surface background and textSecondary icon tint (no text)
 * - Exactly 4 tabs: Home, Prescription, Track, Follow-up
 */
@Composable
fun FloatingBottomNavBar(
    currentRoute: String?,
    onNavigateToDestination: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .testTag("bottom_nav_bar"),
            shape = RoundedCornerShape(50),
            color = MediciTheme.colors.card,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavDestinations.forEach { destination ->
                    val isSelected = currentRoute == destination.route
                    FloatingNavItem(
                        destination = destination,
                        isSelected = isSelected,
                        onClick = { onNavigateToDestination(destination) },
                        modifier = if (isSelected) {
                            Modifier.weight(1f, fill = false)
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    destination: BottomNavDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(destination.labelResId)
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MediciTheme.colors.primary else MediciTheme.colors.surface,
        animationSpec = tween(durationMillis = 250),
        label = "nav_item_bg_${destination.route}"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(color = backgroundColor, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                selected = isSelected
                role = Role.Tab
                contentDescription = label
            }
            .testTag(destination.testTag),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) +
                    expandHorizontally(
                        animationSpec = tween(240),
                        expandFrom = Alignment.Start
                    )) togetherWith
                    (fadeOut(animationSpec = tween(150)) +
                        shrinkHorizontally(
                            animationSpec = tween(200),
                            shrinkTowards = Alignment.Start
                        ))
            },
            label = "nav_item_content_${destination.route}"
        ) { selectedState ->
            if (selectedState) {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = destination.selectedIcon,
                        contentDescription = null,
                        tint = MediciTheme.colors.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("${destination.testTag}_label")
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = destination.unselectedIcon,
                        contentDescription = null,
                        tint = MediciTheme.colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
