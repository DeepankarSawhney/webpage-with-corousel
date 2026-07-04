package com.deepankarsawhney.cameraadvisor.core.util

fun interface Clock {
    fun nowMillis(): Long

    companion object {
        val SYSTEM = Clock { System.currentTimeMillis() }
    }
}
