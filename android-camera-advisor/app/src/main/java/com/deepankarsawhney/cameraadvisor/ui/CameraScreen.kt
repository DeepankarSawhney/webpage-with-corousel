package com.deepankarsawhney.cameraadvisor.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl
import com.deepankarsawhney.cameraadvisor.ui.components.ManualControlsSheet
import com.deepankarsawhney.cameraadvisor.ui.components.RuleOfThirdsOverlay
import com.deepankarsawhney.cameraadvisor.ui.components.SceneBadge
import com.deepankarsawhney.cameraadvisor.ui.components.ShutterButton
import com.deepankarsawhney.cameraadvisor.ui.components.SuggestionHud
import com.deepankarsawhney.cameraadvisor.ui.components.ThumbnailBadge
import com.deepankarsawhney.cameraadvisor.ui.components.TopToolbar
import com.deepankarsawhney.cameraadvisor.ui.components.readinessColor
import com.deepankarsawhney.cameraadvisor.viewmodel.CameraViewModel

@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context).also { previewView ->
                    viewModel.bindCamera(lifecycleOwner, previewView)
                }
            },
        )

        if (uiState.gridVisible) {
            RuleOfThirdsOverlay(modifier = Modifier.fillMaxSize())
        }

        TopToolbar(
            flashMode = uiState.flashMode,
            gridVisible = uiState.gridVisible,
            onCycleFlash = viewModel::cycleFlashMode,
            onToggleGrid = viewModel::toggleGrid,
            onOpenManualControls = { viewModel.onSuggestionTapped(ManualControl.EXPOSURE_COMPENSATION) },
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
        )

        SceneBadge(
            sceneTag = uiState.sceneTag,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
        )

        SuggestionHud(
            suggestions = uiState.suggestions,
            onSuggestionTapped = viewModel::onSuggestionTapped,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp),
        )

        BottomControlBar(
            lastPhotoUri = uiState.lastPhotoUri,
            shutterRingColor = readinessColor(uiState.suggestions),
            onCapture = viewModel::onCapture,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (uiState.jumpToControl != null) {
            ManualControlsSheet(
                state = uiState.manualControlState,
                jumpToControl = uiState.jumpToControl,
                isoRange = ISO_RANGE,
                exposureTimeRangeNanos = EXPOSURE_TIME_RANGE_NANOS,
                exposureCompensationRange = EXPOSURE_COMPENSATION_RANGE,
                onIsoChanged = viewModel::setIso,
                onShutterSpeedChanged = viewModel::setShutterSpeed,
                onExposureCompensationChanged = viewModel::setExposureCompensationSteps,
                onFocusChanged = viewModel::setFocusDistance,
                onFocusAuto = viewModel::setFocusAuto,
                onAwbLockChanged = viewModel::setAwbLocked,
                onDismiss = viewModel::onManualControlsSheetDismissed,
            )
        }
    }
}

@Composable
private fun BottomControlBar(
    lastPhotoUri: android.net.Uri?,
    shutterRingColor: Color,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
    ) {
        ThumbnailBadge(lastPhotoUri = lastPhotoUri, modifier = Modifier.align(Alignment.CenterStart))
        ShutterButton(
            onClick = onCapture,
            ringColor = shutterRingColor,
            modifier = Modifier.align(Alignment.Center),
        )
        // Empty spacer mirroring the thumbnail's footprint so the shutter stays visually centered.
        Box(modifier = Modifier.size(48.dp).align(Alignment.CenterEnd))
    }
}

// Conservative fallback ranges (approximate Camera2 SENSOR_INFO_SENSITIVITY_RANGE /
// SENSOR_INFO_EXPOSURE_TIME_RANGE / CONTROL_AE_COMPENSATION_RANGE for a modern flagship sensor).
// TODO: read the real ranges from CameraController.getCharacteristic() once bound, and pass them
// down instead of these constants.
private val ISO_RANGE = 50..6400
private val EXPOSURE_TIME_RANGE_NANOS = 1_000_000L..1_000_000_000L
private val EXPOSURE_COMPENSATION_RANGE = -6..6
