package com.mediciplanner.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.MediciTheme

private enum class AboutPolicyNotice {
    PRIVACY,
    TERMS
}

@Composable
fun AboutAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Toast.makeText(
            context,
            context.getString(R.string.msg_about_loading),
            Toast.LENGTH_SHORT
        ).show()
    }
    var activeNotice by remember { mutableStateOf<AboutPolicyNotice?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MediciTheme.colors.cardBackground,
        modifier = Modifier.testTag("about_dialog"),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MediciTheme.colors.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = stringResource(R.string.about_app_name),
                        tint = MediciTheme.colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.about_app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("about_app_name")
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = MediciTheme.colors.primaryContainer,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_app_version),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("about_app_version")
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_app_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                val currentNotice = activeNotice
                if (currentNotice != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MediciTheme.colors.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                            .testTag("about_policy_notice_box")
                    ) {
                        Text(
                            text = when (currentNotice) {
                                AboutPolicyNotice.PRIVACY -> stringResource(R.string.about_privacy_summary)
                                AboutPolicyNotice.TERMS -> stringResource(R.string.about_terms_summary)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MediciTheme.colors.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            activeNotice = if (activeNotice == AboutPolicyNotice.PRIVACY) {
                                null
                            } else {
                                AboutPolicyNotice.PRIVACY
                            }
                        },
                        modifier = Modifier.testTag("about_privacy_button")
                    ) {
                        Text(
                            text = stringResource(R.string.about_privacy_policy),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MediciTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = {
                            activeNotice = if (activeNotice == AboutPolicyNotice.TERMS) {
                                null
                            } else {
                                AboutPolicyNotice.TERMS
                            }
                        },
                        modifier = Modifier.testTag("about_terms_button")
                    ) {
                        Text(
                            text = stringResource(R.string.about_terms),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MediciTheme.colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            BouncingButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediciTheme.colors.primary,
                    contentColor = MediciTheme.colors.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_close_button")
            ) {
                Text(
                    text = stringResource(R.string.about_close),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}
