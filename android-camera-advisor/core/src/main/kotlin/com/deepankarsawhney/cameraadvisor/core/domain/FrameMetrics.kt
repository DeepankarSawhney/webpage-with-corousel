package com.deepankarsawhney.cameraadvisor.core.domain

enum class AfState {
    INACTIVE,
    SCANNING,
    FOCUSED,
    FOCUS_LOCKED,
    NOT_FOCUS_LOCKED,
}

/** Camera2 CaptureResult fields for the current frame, mapped into plain Kotlin types. */
data class FrameMetrics(
    val isoSensitivity: Int,
    val exposureTimeNanos: Long,
    val afState: AfState,
    val awbLocked: Boolean,
)
