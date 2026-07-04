package com.deepankarsawhney.cameraadvisor.core.heuristics

import com.deepankarsawhney.cameraadvisor.core.domain.FrameAssessment
import com.deepankarsawhney.cameraadvisor.core.domain.FrameMetrics

/** Orchestrates the four heuristic analyzers into a single FrameAssessment. */
object HeuristicEngine {

    fun assess(
        luma: IntArray,
        width: Int,
        height: Int,
        avgRed: Double,
        avgGreen: Double,
        avgBlue: Double,
        metrics: FrameMetrics,
        shakeScore: Double,
        horizonTiltDegrees: Double = 0.0,
    ): FrameAssessment {
        val exposure = ExposureAnalyzer.analyze(luma)
        val sharpnessVariance = SharpnessAnalyzer.laplacianVariance(luma, width, height)
        val noiseVariance = NoiseAnalyzer.flatRegionNoiseVariance(luma, width, height)
        val whiteBalance = WhiteBalanceAnalyzer.analyze(avgRed, avgGreen, avgBlue)

        return FrameAssessment(
            meanLuma = exposure.meanLuma,
            shadowClipFraction = exposure.shadowClipFraction,
            highlightClipFraction = exposure.highlightClipFraction,
            sharpnessVariance = sharpnessVariance,
            shakeScore = shakeScore,
            localNoiseVariance = noiseVariance,
            redGreenRatio = whiteBalance.redGreenRatio,
            blueGreenRatio = whiteBalance.blueGreenRatio,
            isoSensitivity = metrics.isoSensitivity,
            exposureTimeNanos = metrics.exposureTimeNanos,
            afState = metrics.afState,
            awbLocked = metrics.awbLocked,
            horizonTiltDegrees = horizonTiltDegrees,
        )
    }
}
