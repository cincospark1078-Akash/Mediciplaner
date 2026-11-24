package com.mediciplanner.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme

sealed class BottomNavDestination(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object Home : BottomNavDestination(
        route = "home",
        labelResId = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_item_home"
    )

    data object Prescription : BottomNavDestination(
        route = "prescription",
        labelResId = R.string.nav_prescription,
        selectedIcon = Icons.Filled.Medication,
        unselectedIcon = Icons.Outlined.Medication,
        testTag = "nav_item_prescription"
    )

    data object Track : BottomNavDestination(
        route = "track",
        labelResId = R.string.nav_track,
        selectedIcon = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart,
        testTag = "nav_item_track"
    )

    data object Followup : BottomNavDestination(
        route = "followup",
        labelResId = R.string.nav_followup,
        selectedIcon = Icons.Filled.Event,
        unselectedIcon = Icons.Outlined.Event,
        testTag = "nav_item_followup"
    )
}

val bottomNavDestinations = listOf(
    BottomNavDestination.Home,
    BottomNavDestination.Prescription,
    BottomNavDestination.Track,
    BottomNavDestination.Followup
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigateToDestination: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingBottomNavBar(
        currentRoute = currentRoute,
        onNavigateToDestination = onNavigateToDestination,
        modifier = modifier
    )
}
