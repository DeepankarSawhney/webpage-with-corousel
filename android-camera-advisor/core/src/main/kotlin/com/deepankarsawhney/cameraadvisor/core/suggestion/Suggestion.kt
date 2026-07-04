package com.deepankarsawhney.cameraadvisor.core.suggestion

import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl

enum class SuggestionCategory {
    EXPOSURE,
    FOCUS,
    SHAKE,
    NOISE,
    WHITE_BALANCE,
}

enum class AdjustmentDirection {
    INCREASE,
    DECREASE,
    NONE,
}

data class Suggestion(
    val category: SuggestionCategory,
    val message: String,
    val severity: Double,
    val targetControl: ManualControl,
    val direction: AdjustmentDirection,
    val suggestedValueDescription: String? = null,
)
