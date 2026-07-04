package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl
import com.deepankarsawhney.cameraadvisor.core.suggestion.Suggestion
import com.deepankarsawhney.cameraadvisor.core.suggestion.SuggestionCategory

/**
 * Suggestion surface, styled as elevated Material 3 cards rather than flat translucent bars:
 * each card carries a category icon, the message, and a thin severity meter so you can gauge
 * "how bad is this" at a glance, not just "what's wrong."
 */
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
            SuggestionCard(suggestion = suggestion, onTapped = { onSuggestionTapped(suggestion.targetControl) })
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
private fun SuggestionCard(suggestion: Suggestion, onTapped: () -> Unit) {
    val accent = severityColor(suggestion.severity)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = suggestion.targetControl != null, onClick = onTapped),
        color = Color(0xFF1C1C1E).copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = iconFor(suggestion.category),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Text(
                        text = suggestion.message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (suggestion.targetControl != null) {
                    Text(text = "Fix", color = accent, style = MaterialTheme.typography.labelLarge)
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, start = 38.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(suggestion.severity.toFloat().coerceIn(0.08f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent),
                )
            }
        }
    }
}
