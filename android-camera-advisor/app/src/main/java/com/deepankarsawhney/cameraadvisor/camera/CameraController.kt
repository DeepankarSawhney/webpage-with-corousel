package com.deepankarsawhney.cameraadvisor.camera

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Owns the CameraX use-case binding (Preview + ImageAnalysis + ImageCapture) and the
 * Camera2Interop bridge used both to read live CaptureResult metadata and to issue manual
 * control changes (ISO, shutter speed, focus distance, white balance, exposure compensation).
 */
class CameraController(private val context: Context) {

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var camera: Camera? = null
    private var camera2CameraControl: Camera2CameraControl? = null
    private var imageCapture: ImageCapture? = null

    // Camera2CameraControl.captureRequestOptions REPLACES the whole bundle on every assignment
    // rather than merging, so this tracks the accumulated set of manual options ourselves —
    // otherwise e.g. setting shutter speed alone would silently wipe out a previously-set ISO.
    private var activeOptions: CaptureRequestOptions = CaptureRequestOptions.Builder().build()

    @Volatile
    var latestCaptureResult: CaptureResult? = null
        private set

    fun bindToLifecycle(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
    ) {
        val previewBuilder = Preview.Builder()
        val analysisBuilder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        val captureBuilder = ImageCapture.Builder()

        Camera2Interop.Extender(analysisBuilder).setSessionCaptureCallback(
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    latestCaptureResult = result
                }
            },
        )

        val preview = previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }
        val imageAnalysis = analysisBuilder.build().also { it.setAnalyzer(analysisExecutor, analyzer) }
        val capture = captureBuilder.build()
        imageCapture = capture

        cameraProvider.unbindAll()
        val boundCamera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis,
            capture,
        )
        camera = boundCamera
        camera2CameraControl = Camera2CameraControl.from(boundCamera.cameraControl)
        activeOptions = CaptureRequestOptions.Builder().build()
    }

    fun <T> getCharacteristic(key: CameraCharacteristics.Key<T>): T? {
        val currentCamera = camera ?: return null
        return Camera2CameraInfo.from(currentCamera.cameraInfo).getCameraCharacteristic(key)
    }

    fun capturePhoto(onSaved: (Uri) -> Unit, onFailure: (Exception) -> Unit) {
        val capture = imageCapture
        if (capture == null) {
            onFailure(IllegalStateException("Camera is not bound yet"))
            return
        }
        CaptureRepository.capture(context, capture, onSaved, onFailure)
    }

    fun setManualIso(iso: Int) {
        applyOptions {
            setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
        }
    }

    fun setManualExposureTime(nanos: Long) {
        applyOptions {
            setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, nanos)
        }
    }

    fun setAutoExposure() {
        applyOptions {
            setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            clearCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY)
            clearCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME)
        }
    }

    fun setExposureCompensationSteps(steps: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(steps)
    }

    fun setManualFocusDistance(diopters: Float) {
        applyOptions {
            setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, diopters)
        }
    }

    fun setAutoFocus() {
        applyOptions {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
            )
            clearCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE)
        }
    }

    fun setWhiteBalancePreset(awbMode: Int) {
        applyOptions {
            setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, false)
            setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, awbMode)
        }
    }

    fun setAwbLocked(locked: Boolean) {
        applyOptions {
            setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, locked)
        }
    }

    /**
     * Applies [block] on top of the previously accumulated manual options (not just the keys it
     * touches) — Camera2CameraControl.captureRequestOptions replaces the whole bundle on every
     * assignment, so every setter must always resend every manual key still in effect.
     */
    private fun applyOptions(block: CaptureRequestOptions.Builder.() -> Unit) {
        val builder = CaptureRequestOptions.Builder.from(activeOptions)
        builder.block()
        val newOptions = builder.build()
        activeOptions = newOptions
        camera2CameraControl?.captureRequestOptions = newOptions
    }

    fun unbind(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        camera = null
        camera2CameraControl = null
        imageCapture = null
        activeOptions = CaptureRequestOptions.Builder().build()
    }

    fun shutdown() {
        analysisExecutor.shutdown()
    }
}
