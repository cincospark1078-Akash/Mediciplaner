package com.mediciplanner.app.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.components.BouncingButton
import kotlinx.coroutines.launch

data class TutorialPageData(
    val icon: ImageVector,
    val titleRes: Int,
    val subtitleRes: Int
)

@Composable
fun TutorialScreen(
    onFinishTutorial: () -> Unit,
    onSkipTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val pages = listOf(
        TutorialPageData(
            icon = Icons.Outlined.ReceiptLong,
            titleRes = R.string.tutorial_page1_title,
            subtitleRes = R.string.tutorial_page1_subtitle
        ),
        TutorialPageData(
            icon = Icons.Outlined.NotificationsActive,
            titleRes = R.string.tutorial_page2_title,
            subtitleRes = R.string.tutorial_page2_subtitle
        ),
        TutorialPageData(
            icon = Icons.Outlined.ShowChart,
            titleRes = R.string.tutorial_page3_title,
            subtitleRes = R.string.tutorial_page3_subtitle
        ),
        TutorialPageData(
            icon = Icons.Outlined.CalendarMonth,
            titleRes = R.string.tutorial_page4_title,
            subtitleRes = R.string.tutorial_page4_subtitle
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MediciTheme.colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("tutorial_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: App Tour pill label (Left) & Skip Button (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MediciTheme.colors.primaryContainer.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_app_tour),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("tutorial_app_tour_label")
                    )
                }

                TextButton(
                    onClick = onSkipTutorial,
                    modifier = Modifier.testTag("tutorial_skip_button")
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_skip),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Middle: Horizontal Pager with exactly 4 pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("tutorial_pager")
            ) { pageIndex ->
                val pageData = pages[pageIndex]
                TutorialPageContent(
                    data = pageData,
                    pageNumber = pageIndex + 1
                )
            }

            // Bottom Section: Page Indicator Dots & Next / Get Started Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("tutorial_indicator_dots")
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "dot_width"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MediciTheme.colors.primary
                            } else {
                                MediciTheme.colors.primaryContainer.copy(alpha = 0.5f)
                            },
                            animationSpec = tween(300),
                            label = "dot_color"
                        )

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .size(width = dotWidth, height = 8.dp)
                                .background(
                                    color = dotColor,
                                    shape = if (isSelected) RoundedCornerShape(4.dp) else CircleShape
                                )
                        )
                    }
                }

                // Next or Get Started Button (Locked 20.dp radius)
                val isLastPage = pagerState.currentPage == pages.size - 1
                BouncingButton(
                    onClick = {
                        if (isLastPage) {
                            onFinishTutorial()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.primary,
                        contentColor = MediciTheme.colors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag(if (isLastPage) "tutorial_get_started_button" else "tutorial_next_button")
                ) {
                    Text(
                        text = if (isLastPage) {
                            stringResource(R.string.tutorial_get_started)
                        } else {
                            stringResource(R.string.action_next)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialPageContent(
    data: TutorialPageData,
    pageNumber: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration Badge
        Box(
            modifier = Modifier
                .size(130.dp)
                .background(
                    color = MediciTheme.colors.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = stringResource(data.titleRes),
                tint = MediciTheme.colors.primary,
                modifier = Modifier.size(68.dp)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = stringResource(data.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MediciTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tutorial_page_title_$pageNumber")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(data.subtitleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MediciTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tutorial_page_subtitle_$pageNumber")
        )
    }
}
