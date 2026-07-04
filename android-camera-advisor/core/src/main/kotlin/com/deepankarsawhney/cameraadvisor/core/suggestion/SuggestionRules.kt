package com.deepankarsawhney.cameraadvisor.core.suggestion

import com.deepankarsawhney.cameraadvisor.core.domain.AfState
import com.deepankarsawhney.cameraadvisor.core.domain.FrameAssessment
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl
import com.deepankarsawhney.cameraadvisor.core.ml.SceneTag
import kotlin.math.min

/**
 * Scene-adjusted thresholds for turning a [FrameAssessment] into candidate [Suggestion]s.
 * Thresholds are looked up per [SceneTag]; scenes not listed fall back to [GENERAL].
 */
object SuggestionRules {

    private data class Thresholds(
        val highlightClipMax: Double,
        val meanLumaLowBand: Double,
        val shadowClipMinForUnderexposed: Double,
        val isoNoiseCeiling: Int,
        val sharpnessVarianceFloor: Double,
        val safeShutterNanos: Long,
        val shakeScoreThreshold: Double,
        val whiteBalanceCastThreshold: Double,
    )

    private val GENERAL = Thresholds(
        highlightClipMax = 0.03,
        meanLumaLowBand = 90.0,
        shadowClipMinForUnderexposed = 0.15,
        isoNoiseCeiling = 1600,
        sharpnessVarianceFloor = 40.0,
        safeShutterNanos = 16_666_667L, // 1/60s
        shakeScoreThreshold = 0.5,
        whiteBalanceCastThreshold = 0.15,
    )

    private val byScene: Map<SceneTag, Thresholds> = mapOf(
        SceneTag.GENERAL to GENERAL,
        SceneTag.LOW_LIGHT to GENERAL.copy(
            highlightClipMax = 0.05,
            meanLumaLowBand = 60.0,
            shadowClipMinForUnderexposed = 0.25,
            isoNoiseCeiling = 6400,
            sharpnessVarianceFloor = 25.0,
        ),
        SceneTag.ACTION to GENERAL.copy(
            isoNoiseCeiling = 3200,
            sharpnessVarianceFloor = 55.0,
            safeShutterNanos = 8_000_000L, // 1/125s
            shakeScoreThreshold = 0.4,
        ),
        SceneTag.BACKLIT to GENERAL.copy(
            highlightClipMax = 0.08,
            isoNoiseCeiling = 3200,
        ),
        SceneTag.PORTRAIT to GENERAL.copy(
            sharpnessVarianceFloor = 35.0,
        ),
        SceneTag.MACRO to GENERAL.copy(
            isoNoiseCeiling = 1600,
            sharpnessVarianceFloor = 60.0,
        ),
        SceneTag.LANDSCAPE to GENERAL,
    )

    private fun severityOf(value: Double, threshold: Double, scale: Double): Double {
        if (scale <= 0.0) return if (value > threshold) 1.0 else 0.0
        return min(1.0, (value - threshold) / scale).coerceAtLeast(0.0)
    }

    fun candidates(assessment: FrameAssessment, sceneTag: SceneTag): List<Suggestion> {
        val t = byScene[sceneTag] ?: GENERAL
        val result = mutableListOf<Suggestion>()

        if (assessment.highlightClipFraction > t.highlightClipMax) {
            result += Suggestion(
                category = SuggestionCategory.EXPOSURE,
                message = "Overexposed — lower exposure compensation or ISO",
                severity = severityOf(assessment.highlightClipFraction, t.highlightClipMax, t.highlightClipMax * 2),
                targetControl = ManualControl.EXPOSURE_COMPENSATION,
                direction = AdjustmentDirection.DECREASE,
            )
        }

        if (assessment.meanLuma < t.meanLumaLowBand && assessment.shadowClipFraction > t.shadowClipMinForUnderexposed) {
            result += Suggestion(
                category = SuggestionCategory.EXPOSURE,
                message = "Underexposed — raise ISO or exposure compensation",
                severity = severityOf(t.meanLumaLowBand - assessment.meanLuma, 0.0, t.meanLumaLowBand),
                targetControl = ManualControl.ISO,
                direction = AdjustmentDirection.INCREASE,
            )
        }

        if (assessment.sharpnessVariance < t.sharpnessVarianceFloor && assessment.afState != AfState.SCANNING) {
            result += Suggestion(
                category = SuggestionCategory.FOCUS,
                message = "Image appears soft — tap to focus",
                severity = severityOf(t.sharpnessVarianceFloor - assessment.sharpnessVariance, 0.0, t.sharpnessVarianceFloor),
                targetControl = ManualControl.FOCUS,
                direction = AdjustmentDirection.NONE,
            )
        }

        if (assessment.shakeScore > t.shakeScoreThreshold && assessment.exposureTimeNanos > t.safeShutterNanos) {
            result += Suggestion(
                category = SuggestionCategory.SHAKE,
                message = "Camera shake detected — use a faster shutter speed or brace the phone",
                severity = severityOf(assessment.shakeScore, t.shakeScoreThreshold, 1.0 - t.shakeScoreThreshold),
                targetControl = ManualControl.SHUTTER_SPEED,
                direction = AdjustmentDirection.DECREASE,
            )
        }

        if (assessment.isoSensitivity > t.isoNoiseCeiling) {
            result += Suggestion(
                category = SuggestionCategory.NOISE,
                message = "ISO very high — noise likely, allow more light or use a tripod for a slower shutter",
                severity = severityOf(
                    assessment.isoSensitivity.toDouble(),
                    t.isoNoiseCeiling.toDouble(),
                    t.isoNoiseCeiling.toDouble(),
                ),
                targetControl = ManualControl.ISO,
                direction = AdjustmentDirection.DECREASE,
            )
        }

        if (!assessment.awbLocked) {
            val castMagnitude = maxOf(
                kotlin.math.abs(assessment.redGreenRatio - 1.0),
                kotlin.math.abs(assessment.blueGreenRatio - 1.0),
            )
            if (castMagnitude > t.whiteBalanceCastThreshold) {
                val warm = assessment.redGreenRatio > assessment.blueGreenRatio
                result += Suggestion(
                    category = SuggestionCategory.WHITE_BALANCE,
                    message = if (warm) {
                        "Image looks warm — try adjusting white balance cooler"
                    } else {
                        "Image looks cool — try adjusting white balance warmer"
                    },
                    severity = severityOf(castMagnitude, t.whiteBalanceCastThreshold, t.whiteBalanceCastThreshold),
                    targetControl = ManualControl.WHITE_BALANCE,
                    direction = if (warm) AdjustmentDirection.DECREASE else AdjustmentDirection.INCREASE,
                )
            }
        }

        return result
    }
}
