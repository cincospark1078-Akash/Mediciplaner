package com.mediciplanner.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.navigation.AppRoutes
import com.mediciplanner.app.ui.navigation.FloatingBottomNavBar
import com.mediciplanner.app.ui.navigation.bottomNavDestinations
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class FloatingBottomNavTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBottomNavDestinationsCountIsExactlyFour() {
        assertEquals("Bottom nav must have exactly 4 destinations", 4, bottomNavDestinations.size)
    }

    @Test
    fun testBottomNavDestinationsOrderAndRoutes() {
        assertEquals("home", bottomNavDestinations[0].route)
        assertEquals("prescription", bottomNavDestinations[1].route)
        assertEquals("track", bottomNavDestinations[2].route)
        assertEquals("followup", bottomNavDestinations[3].route)
    }

    @Test
    fun testBottomNavDestinationsTestTags() {
        assertEquals("nav_item_home", bottomNavDestinations[0].testTag)
        assertEquals("nav_item_prescription", bottomNavDestinations[1].testTag)
        assertEquals("nav_item_track", bottomNavDestinations[2].testTag)
        assertEquals("nav_item_followup", bottomNavDestinations[3].testTag)
    }

    @Test
    fun testDestinationsAreDistinct() {
        val routes = bottomNavDestinations.map { it.route }
        assertEquals(routes.distinct().size, routes.size)
    }

    @Test
    fun testSelectedTabShowsLabelPillAndUnselectedShowOnlyIcon() {
        var currentRoute by mutableStateOf(AppRoutes.HOME)

        composeTestRule.setContent {
            MediciTheme {
                FloatingBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigateToDestination = { currentRoute = it.route }
                )
            }
        }
        composeTestRule.waitForIdle()

        // All 4 tab items exist
        composeTestRule.onNodeWithTag("nav_item_home").assertIsDisplayed().assertIsSelected()
        composeTestRule.onNodeWithTag("nav_item_prescription").assertIsDisplayed().assertIsNotSelected()
        composeTestRule.onNodeWithTag("nav_item_track").assertIsDisplayed().assertIsNotSelected()
        composeTestRule.onNodeWithTag("nav_item_followup").assertIsDisplayed().assertIsNotSelected()

        // Only selected item (Home) displays its label text
        composeTestRule.onNodeWithTag("nav_item_home_label", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("হোম")
        composeTestRule.onNodeWithText("প্রেসক্রিপশন", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("ট্র্যাক", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("ফলো-আপ", useUnmergedTree = true).assertDoesNotExist()

        // Click Track tab -> Track expands with label, Home collapses to icon only
        composeTestRule.onNodeWithTag("nav_item_track").performClick()
        composeTestRule.waitForIdle()

        assertEquals(AppRoutes.TRACK, currentRoute)
        composeTestRule.onNodeWithTag("nav_item_track").assertIsSelected()
        composeTestRule.onNodeWithTag("nav_item_home").assertIsNotSelected()
        composeTestRule.onNodeWithTag("nav_item_track_label", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("ট্র্যাক")
        composeTestRule.onNodeWithText("হোম", useUnmergedTree = true).assertDoesNotExist()
    }
}
