package com.deepankarsawhney.cameraadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.deepankarsawhney.cameraadvisor.ui.CameraScreen
import com.deepankarsawhney.cameraadvisor.ui.permissions.PermissionGate
import com.deepankarsawhney.cameraadvisor.ui.theme.CameraAdvisorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CameraAdvisorTheme {
                PermissionGate {
                    CameraScreen()
                }
            }
        }
    }
}
