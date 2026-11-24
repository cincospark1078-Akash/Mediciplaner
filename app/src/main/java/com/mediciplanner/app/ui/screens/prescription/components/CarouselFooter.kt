package com.mediciplanner.app.ui.screens.prescription.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme
import kotlinx.coroutines.CoroutineScope

@Composable
fun CarouselFooter(
    currentPage: Int,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit,
    onAddMoreClick: () -> Unit,
    onReviewAndSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNextEnabled: Boolean = true,
    isSaving: Boolean = false,
    isCard1Valid: Boolean = true,
    isCard2Valid: Boolean = true,
    isCard3Valid: Boolean = true,
    hasNextMedicine: Boolean = false,
    onNextMedicineClick: () -> Unit = {},
    pagerState: PagerState? = null,
    scope: CoroutineScope? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("carousel_footer"),
        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
    ) {
        when (currentPage) {
            0 -> {
                // Page 0 (Card 1): Only Next button
                com.mediciplanner.app.ui.components.BouncingButton(
                    onClick = onNextClick,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.primary,
                        contentColor = MediciTheme.colors.onPrimary,
                        disabledContainerColor = MediciTheme.colors.outlineVariant,
                        disabledContentColor = MediciTheme.colors.textSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("footer_next_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_next),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            1, 2 -> {
                // Pages 1 and 2 (Cards 2 & 3): Previous + Next buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.mediciplanner.app.ui.components.BouncingOutlinedButton(
                        onClick = onPreviousClick,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MediciTheme.colors.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MediciTheme.colors.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("footer_prev_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_previous),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    com.mediciplanner.app.ui.components.BouncingButton(
                        onClick = onNextClick,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MediciTheme.colors.primary,
                            contentColor = MediciTheme.colors.onPrimary,
                            disabledContainerColor = MediciTheme.colors.outlineVariant,
                            disabledContentColor = MediciTheme.colors.textSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("footer_next_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_next),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            3 -> {
                // Page 3 (Card 4): Equal width (weight 1f) and equal height (50.dp) buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.mediciplanner.app.ui.components.BouncingOutlinedButton(
                        onClick = onAddMoreClick,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MediciTheme.colors.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MediciTheme.colors.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("footer_add_more_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_add_more),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    com.mediciplanner.app.ui.components.BouncingButton(
                        onClick = onReviewAndSaveClick,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MediciTheme.colors.primary,
                            contentColor = MediciTheme.colors.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("footer_review_save_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = MediciTheme.colors.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = MediciTheme.spacing.small)
                            )
                            Text(
                                text = stringResource(R.string.action_saving),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.action_review_save),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (hasNextMedicine) {
                    com.mediciplanner.app.ui.components.BouncingOutlinedButton(
                        onClick = onNextMedicineClick,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MediciTheme.colors.primaryContainer.copy(alpha = 0.45f),
                            contentColor = MediciTheme.colors.primary
                        ),
                        border = BorderStroke(1.5.dp, MediciTheme.colors.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("footer_next_medicine_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_next_medicine),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
