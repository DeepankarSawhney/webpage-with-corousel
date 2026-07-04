package com.deepankarsawhney.cameraadvisor.core.suggestion

import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl

enum class SuggestionCategory {
    EXPOSURE,
    FOCUS,
    SHAKE,
    NOISE,
    WHITE_BALANCE,
    FRAMING,
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
    /** Null for suggestions with no corresponding manual control (e.g. FRAMING tips). */
    val targetControl: ManualControl?,
    val direction: AdjustmentDirection,
    val suggestedValueDescription: String? = null,
)
