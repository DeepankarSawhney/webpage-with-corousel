package com.deepankarsawhney.cameraadvisor.core.heuristics

/** Gray-world color-cast estimate from average per-channel intensities of a downsampled frame. */
object WhiteBalanceAnalyzer {

    data class Result(
        val redGreenRatio: Double,
        val blueGreenRatio: Double,
    )

    fun analyze(avgRed: Double, avgGreen: Double, avgBlue: Double): Result {
        val safeGreen = if (avgGreen <= 0.0) 1.0 else avgGreen
        return Result(
            redGreenRatio = avgRed / safeGreen,
            blueGreenRatio = avgBlue / safeGreen,
        )
    }
}
