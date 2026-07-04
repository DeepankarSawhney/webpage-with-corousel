package com.deepankarsawhney.cameraadvisor.core.heuristics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhiteBalanceAnalyzerTest {

    @Test
    fun `neutral gray-world average reports ratios near 1`() {
        val result = WhiteBalanceAnalyzer.analyze(avgRed = 120.0, avgGreen = 120.0, avgBlue = 120.0)
        assertEquals(1.0, result.redGreenRatio, 0.001)
        assertEquals(1.0, result.blueGreenRatio, 0.001)
    }

    @Test
    fun `warm cast reports red-green ratio above 1`() {
        val result = WhiteBalanceAnalyzer.analyze(avgRed = 156.0, avgGreen = 120.0, avgBlue = 90.0)
        assertTrue(result.redGreenRatio > 1.2)
        assertTrue(result.blueGreenRatio < 1.0)
    }

    @Test
    fun `cool cast reports blue-green ratio above 1`() {
        val result = WhiteBalanceAnalyzer.analyze(avgRed = 90.0, avgGreen = 120.0, avgBlue = 156.0)
        assertTrue(result.blueGreenRatio > 1.2)
        assertTrue(result.redGreenRatio < 1.0)
    }
}
