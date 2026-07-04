package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl
import com.deepankarsawhney.cameraadvisor.core.suggestion.Suggestion
import com.deepankarsawhney.cameraadvisor.core.suggestion.SuggestionCategory

@Composable
fun SuggestionHud(
    suggestions: List<Suggestion>,
    onSuggestionTapped: (ManualControl?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(suggestion = suggestion, onTapped = { onSuggestionTapped(suggestion.targetControl) })
        }
    }
}

private fun iconFor(category: SuggestionCategory): ImageVector = when (category) {
    SuggestionCategory.EXPOSURE -> Icons.Filled.WbSunny
    SuggestionCategory.FOCUS -> Icons.Filled.CenterFocusStrong
    SuggestionCategory.SHAKE -> Icons.Filled.Vibration
    SuggestionCategory.NOISE -> Icons.Filled.BlurOn
    SuggestionCategory.WHITE_BALANCE -> Icons.Filled.Palette
    SuggestionCategory.FRAMING -> Icons.Filled.Straighten
}

@Composable
private fun SuggestionChip(suggestion: Suggestion, onTapped: () -> Unit) {
    // Severity tints the icon from a calm amber toward a more urgent red.
    val accent = lerp(Color(0xFFFDD663), Color(0xFFF28B82), suggestion.severity.toFloat().coerceIn(0f, 1f))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = suggestion.targetControl != null, onClick = onTapped)
            .padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconFor(suggestion.category),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = suggestion.message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (suggestion.targetControl != null) {
            Text(text = "Fix", color = accent, style = MaterialTheme.typography.labelLarge)
        }
    }
}
