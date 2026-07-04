package com.deepankarsawhney.cameraadvisor.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Rolling RMS of gyroscope angular-velocity magnitude over a short window, normalized to an
 * approximate 0..1 hand-shake score. Runs entirely independent of the camera frame pipeline.
 */
class MotionSensorMonitor(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val samples = ArrayDeque<Pair<Long, Double>>()

    @Volatile
    var shakeScore: Double = 0.0
        private set

    val isAvailable: Boolean get() = gyroscope != null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val magnitude = sqrt(
                (event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]).toDouble(),
            )
            val now = System.currentTimeMillis()
            samples.addLast(now to magnitude)
            while (samples.isNotEmpty() && now - samples.first().first > WINDOW_MILLIS) {
                samples.removeFirst()
            }
            val rms = sqrt(samples.sumOf { it.second * it.second } / samples.size)
            // Sustained handheld shake typically produces gyroscope RMS in the ~0.5-3 rad/s range;
            // normalize so ~2 rad/s reads as fully "shaking" (1.0).
            shakeScore = (rms / NORMALIZATION_RAD_PER_SEC).coerceIn(0.0, 1.0)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        gyroscope?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
        samples.clear()
        shakeScore = 0.0
    }

    private companion object {
        const val WINDOW_MILLIS = 400L
        const val NORMALIZATION_RAD_PER_SEC = 2.0
    }
}
