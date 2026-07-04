package com.deepankarsawhney.cameraadvisor.core.heuristics

/** Luminance-histogram based exposure analysis over a Y-plane (0..255) buffer. */
object ExposureAnalyzer {

    private const val SHADOW_BIN_MAX = 12
    private const val HIGHLIGHT_BIN_MIN = 250

    data class Result(
        val meanLuma: Double,
        val shadowClipFraction: Double,
        val highlightClipFraction: Double,
    )

    fun analyze(luma: IntArray): Result {
        require(luma.isNotEmpty()) { "luma buffer must not be empty" }
        var sum = 0L
        var shadowCount = 0
        var highlightCount = 0
        for (value in luma) {
            sum += value
            if (value <= SHADOW_BIN_MAX) shadowCount++
            if (value >= HIGHLIGHT_BIN_MIN) highlightCount++
        }
        val n = luma.size
        return Result(
            meanLuma = sum.toDouble() / n,
            shadowClipFraction = shadowCount.toDouble() / n,
            highlightClipFraction = highlightCount.toDouble() / n,
        )
    }
}
