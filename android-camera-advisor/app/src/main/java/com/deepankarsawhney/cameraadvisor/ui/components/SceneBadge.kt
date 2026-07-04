package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deepankarsawhney.cameraadvisor.core.ml.SceneTag

/**
 * Small ambient badge naming the detected scene (Samsung Scene Optimizer-style), shown only when
 * the classifier is confident about something other than GENERAL — this was already computed by
 * SceneClassifier/SuggestionEngine but had no UI surface until now.
 */
@Composable
fun SceneBadge(sceneTag: SceneTag, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = sceneTag != SceneTag.GENERAL, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val (icon, label) = sceneInfo(sceneTag)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier)
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

private fun sceneInfo(sceneTag: SceneTag): Pair<ImageVector, String> = when (sceneTag) {
    SceneTag.PORTRAIT -> Icons.Filled.Face to "Portrait"
    SceneTag.LANDSCAPE -> Icons.Filled.Landscape to "Landscape"
    SceneTag.LOW_LIGHT -> Icons.Filled.Brightness2 to "Low Light"
    SceneTag.ACTION -> Icons.Filled.DirectionsRun to "Action"
    SceneTag.MACRO -> Icons.Filled.Grain to "Macro"
    SceneTag.BACKLIT -> Icons.Filled.Brightness7 to "Backlit"
    SceneTag.GENERAL -> Icons.Filled.Brightness7 to "" // unreachable, AnimatedVisibility hides GENERAL
}
