package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl
import com.deepankarsawhney.cameraadvisor.core.suggestion.Suggestion

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

@Composable
private fun SuggestionChip(suggestion: Suggestion, onTapped: () -> Unit) {
    // Severity tints the chip from a calm amber toward a more urgent red.
    val accent = lerp(Color(0xFFFDD663), Color(0xFFF28B82), suggestion.severity.toFloat().coerceIn(0f, 1f))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = suggestion.targetControl != null, onClick = onTapped)
            .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
                    .padding(horizontal = 2.dp),
            ) {}
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
