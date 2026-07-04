package com.deepankarsawhney.cameraadvisor.core.heuristics

/** Focus-quality proxy: variance of the Laplacian over a Y-plane (0..255) buffer. */
object SharpnessAnalyzer {

    fun laplacianVariance(luma: IntArray, width: Int, height: Int): Double {
        require(luma.size == width * height) { "luma buffer size must equal width*height" }
        if (width < 3 || height < 3) return 0.0

        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            val rowAbove = (y - 1) * width
            val row = y * width
            val rowBelow = (y + 1) * width
            for (x in 1 until width - 1) {
                val center = luma[row + x]
                val laplacian = (
                    luma[rowAbove + x] + luma[rowBelow + x] +
                        luma[row + x - 1] + luma[row + x + 1] - 4 * center
                    ).toDouble()
                sum += laplacian
                sumSq += laplacian * laplacian
                count++
            }
        }
        if (count == 0) return 0.0
        val mean = sum / count
        return (sumSq / count) - mean * mean
    }
}
