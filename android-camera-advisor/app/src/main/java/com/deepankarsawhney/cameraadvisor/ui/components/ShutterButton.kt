package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * [ringColor] doubles as an at-a-glance shot-readiness signal (green/amber/red based on the
 * current suggestions' severity) — see [readinessColor] — rather than a fixed decorative ring.
 */
@Composable
fun ShutterButton(onClick: () -> Unit, modifier: Modifier = Modifier, ringColor: Color = Color.White) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp)
            .border(4.dp, ringColor, CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color.White, CircleShape),
        )
    }
}
