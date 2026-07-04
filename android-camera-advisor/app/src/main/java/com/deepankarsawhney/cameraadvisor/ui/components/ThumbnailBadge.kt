package com.deepankarsawhney.cameraadvisor.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ThumbnailBadge(lastPhotoUri: Uri?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var thumbnail by remember(lastPhotoUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(lastPhotoUri) {
        val uri = lastPhotoUri ?: return@LaunchedEffect
        thumbnail = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(96, 96), null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(enabled = lastPhotoUri != null) {
                lastPhotoUri?.let { uri ->
                    val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "image/*") }
                    context.startActivity(intent)
                }
            },
    ) {
        thumbnail?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}
