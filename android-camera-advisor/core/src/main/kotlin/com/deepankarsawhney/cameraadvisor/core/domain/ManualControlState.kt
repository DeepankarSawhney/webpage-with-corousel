package com.deepankarsawhney.cameraadvisor.core.domain

enum class ManualControl {
    ISO,
    SHUTTER_SPEED,
    WHITE_BALANCE,
    FOCUS,
    EXPOSURE_COMPENSATION,
}

/** Current state of each manual control, as reflected in the ManualControlsSheet UI. */
data class ManualControlState(
    val isoAuto: Boolean,
    val iso: Int,
    val shutterSpeedAuto: Boolean,
    val exposureTimeNanos: Long,
    val whiteBalanceAuto: Boolean,
    val whiteBalanceLocked: Boolean,
    val colorTemperatureK: Int,
    val focusAuto: Boolean,
    val focusDistanceDiopters: Float,
    val exposureCompensationSteps: Int,
)
