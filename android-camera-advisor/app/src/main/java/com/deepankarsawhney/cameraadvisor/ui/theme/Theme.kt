package com.deepankarsawhney.cameraadvisor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CameraAdvisorColorScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    secondary = Color(0xFFFDD663),
    error = Color(0xFFF28B82),
    surface = Color(0xFF1F1F1F),
    background = Color(0xFF000000),
)

@Composable
fun CameraAdvisorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CameraAdvisorColorScheme,
        content = content,
    )
}
