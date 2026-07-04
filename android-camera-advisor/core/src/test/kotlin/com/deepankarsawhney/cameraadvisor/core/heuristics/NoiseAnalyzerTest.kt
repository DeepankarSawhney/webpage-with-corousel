package com.deepankarsawhney.cameraadvisor.core.heuristics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseAnalyzerTest {

    private val size = 8 // exactly one analysis block

    @Test
    fun `perfectly flat block has zero noise variance`() {
        val flat = IntArray(size * size) { 120 }
        val variance = NoiseAnalyzer.flatRegionNoiseVariance(flat, size, size)
        assertEquals(0.0, variance, 0.0001)
    }

    @Test
    fun `flat block with small grain reports nonzero noise variance`() {
        val noisy = IntArray(size * size) { i ->
            val x = i % size
            val y = i / size
            120 + (((x * 7 + y * 13) % 5) - 2)
        }
        val variance = NoiseAnalyzer.flatRegionNoiseVariance(noisy, size, size)
        assertTrue("expected nonzero variance for grainy flat block, got $variance", variance > 0.0)
    }

    @Test
    fun `steep gradient block is excluded as non-flat and reports zero`() {
        val gradient = IntArray(size * size) { i ->
            val x = i % size
            x * 36
        }
        val variance = NoiseAnalyzer.flatRegionNoiseVariance(gradient, size, size)
        assertEquals(0.0, variance, 0.0001)
    }
}
