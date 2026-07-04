package com.deepankarsawhney.cameraadvisor.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures a JPEG via CameraX ImageCapture and saves it into MediaStore using the
 * scoped-storage-correct ContentValues + IS_PENDING pattern — no WRITE_EXTERNAL_STORAGE needed.
 */
object CaptureRepository {

    private val RELATIVE_PATH = "${Environment.DIRECTORY_PICTURES}/CameraAdvisor"
    private val FILENAME_FORMAT = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)

    fun capture(
        context: Context,
        imageCapture: ImageCapture,
        onSaved: (Uri) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val name = "IMG_${FILENAME_FORMAT.format(Date())}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues,
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (savedUri == null) {
                        onFailure(IllegalStateException("No URI returned for saved photo"))
                        return
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val clearPending = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                        context.contentResolver.update(savedUri, clearPending, null, null)
                    }
                    onSaved(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onFailure(exception)
                }
            },
        )
    }
}
