package com.deepankarsawhney.cameraadvisor.camera

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import com.deepankarsawhney.cameraadvisor.core.domain.AfState
import com.deepankarsawhney.cameraadvisor.core.domain.FrameMetrics

/** Maps a Camera2 CaptureResult into the plain-Kotlin FrameMetrics consumed by the core module. */
object CameraMetadataMapper {

    private const val DEFAULT_ISO = 100
    private const val DEFAULT_EXPOSURE_TIME_NANOS = 16_666_667L // 1/60s

    fun map(result: CaptureResult): FrameMetrics {
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: DEFAULT_ISO
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: DEFAULT_EXPOSURE_TIME_NANOS
        val awbState = result.get(CaptureResult.CONTROL_AWB_STATE)

        return FrameMetrics(
            isoSensitivity = iso,
            exposureTimeNanos = exposureTime,
            afState = mapAfState(result.get(CaptureResult.CONTROL_AF_STATE)),
            awbLocked = awbState == CameraMetadata.CONTROL_AWB_STATE_LOCKED,
        )
    }

    private fun mapAfState(state: Int?): AfState = when (state) {
        CameraMetadata.CONTROL_AF_STATE_INACTIVE -> AfState.INACTIVE
        CameraMetadata.CONTROL_AF_STATE_PASSIVE_SCAN,
        CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN,
        -> AfState.SCANNING
        CameraMetadata.CONTROL_AF_STATE_PASSIVE_FOCUSED,
        CameraMetadata.CONTROL_AF_STATE_PASSIVE_UNFOCUSED,
        -> AfState.FOCUSED
        CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED -> AfState.FOCUS_LOCKED
        CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> AfState.NOT_FOCUS_LOCKED
        else -> AfState.INACTIVE
    }
}
