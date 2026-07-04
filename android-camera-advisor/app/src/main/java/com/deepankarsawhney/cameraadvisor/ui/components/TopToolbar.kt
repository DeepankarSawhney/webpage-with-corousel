package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Translucent pill toolbar (flash / grid / manual-controls), styled after the iOS Camera app. */
@Composable
fun TopToolbar(
    flashMode: Int,
    gridVisible: Boolean,
    onCycleFlash: () -> Unit,
    onToggleGrid: () -> Unit,
    onOpenManualControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
