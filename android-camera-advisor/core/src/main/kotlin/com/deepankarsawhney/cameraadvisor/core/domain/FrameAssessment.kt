package com.deepankarsawhney.cameraadvisor.core.domain

/** Aggregated per-frame heuristic scores, produced by HeuristicEngine and consumed by SuggestionRules. */
data class FrameAssessment(
    val meanLuma: Double,
    val shadowClipFraction: Double,
    val highlightClipFraction: Double,
    val sharpnessVariance: Double,
    val shakeScore: Double,
    val localNoiseVariance: Double,
    val redGreenRatio: Double,
    val blueGreenRatio: Double,
    val isoSensitivity: Int,
    val exposureTimeNanos: Long,
    val afState: AfState,
    val awbLocked: Boolean,
    /** Device roll relative to level, in degrees, from the accelerometer (0 = level horizon). */
    val horizonTiltDegrees: Double = 0.0,
)
