package com.mediciplanner.app.ui.screens.prescription

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Entry point for Prescription tab, delegating to the complete [PrescriptionListScreen].
 */
@Composable
fun PrescriptionScreen(
    modifier: Modifier = Modifier
) {
    PrescriptionListScreen(modifier = modifier)
}
