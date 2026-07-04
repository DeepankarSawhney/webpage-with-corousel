package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A simple rule-of-thirds composition grid drawn over the viewfinder as a framing aid. */
@Composable
fun RuleOfThirdsOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val lineColor = Color.White.copy(alpha = 0.35f)
        val strokeWidth = 1.dp.toPx()

        val thirdWidth = size.width / 3f
        val thirdHeight = size.height / 3f

        for (i in 1..2) {
            drawLine(
                color = lineColor,
                start = Offset(thirdWidth * i, 0f),
                end = Offset(thirdWidth * i, size.height),
                strokeWidth = strokeWidth,
            )
            drawLine(
                color = lineColor,
                start = Offset(0f, thirdHeight * i),
                end = Offset(size.width, thirdHeight * i),
                strokeWidth = strokeWidth,
            )
        }
    }
}
