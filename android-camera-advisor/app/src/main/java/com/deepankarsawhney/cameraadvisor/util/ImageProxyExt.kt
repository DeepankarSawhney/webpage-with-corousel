package com.deepankarsawhney.cameraadvisor.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

data class ExtractedFrame(
    val luma: IntArray,
    val width: Int,
    val height: Int,
    val avgRed: Double,
    val avgGreen: Double,
    val avgBlue: Double,
)

private const val DEFAULT_TARGET_WIDTH = 160
private const val DEFAULT_TARGET_HEIGHT = 120

/**
 * Downsamples a YUV_420_888 frame to a small luma buffer (for the heuristic analyzers) plus an
 * average RGB estimate (for white-balance), keeping per-frame analysis cost low.
 */
fun ImageProxy.extractDownsampled(
    targetWidth: Int = DEFAULT_TARGET_WIDTH,
    targetHeight: Int = DEFAULT_TARGET_HEIGHT,
): ExtractedFrame {
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val outW = minOf(targetWidth, width)
    val outH = minOf(targetHeight, height)
    val luma = IntArray(outW * outH)

    var sumR = 0.0
    var sumG = 0.0
    var sumB = 0.0

    for (oy in 0 until outH) {
        val sy = oy * height / outH
        for (ox in 0 until outW) {
            val sx = ox * width / outW
            val yValue = yPlane.buffer.get(sy * yPlane.rowStride + sx * yPlane.pixelStride).toInt() and 0xFF
            luma[oy * outW + ox] = yValue

            val cx = sx / 2
            val cy = sy / 2
            val uValue = (uPlane.buffer.get(cy * uPlane.rowStride + cx * uPlane.pixelStride).toInt() and 0xFF) - 128
            val vValue = (vPlane.buffer.get(cy * vPlane.rowStride + cx * vPlane.pixelStride).toInt() and 0xFF) - 128

            sumR += (yValue + 1.402 * vValue).coerceIn(0.0, 255.0)
            sumG += (yValue - 0.344136 * uValue - 0.714136 * vValue).coerceIn(0.0, 255.0)
            sumB += (yValue + 1.772 * uValue).coerceIn(0.0, 255.0)
        }
    }

    val n = (outW * outH).toDouble()
    return ExtractedFrame(luma, outW, outH, sumR / n, sumG / n, sumB / n)
}

/** Full-resolution JPEG round-trip, used only for the throttled (~2Hz) scene-classifier input. */
fun ImageProxy.toJpegBitmap(quality: Int = 90): Bitmap {
    val nv21 = toNv21()
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, out)
    val bytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun ImageProxy.toNv21(): ByteArray {
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val nv21 = ByteArray(width * height + 2 * (width / 2) * (height / 2))

    var pos = 0
    val yBuffer = yPlane.buffer.duplicate()
    for (row in 0 until height) {
        yBuffer.position(row * yPlane.rowStride)
        yBuffer.get(nv21, pos, width)
        pos += width
    }

    val chromaHeight = height / 2
    val chromaWidth = width / 2
    val uBuffer = uPlane.buffer.duplicate()
    val vBuffer = vPlane.buffer.duplicate()
    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {
            val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
            val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
            nv21[pos++] = vBuffer.get(vIndex)
            nv21[pos++] = uBuffer.get(uIndex)
        }
    }

    return nv21
}
