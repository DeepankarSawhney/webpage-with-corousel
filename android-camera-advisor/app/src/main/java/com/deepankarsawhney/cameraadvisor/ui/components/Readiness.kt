package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.deepankarsawhney.cameraadvisor.core.suggestion.Suggestion

private val READY_GREEN = Color(0xFF81C995)
private val CAUTION_AMBER = Color(0xFFFDD663)
private val URGENT_RED = Color(0xFFF28B82)

/**
 * Maps the current suggestion list to a single at-a-glance readiness color: green when nothing's
 * wrong, sliding from amber toward red as the worst active suggestion's severity climbs.
 */
fun readinessColor(suggestions: List<Suggestion>): Color {
    val worstSeverity = suggestions.maxOfOrNull { it.severity } ?: return READY_GREEN
    return severityColor(worstSeverity)
}

/** Amber (mild) sliding toward red (severe) for a single suggestion's severity, 0..1. */
fun severityColor(severity: Double): Color =
    lerp(CAUTION_AMBER, URGENT_RED, severity.toFloat().coerceIn(0f, 1f))
