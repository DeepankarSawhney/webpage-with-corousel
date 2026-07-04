package com.deepankarsawhney.cameraadvisor.core.heuristics

import kotlin.math.abs

/**
 * Estimates visible sensor noise by measuring pixel variance inside visually "flat" blocks
 * (blocks with low gradient energy, where any variance present is likely grain rather than detail).
 * Ground-truth ISO itself comes straight from Camera2 CaptureResult metadata, not from this analyzer.
 */
object NoiseAnalyzer {

    private const val BLOCK_SIZE = 8
    private const val FLATNESS_GRADIENT_THRESHOLD = 6.0

    fun flatRegionNoiseVariance(luma: IntArray, width: Int, height: Int): Double {
        require(luma.size == width * height) { "luma buffer size must equal width*height" }
        if (width < BLOCK_SIZE || height < BLOCK_SIZE) return 0.0

        var totalVariance = 0.0
        var flatBlockCount = 0

        var by = 0
        while (by + BLOCK_SIZE <= height) {
            var bx = 0
            while (bx + BLOCK_SIZE <= width) {
                val block = IntArray(BLOCK_SIZE * BLOCK_SIZE)
                var idx = 0
                for (y in by until by + BLOCK_SIZE) {
                    val row = y * width
                    for (x in bx until bx + BLOCK_SIZE) {
                        block[idx++] = luma[row + x]
                    }
                }
                val gradientEnergy = averageGradientEnergy(block, BLOCK_SIZE)
                if (gradientEnergy < FLATNESS_GRADIENT_THRESHOLD) {
                    totalVariance += variance(block)
                    flatBlockCount++
                }
                bx += BLOCK_SIZE
            }
            by += BLOCK_SIZE
        }

        return if (flatBlockCount == 0) 0.0 else totalVariance / flatBlockCount
    }

    private fun averageGradientEnergy(block: IntArray, size: Int): Double {
        var sum = 0.0
        var count = 0
        for (y in 0 until size - 1) {
            for (x in 0 until size - 1) {
                val i = y * size + x
                sum += abs(block[i + 1] - block[i]) + abs(block[i + size] - block[i])
                count++
            }
        }
        return if (count == 0) 0.0 else sum / count
    }

    private fun variance(block: IntArray): Double {
        val mean = block.sumOf { it.toLong() }.toDouble() / block.size
        val sumSq = block.sumOf { (it - mean) * (it - mean) }
        return sumSq / block.size
    }
}
