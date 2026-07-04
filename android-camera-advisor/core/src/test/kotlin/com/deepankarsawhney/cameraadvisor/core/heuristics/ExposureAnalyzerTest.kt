package com.deepankarsawhney.cameraadvisor.core.heuristics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExposureAnalyzerTest {

    @Test
    fun `all-bright frame reports high mean luma and highlight clipping`() {
        val luma = IntArray(1000) { 255 }
        val result = ExposureAnalyzer.analyze(luma)
        assertEquals(255.0, result.meanLuma, 0.001)
        assertEquals(1.0, result.highlightClipFraction, 0.001)
        assertEquals(0.0, result.shadowClipFraction, 0.001)
    }

    @Test
    fun `all-dark frame reports low mean luma and shadow clipping`() {
        val luma = IntArray(1000) { 0 }
        val result = ExposureAnalyzer.analyze(luma)
        assertEquals(0.0, result.meanLuma, 0.001)
        assertEquals(1.0, result.shadowClipFraction, 0.001)
        assertEquals(0.0, result.highlightClipFraction, 0.001)
    }

    @Test
    fun `bimodal frame splits clipping between shadows and highlights`() {
        val luma = IntArray(1000) { i -> if (i % 2 == 0) 0 else 255 }
        val result = ExposureAnalyzer.analyze(luma)
        assertEquals(127.5, result.meanLuma, 0.001)
        assertEquals(0.5, result.shadowClipFraction, 0.001)
        assertEquals(0.5, result.highlightClipFraction, 0.001)
    }

    @Test
    fun `well-exposed midtone frame reports no clipping`() {
        val luma = IntArray(1000) { 128 }
        val result = ExposureAnalyzer.analyze(luma)
        assertTrue(result.shadowClipFraction == 0.0)
        assertTrue(result.highlightClipFraction == 0.0)
    }
}
