package com.deepankarsawhney.cameraadvisor.analysis.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.deepankarsawhney.cameraadvisor.core.ml.SceneTag
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * On-device TFLite scene classifier. See ml-model/README.md — the model asset isn't bundled in
 * this repo, so this degrades to always reporting SceneTag.GENERAL at zero confidence until one
 * is added at app/src/main/assets/scene_classifier.tflite.
 */
class SceneClassifier(context: Context) {

    data class Result(val sceneTag: SceneTag, val confidence: Double)

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD))
        .build()

    init {
        interpreter = try {
            buildInterpreter(loadModelFile(context))
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "$MODEL_ASSET_NAME not found in assets — scene tagging disabled, see ml-model/README.md")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load scene classifier model", e)
            null
        }
    }

    fun classify(bitmap: Bitmap): Result {
        val model = interpreter ?: return Result(SceneTag.GENERAL, 0.0)
        return try {
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
            val output = Array(1) { FloatArray(LABELS.size) }
            model.run(tensorImage.buffer, output)

            val scores = output[0]
            var bestIndex = 0
            for (i in scores.indices) if (scores[i] > scores[bestIndex]) bestIndex = i
            Result(mapLabelToSceneTag(LABELS[bestIndex]), scores[bestIndex].toDouble())
        } catch (e: Exception) {
            Log.e(TAG, "Scene classification failed for this frame", e)
            Result(SceneTag.GENERAL, 0.0)
        }
    }

    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
    }

    private fun buildInterpreter(model: MappedByteBuffer): Interpreter {
        val options = Interpreter.Options()
        try {
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                gpuDelegate = delegate
                options.addDelegate(delegate)
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPU delegate unavailable, falling back to CPU (XNNPACK)", e)
        }
        return Interpreter(model, options)
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_ASSET_NAME)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { input ->
            val fileChannel = input.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength,
            )
        }
    }

    private fun mapLabelToSceneTag(label: String): SceneTag = when {
        label.contains("beach") || label.contains("mountain") ||
            label.contains("forest") || label.contains("field") -> SceneTag.LANDSCAPE
        label.contains("food") || label.contains("flower") || label.contains("closeup") -> SceneTag.MACRO
        label.contains("sport") || label.contains("race") || label.contains("motion") -> SceneTag.ACTION
        label.contains("night") || label.contains("dark") -> SceneTag.LOW_LIGHT
        else -> SceneTag.GENERAL
    }

    companion object {
        private const val TAG = "SceneClassifier"
        private const val MODEL_ASSET_NAME = "scene_classifier.tflite"
        private const val INPUT_SIZE = 224
        private const val NORMALIZE_MEAN = 127.5f
        private const val NORMALIZE_STD = 127.5f

        // Placeholder — replace with the bundled model's actual output labels, in output-tensor
        // order, once a real scene_classifier.tflite is added (see ml-model/README.md).
        private val LABELS = listOf("general")
    }
}
