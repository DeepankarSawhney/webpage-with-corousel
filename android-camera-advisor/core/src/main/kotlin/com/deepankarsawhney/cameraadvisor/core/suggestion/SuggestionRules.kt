package com.deepankarsawhney.cameraadvisor.core.suggestion

import com.deepankarsawhney.cameraadvisor.core.domain.AfState
import com.deepankarsawhney.cameraadvisor.core.domain.FrameAssessment
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl
import com.deepankarsawhney.cameraadvisor.core.ml.SceneTag
import kotlin.math.min
import kotlin.math.roundToInt

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

    private val ISO_STEPS = intArrayOf(50, 100, 200, 400, 800, 1600, 3200, 6400, 12800)
    private const val HORIZON_TILT_THRESHOLD_DEGREES = 4.0

    private fun severityOf(value: Double, threshold: Double, scale: Double): Double {
        if (scale <= 0.0) return if (value > threshold) 1.0 else 0.0
        return min(1.0, (value - threshold) / scale).coerceAtLeast(0.0)
    }

    /** Nearest ISO step at or above [current], skipped forward by [stepsUp] additional stops. */
    private fun isoStepUp(current: Int, stepsUp: Int): Int {
        var index = ISO_STEPS.indexOfFirst { it >= current }
        if (index == -1) index = ISO_STEPS.lastIndex
        val target = (index + stepsUp).coerceIn(0, ISO_STEPS.lastIndex)
        return ISO_STEPS[target]
    }

    /** Nearest ISO step at or below [ceiling]. */
    private fun isoStepAtOrBelow(ceiling: Int): Int {
        val index = ISO_STEPS.indexOfLast { it <= ceiling }
        return ISO_STEPS[if (index == -1) 0 else index]
    }

    private fun formatEvStops(stops: Double): String {
        val thirds = (stops * 3).roundToInt()
        return when {
            thirds % 3 == 0 -> "${thirds / 3} EV"
            else -> String.format("%.1f EV", thirds / 3.0)
        }
    }

    fun candidates(assessment: FrameAssessment, sceneTag: SceneTag): List<Suggestion> {
        val t = byScene[sceneTag] ?: GENERAL
        val result = mutableListOf<Suggestion>()

        if (assessment.highlightClipFraction > t.highlightClipMax) {
            val severity = severityOf(assessment.highlightClipFraction, t.highlightClipMax, t.highlightClipMax * 2)
            val evStops = 1.0 + severity // 1..2 EV
            val evLabel = formatEvStops(evStops)
            result += Suggestion(
                category = SuggestionCategory.EXPOSURE,
                message = "Overexposed — lower exposure compensation by about $evLabel (or lower ISO)",
                severity = severity,
                targetControl = ManualControl.EXPOSURE_COMPENSATION,
                direction = AdjustmentDirection.DECREASE,
                suggestedValueDescription = "-$evLabel",
            )
        }

        if (assessment.meanLuma < t.meanLumaLowBand && assessment.shadowClipFraction > t.shadowClipMinForUnderexposed) {
            val severity = severityOf(t.meanLumaLowBand - assessment.meanLuma, 0.0, t.meanLumaLowBand)
            val stepsUp = if (severity > 0.5) 2 else 1
            val targetIso = isoStepUp(assessment.isoSensitivity, stepsUp)
            result += Suggestion(
                category = SuggestionCategory.EXPOSURE,
                message = "Underexposed — raise ISO to about $targetIso (or increase exposure compensation)",
                severity = severity,
                targetControl = ManualControl.ISO,
                direction = AdjustmentDirection.INCREASE,
                suggestedValueDescription = "ISO $targetIso",
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
            val targetDenominator = (1_000_000_000.0 / t.safeShutterNanos).roundToInt()
            result += Suggestion(
                category = SuggestionCategory.SHAKE,
                message = "Camera shake detected — use a faster shutter speed (about 1/${targetDenominator}s or faster) or brace the phone",
                severity = severityOf(assessment.shakeScore, t.shakeScoreThreshold, 1.0 - t.shakeScoreThreshold),
                targetControl = ManualControl.SHUTTER_SPEED,
                direction = AdjustmentDirection.DECREASE,
                suggestedValueDescription = "1/${targetDenominator}s",
            )
        }

        if (assessment.isoSensitivity > t.isoNoiseCeiling) {
            val targetIso = isoStepAtOrBelow(t.isoNoiseCeiling)
            result += Suggestion(
                category = SuggestionCategory.NOISE,
                message = "ISO very high (${assessment.isoSensitivity}) — lower to about $targetIso or below, " +
                    "or use a tripod for a slower shutter",
                severity = severityOf(
                    assessment.isoSensitivity.toDouble(),
                    t.isoNoiseCeiling.toDouble(),
                    t.isoNoiseCeiling.toDouble(),
                ),
                targetControl = ManualControl.ISO,
                direction = AdjustmentDirection.DECREASE,
                suggestedValueDescription = "ISO $targetIso",
            )
        }

        if (!assessment.awbLocked) {
            val castMagnitude = maxOf(
                kotlin.math.abs(assessment.redGreenRatio - 1.0),
                kotlin.math.abs(assessment.blueGreenRatio - 1.0),
            )
            if (castMagnitude > t.whiteBalanceCastThreshold) {
                val warm = assessment.redGreenRatio > assessment.blueGreenRatio
                val severity = severityOf(castMagnitude, t.whiteBalanceCastThreshold, t.whiteBalanceCastThreshold)
                val kelvinShift = (300 + severity * 900).roundToInt() / 100 * 100
                result += Suggestion(
                    category = SuggestionCategory.WHITE_BALANCE,
                    message = if (warm) {
                        "Image looks warm — try shifting white balance about ${kelvinShift}K cooler"
                    } else {
                        "Image looks cool — try shifting white balance about ${kelvinShift}K warmer"
                    },
                    severity = severity,
                    targetControl = ManualControl.WHITE_BALANCE,
                    direction = if (warm) AdjustmentDirection.DECREASE else AdjustmentDirection.INCREASE,
                    suggestedValueDescription = "${if (warm) "-" else "+"}${kelvinShift}K",
                )
            }
        }

        val tiltMagnitude = kotlin.math.abs(assessment.horizonTiltDegrees)
        if (tiltMagnitude > HORIZON_TILT_THRESHOLD_DEGREES) {
            result += Suggestion(
                category = SuggestionCategory.FRAMING,
                message = "Horizon tilted about ${tiltMagnitude.roundToInt()}° — level your phone",
                severity = severityOf(tiltMagnitude, HORIZON_TILT_THRESHOLD_DEGREES, HORIZON_TILT_THRESHOLD_DEGREES * 3),
                targetControl = null,
                direction = AdjustmentDirection.NONE,
            )
        }

        return result
    }
}
