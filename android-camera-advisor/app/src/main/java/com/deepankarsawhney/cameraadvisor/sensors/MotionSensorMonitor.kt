package com.deepankarsawhney.cameraadvisor.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Two independent motion signals, both running off-camera-pipeline:
 * - A rolling RMS of gyroscope angular-velocity magnitude, normalized to a 0..1 hand-shake score.
 * - An accelerometer-derived device roll angle (degrees from level), for horizon-tilt framing tips.
 */
class MotionSensorMonitor(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val samples = ArrayDeque<Pair<Long, Double>>()

    @Volatile
    var shakeScore: Double = 0.0
        private set

    @Volatile
    var horizonTiltDegrees: Double = 0.0
        private set

    val isAvailable: Boolean get() = gyroscope != null

    private val gyroscopeListener = object : SensorEventListener {
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

    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // Held upright in portrait, pointed roughly level: gravity's X component grows as the
            // phone rolls left/right, its Y component shrinks — atan2 gives the roll angle in degrees.
            val ax = event.values[0].toDouble()
            val ay = event.values[1].toDouble()
            horizonTiltDegrees = Math.toDegrees(atan2(ax, ay))
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        gyroscope?.let { sensorManager.registerListener(gyroscopeListener, it, SensorManager.SENSOR_DELAY_GAME) }
        accelerometer?.let { sensorManager.registerListener(accelerometerListener, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() {
        sensorManager.unregisterListener(gyroscopeListener)
        sensorManager.unregisterListener(accelerometerListener)
        samples.clear()
        shakeScore = 0.0
        horizonTiltDegrees = 0.0
    }

    private companion object {
        const val WINDOW_MILLIS = 400L
        const val NORMALIZATION_RAD_PER_SEC = 2.0
    }
}
