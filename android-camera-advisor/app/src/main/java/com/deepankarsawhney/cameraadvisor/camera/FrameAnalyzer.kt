package com.deepankarsawhney.cameraadvisor.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.deepankarsawhney.cameraadvisor.analysis.ml.SceneClassifier
import com.deepankarsawhney.cameraadvisor.core.domain.FrameAssessment
import com.deepankarsawhney.cameraadvisor.core.domain.FrameMetrics
import com.deepankarsawhney.cameraadvisor.core.heuristics.HeuristicEngine
import com.deepankarsawhney.cameraadvisor.core.ml.SceneTag
import com.deepankarsawhney.cameraadvisor.util.extractDownsampled
import com.deepankarsawhney.cameraadvisor.util.toJpegBitmap
import java.util.concurrent.atomic.AtomicInteger

/**
 * CameraX ImageAnalysis.Analyzer: extracts a downsampled luma/RGB buffer every frame for the
 * heuristic engine, and throttles the (comparatively expensive) TFLite scene classifier to every
 * [SCENE_CLASSIFY_EVERY_N_FRAMES]th frame. Always runs on the analysis executor, never the main thread.
 */
class FrameAnalyzer(
    private val sceneClassifier: SceneClassifier,
    private val currentMetrics: () -> FrameMetrics,
    private val currentShakeScore: () -> Double,
    private val onAssessment: (FrameAssessment, SceneTag, Double) -> Unit,
) : ImageAnalysis.Analyzer {

    private val frameCounter = AtomicInteger(0)

    @Volatile private var lastSceneTag: SceneTag = SceneTag.GENERAL
    @Volatile private var lastSceneConfidence: Double = 0.0

    override fun analyze(image: ImageProxy) {
        try {
            val frame = image.extractDownsampled()
            val assessment: FrameAssessment = HeuristicEngine.assess(
                luma = frame.luma,
                width = frame.width,
                height = frame.height,
                avgRed = frame.avgRed,
                avgGreen = frame.avgGreen,
                avgBlue = frame.avgBlue,
                metrics = currentMetrics(),
                shakeScore = currentShakeScore(),
            )

            if (frameCounter.incrementAndGet() % SCENE_CLASSIFY_EVERY_N_FRAMES == 0) {
                val result = sceneClassifier.classify(image.toJpegBitmap())
                lastSceneTag = result.sceneTag
                lastSceneConfidence = result.confidence
            }

            onAssessment(assessment, lastSceneTag, lastSceneConfidence)
        } finally {
            image.close()
        }
    }

    private companion object {
        const val SCENE_CLASSIFY_EVERY_N_FRAMES = 12
    }
}
