package com.deepankarsawhney.cameraadvisor.core.heuristics

import org.junit.Assert.assertTrue
import org.junit.Test

class SharpnessAnalyzerTest {

    private val width = 32
    private val height = 32

    @Test
    fun `checkerboard pattern scores much higher than a smooth gradient`() {
        val checkerboard = IntArray(width * height) { i ->
            val x = i % width
            val y = i / width
            if ((x + y) % 2 == 0) 0 else 255
        }
        val smoothGradient = IntArray(width * height) { i ->
            val x = i % width
            (x * 255 / width)
        }

        val sharpVariance = SharpnessAnalyzer.laplacianVariance(checkerboard, width, height)
        val smoothVariance = SharpnessAnalyzer.laplacianVariance(smoothGradient, width, height)

        assertTrue(
            "expected checkerboard variance ($sharpVariance) to be much greater than smooth gradient ($smoothVariance)",
            sharpVariance > smoothVariance * 10,
        )
    }

    @Test
    fun `flat frame has zero laplacian variance`() {
        val flat = IntArray(width * height) { 128 }
        val variance = SharpnessAnalyzer.laplacianVariance(flat, width, height)
        assertTrue(variance == 0.0)
    }
}
