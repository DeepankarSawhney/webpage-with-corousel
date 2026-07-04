package com.deepankarsawhney.cameraadvisor.viewmodel

import android.net.Uri
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControlState
import com.deepankarsawhney.cameraadvisor.core.ml.SceneTag
import com.deepankarsawhney.cameraadvisor.core.suggestion.Suggestion

data class CameraUiState(
    val suggestions: List<Suggestion> = emptyList(),
    val sceneTag: SceneTag = SceneTag.GENERAL,
    val manualControlState: ManualControlState = ManualControlState(
        isoAuto = true,
        iso = 100,
        shutterSpeedAuto = true,
        exposureTimeNanos = 16_666_667L,
        whiteBalanceAuto = true,
        whiteBalanceLocked = false,
        colorTemperatureK = 5500,
        focusAuto = true,
        focusDistanceDiopters = 0f,
        exposureCompensationSteps = 0,
    ),
    val lastPhotoUri: Uri? = null,
    val jumpToControl: ManualControl? = null,
)
