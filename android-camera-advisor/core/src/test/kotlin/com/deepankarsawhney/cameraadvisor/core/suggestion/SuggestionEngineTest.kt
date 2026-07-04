package com.deepankarsawhney.cameraadvisor.core.suggestion

import com.deepankarsawhney.cameraadvisor.core.domain.AfState
import com.deepankarsawhney.cameraadvisor.core.domain.FrameAssessment
import com.deepankarsawhney.cameraadvisor.core.ml.SceneTag
import com.deepankarsawhney.cameraadvisor.core.util.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionEngineTest {

    private val clock = FakeClock(0)
    private val engine = SuggestionEngine(
        clock = clock,
        dwellToShowMillis = 100,
        dwellToHideMillis = 100,
        cooldownMillis = 200,
        maxVisible = 2,
        sceneDwellMillis = 50,
        sceneConfidenceFloor = 0.5,
    )

    private fun baselineAssessment() = FrameAssessment(
        meanLuma = 150.0,
        shadowClipFraction = 0.0,
        highlightClipFraction = 0.0,
        sharpnessVariance = 100.0,
        shakeScore = 0.0,
        localNoiseVariance = 0.0,
        redGreenRatio = 1.0,
        blueGreenRatio = 1.0,
        isoSensitivity = 100,
        exposureTimeNanos = 8_000_000L,
        afState = AfState.FOCUSED,
        awbLocked = true,
    )

    @Test
    fun `no candidates for a well-balanced frame`() {
        repeat(5) {
            clock.advanceBy(50)
            assertTrue(engine.evaluate(baselineAssessment(), SceneTag.GENERAL, 1.0).isEmpty())
        }
    }

    @Test
    fun `suggestion does not appear before dwell time elapses`() {
        val assessment = baselineAssessment().copy(highlightClipFraction = 0.5)
        val result = engine.evaluate(assessment, SceneTag.GENERAL, 1.0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `suggestion appears once dwell time elapses`() {
        val assessment = baselineAssessment().copy(highlightClipFraction = 0.5)
        engine.evaluate(assessment, SceneTag.GENERAL, 1.0)
        clock.advanceBy(150)
        val result = engine.evaluate(assessment, SceneTag.GENERAL, 1.0)
        assertEquals(1, result.size)
        assertEquals(SuggestionCategory.EXPOSURE, result.first().category)
    }

    @Test
    fun `conflicting ISO suggestions resolve to the higher severity one`() {
        // Underexposed (wants ISO up) and high-ISO noise (wants ISO down) both target ISO.
        val assessment = baselineAssessment().copy(
            meanLuma = 50.0,
            shadowClipFraction = 0.3,
            isoSensitivity = 3000,
        )
        engine.evaluate(assessment, SceneTag.GENERAL, 1.0)
        clock.advanceBy(150)
        val result = engine.evaluate(assessment, SceneTag.GENERAL, 1.0)
        assertEquals(1, result.size)
        assertEquals(SuggestionCategory.NOISE, result.first().category)
        assertEquals(AdjustmentDirection.DECREASE, result.first().direction)
    }

    @Test
    fun `caps output at two suggestions even with five active categories`() {
        val assessment = baselineAssessment().copy(
            highlightClipFraction = 0.042, // severity 0.2
            sharpnessVariance = 24.0, // severity 0.4
            shakeScore = 0.8, // severity 0.6
            exposureTimeNanos = 20_000_000L,
            isoSensitivity = 2880, // severity 0.8
            redGreenRatio = 1.3, // severity 1.0
            blueGreenRatio = 1.0,
            awbLocked = false,
        )
        engine.evaluate(assessment, SceneTag.GENERAL, 1.0)
        clock.advanceBy(150)
        val result = engine.evaluate(assessment, SceneTag.GENERAL, 1.0)

        assertEquals(2, result.size)
        assertEquals(
            setOf(SuggestionCategory.WHITE_BALANCE, SuggestionCategory.NOISE),
            result.map { it.category }.toSet(),
        )
        // Ranked by severity descending.
        assertTrue(result[0].severity >= result[1].severity)
    }

    @Test
    fun `suggestion stays shown through the hide grace period then clears`() {
        val triggering = baselineAssessment().copy(highlightClipFraction = 0.5)
        engine.evaluate(triggering, SceneTag.GENERAL, 1.0)
        clock.advanceBy(150)
        assertEquals(1, engine.evaluate(triggering, SceneTag.GENERAL, 1.0).size)

        val cleared = baselineAssessment()
        clock.advanceBy(50)
        assertEquals(1, engine.evaluate(cleared, SceneTag.GENERAL, 1.0).size) // grace period

        clock.advanceBy(150)
        assertTrue(engine.evaluate(cleared, SceneTag.GENERAL, 1.0).isEmpty())
    }

    @Test
    fun `low scene classifier confidence does not switch the active scene`() {
        val assessment = baselineAssessment().copy(isoSensitivity = 5000) // over GENERAL ceiling, under LOW_LIGHT's
        engine.evaluate(assessment, SceneTag.LOW_LIGHT, sceneConfidence = 0.1)
        clock.advanceBy(150)
        val result = engine.evaluate(assessment, SceneTag.LOW_LIGHT, sceneConfidence = 0.1)

        assertEquals(SceneTag.GENERAL, engine.currentSceneTag)
        assertEquals(1, result.size)
        assertEquals(SuggestionCategory.NOISE, result.first().category)
    }

    @Test
    fun `high confidence scene tag becomes stable after its dwell time`() {
        val assessment = baselineAssessment().copy(isoSensitivity = 5000)
        engine.evaluate(assessment, SceneTag.LOW_LIGHT, sceneConfidence = 0.9)
        clock.advanceBy(60) // exceeds sceneDwellMillis = 50
        engine.evaluate(assessment, SceneTag.LOW_LIGHT, sceneConfidence = 0.9)

        assertEquals(SceneTag.LOW_LIGHT, engine.currentSceneTag)
    }

    @Test
    fun `tilted horizon surfaces a framing suggestion with no target control`() {
        val assessment = baselineAssessment().copy(horizonTiltDegrees = 10.0)
        engine.evaluate(assessment, SceneTag.GENERAL, 1.0)
        clock.advanceBy(150)
        val result = engine.evaluate(assessment, SceneTag.GENERAL, 1.0)

        assertEquals(1, result.size)
        assertEquals(SuggestionCategory.FRAMING, result.first().category)
        assertEquals(null, result.first().targetControl)
    }

    @Test
    fun `small horizon tilt within tolerance does not trigger a framing suggestion`() {
        val assessment = baselineAssessment().copy(horizonTiltDegrees = 1.5)
        engine.evaluate(assessment, SceneTag.GENERAL, 1.0)
        clock.advanceBy(150)
        val result = engine.evaluate(assessment, SceneTag.GENERAL, 1.0)

        assertTrue(result.none { it.category == SuggestionCategory.FRAMING })
    }
}
