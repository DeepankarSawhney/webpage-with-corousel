package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Individually-spaced circular icon buttons (flash / grid / manual-controls), each its own
 * translucent scrim — the common Android/Pixel-camera convention, rather than one grouped pill.
 */
@Composable
fun TopToolbar(
    flashMode: Int,
    gridVisible: Boolean,
    onCycleFlash: () -> Unit,
    onToggleGrid: () -> Unit,
    onOpenManualControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolbarIconButton(onClick = onCycleFlash) {
            Icon(
                imageVector = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                    else -> Icons.Filled.FlashOff
                },
                contentDescription = "Flash",
                tint = Color.White,
            )
        }
        ToolbarIconButton(onClick = onToggleGrid) {
            Icon(
                imageVector = if (gridVisible) Icons.Filled.GridOn else Icons.Filled.GridOff,
                contentDescription = "Grid",
                tint = Color.White,
            )
        }
        ToolbarIconButton(onClick = onOpenManualControls) {
            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Manual controls", tint = Color.White)
        }
    }
}

@Composable
private fun ToolbarIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
