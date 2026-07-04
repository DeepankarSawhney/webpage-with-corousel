package com.deepankarsawhney.cameraadvisor.viewmodel

import android.app.Application
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.deepankarsawhney.cameraadvisor.analysis.ml.SceneClassifier
import com.deepankarsawhney.cameraadvisor.camera.CameraController
import com.deepankarsawhney.cameraadvisor.camera.CameraMetadataMapper
import com.deepankarsawhney.cameraadvisor.camera.FrameAnalyzer
import com.deepankarsawhney.cameraadvisor.core.domain.AfState
import com.deepankarsawhney.cameraadvisor.core.domain.FrameAssessment
import com.deepankarsawhney.cameraadvisor.core.domain.FrameMetrics
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl
import com.deepankarsawhney.cameraadvisor.core.ml.SceneTag
import com.deepankarsawhney.cameraadvisor.core.suggestion.SuggestionEngine
import com.deepankarsawhney.cameraadvisor.sensors.MotionSensorMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraController = CameraController(application)
    private val motionSensorMonitor = MotionSensorMonitor(application)
    private val sceneClassifier = SceneClassifier(application)
    private val suggestionEngine = SuggestionEngine()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val analyzer = FrameAnalyzer(
        sceneClassifier = sceneClassifier,
        currentMetrics = ::readLatestFrameMetrics,
        currentShakeScore = { motionSensorMonitor.shakeScore },
        onAssessment = ::onFrameAssessment,
    )

    init {
        motionSensorMonitor.start()
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(getApplication())
        providerFuture.addListener(
            {
                cameraController.bindToLifecycle(providerFuture.get(), lifecycleOwner, previewView, analyzer)
            },
            ContextCompat.getMainExecutor(getApplication()),
        )
    }

    private fun readLatestFrameMetrics(): FrameMetrics {
        val result = cameraController.latestCaptureResult
            ?: return FrameMetrics(
                isoSensitivity = 100,
                exposureTimeNanos = 16_666_667L,
                afState = AfState.INACTIVE,
                awbLocked = false,
            )
        return CameraMetadataMapper.map(result)
    }

    private fun onFrameAssessment(assessment: FrameAssessment, sceneTag: SceneTag, confidence: Double) {
        val suggestions = suggestionEngine.evaluate(assessment, sceneTag, confidence)
        _uiState.update { it.copy(suggestions = suggestions, sceneTag = suggestionEngine.currentSceneTag) }
    }

    fun onCapture() {
        cameraController.capturePhoto(
            onSaved = { uri -> _uiState.update { it.copy(lastPhotoUri = uri) } },
            onFailure = { e -> Log.e(TAG, "Failed to capture photo", e) },
        )
    }

    fun onSuggestionTapped(control: ManualControl) {
        _uiState.update { it.copy(jumpToControl = control) }
    }

    fun onManualControlsSheetDismissed() {
        _uiState.update { it.copy(jumpToControl = null) }
    }

    fun setIso(iso: Int) {
        cameraController.setManualIso(iso)
        _uiState.update { it.copy(manualControlState = it.manualControlState.copy(isoAuto = false, iso = iso)) }
    }

    fun setExposureAuto() {
        cameraController.setAutoExposure()
        _uiState.update {
            it.copy(manualControlState = it.manualControlState.copy(isoAuto = true, shutterSpeedAuto = true))
        }
    }

    fun setShutterSpeed(exposureTimeNanos: Long) {
        cameraController.setManualExposureTime(exposureTimeNanos)
        _uiState.update {
            it.copy(
                manualControlState = it.manualControlState.copy(
                    shutterSpeedAuto = false,
                    exposureTimeNanos = exposureTimeNanos,
                ),
            )
        }
    }

    fun setFocusDistance(diopters: Float) {
        cameraController.setManualFocusDistance(diopters)
        _uiState.update {
            it.copy(
                manualControlState = it.manualControlState.copy(
                    focusAuto = false,
                    focusDistanceDiopters = diopters,
                ),
            )
        }
    }

    fun setFocusAuto() {
        cameraController.setAutoFocus()
        _uiState.update { it.copy(manualControlState = it.manualControlState.copy(focusAuto = true)) }
    }

    fun setExposureCompensationSteps(steps: Int) {
        cameraController.setExposureCompensationSteps(steps)
        _uiState.update {
            it.copy(manualControlState = it.manualControlState.copy(exposureCompensationSteps = steps))
        }
    }

    fun setWhiteBalancePreset(awbMode: Int) {
        cameraController.setWhiteBalancePreset(awbMode)
        _uiState.update {
            it.copy(manualControlState = it.manualControlState.copy(whiteBalanceAuto = false, whiteBalanceLocked = false))
        }
    }

    fun setAwbLocked(locked: Boolean) {
        cameraController.setAwbLocked(locked)
        _uiState.update {
            it.copy(manualControlState = it.manualControlState.copy(whiteBalanceLocked = locked))
        }
    }

    override fun onCleared() {
        motionSensorMonitor.stop()
        sceneClassifier.close()
        cameraController.shutdown()
        super.onCleared()
    }

    private companion object {
        const val TAG = "CameraViewModel"
    }
}
