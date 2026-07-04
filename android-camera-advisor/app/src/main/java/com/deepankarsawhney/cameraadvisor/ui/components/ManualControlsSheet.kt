package com.deepankarsawhney.cameraadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControl
import com.deepankarsawhney.cameraadvisor.core.domain.ManualControlState

/**
 * Suggest-only manual controls: dragging a slider here is the only way a Camera2 manual control
 * actually changes — suggestions from the HUD only pre-focus a section and mark a target value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualControlsSheet(
    state: ManualControlState,
    jumpToControl: ManualControl?,
    isoRange: IntRange,
    exposureTimeRangeNanos: LongRange,
    exposureCompensationRange: IntRange,
    onIsoChanged: (Int) -> Unit,
    onShutterSpeedChanged: (Long) -> Unit,
    onExposureCompensationChanged: (Int) -> Unit,
    onFocusChanged: (Float) -> Unit,
    onFocusAuto: () -> Unit,
    onAwbLockChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            ControlSection(title = "ISO: ${state.iso}", highlighted = jumpToControl == ManualControl.ISO) {
                Slider(
                    value = state.iso.toFloat(),
                    valueRange = isoRange.first.toFloat()..isoRange.last.toFloat(),
                    onValueChange = { onIsoChanged(it.toInt()) },
                )
            }

            ControlSection(
                title = "Shutter speed: 1/${(1_000_000_000.0 / state.exposureTimeNanos).toInt()}s",
                highlighted = jumpToControl == ManualControl.SHUTTER_SPEED,
            ) {
                Slider(
                    value = state.exposureTimeNanos.toFloat(),
                    valueRange = exposureTimeRangeNanos.first.toFloat()..exposureTimeRangeNanos.last.toFloat(),
                    onValueChange = { onShutterSpeedChanged(it.toLong()) },
                )
            }

            ControlSection(
                title = "Exposure compensation: ${state.exposureCompensationSteps}",
                highlighted = jumpToControl == ManualControl.EXPOSURE_COMPENSATION,
            ) {
                Slider(
                    value = state.exposureCompensationSteps.toFloat(),
                    valueRange = exposureCompensationRange.first.toFloat()..exposureCompensationRange.last.toFloat(),
                    onValueChange = { onExposureCompensationChanged(it.toInt()) },
                )
            }

            ControlSection(title = "Focus", highlighted = jumpToControl == ManualControl.FOCUS) {
                Slider(
                    value = state.focusDistanceDiopters,
                    valueRange = 0f..10f,
                    onValueChange = { onFocusChanged(it) },
                )
                TextButton(onClick = onFocusAuto) { Text("Auto focus") }
            }

            ControlSection(title = "White balance", highlighted = jumpToControl == ManualControl.WHITE_BALANCE) {
                TextButton(onClick = { onAwbLockChanged(!state.whiteBalanceLocked) }) {
                    Text(if (state.whiteBalanceLocked) "Unlock white balance" else "Lock white balance")
                }
            }
        }
    }
}

@Composable
private fun ControlSection(title: String, highlighted: Boolean, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        content()
    }
}
