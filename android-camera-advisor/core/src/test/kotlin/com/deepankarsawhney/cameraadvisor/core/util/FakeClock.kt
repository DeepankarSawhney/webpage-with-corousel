package com.deepankarsawhney.cameraadvisor.core.util

class FakeClock(private var currentMillis: Long = 0L) : Clock {
    override fun nowMillis(): Long = currentMillis

    fun advanceBy(millis: Long) {
        currentMillis += millis
    }
}
