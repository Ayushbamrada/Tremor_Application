package com.example.yourcare.ui.viewmodel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yourcare.utils.FftHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt


// ... (TremorData and TremorMetrics data classes remain the same) ...
data class TremorMetrics(
    val averageRms: Float,
    val peakAmplitude: Float,
    val minAmplitude: Float,
    val frequency: Float
)

data class TremorData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = 0f
)

class TestViewModel : ViewModel(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private val _currentData = MutableStateFlow(TremorData())
    val currentData = _currentData.asStateFlow()
    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    // --- NEW: Public lists for the PDF Report ---
    val rawX = mutableListOf<Float>()
    val rawY = mutableListOf<Float>()
    val rawZ = mutableListOf<Float>()
    // --------------------------------------------

    private val recordedMagnitudes = mutableListOf<Float>()
    private var testStartTime: Long = 0

    fun startTest(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (gyro != null) {
            // Clear previous history
            recordedMagnitudes.clear()

            rawX.clear()
            rawY.clear()
            rawZ.clear()

            testStartTime = System.currentTimeMillis()
            _isTesting.value = true
            sensorManager?.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopTest(): TremorMetrics {
        _isTesting.value = false
        sensorManager?.unregisterListener(this)

        if (recordedMagnitudes.isEmpty()) return TremorMetrics(0f, 0f, 0f, 0f)

        val maxTremor = recordedMagnitudes.maxOrNull() ?: 0f
        val minTremor = recordedMagnitudes.minOrNull() ?: 0f
        val sumSquares = recordedMagnitudes.sumOf { (it * it).toDouble() }
        val averageRMS = sqrt(sumSquares / recordedMagnitudes.size).toFloat()

        val totalTimeSeconds = (System.currentTimeMillis() - testStartTime) / 1000f
        val sampleRate = if (totalTimeSeconds > 0) recordedMagnitudes.size / totalTimeSeconds else 0f

        val dominantFrequency = FftHelper.calculateDominantFrequency(recordedMagnitudes, sampleRate)

        return TremorMetrics(averageRMS, maxTremor, minTremor, dominantFrequency)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (_isTesting.value) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                val magnitude = sqrt(x*x + y*y + z*z)

                viewModelScope.launch {
                    _currentData.emit(TremorData(x, y, z, magnitude))
                }

                // Save raw data for PDF
                recordedMagnitudes.add(magnitude)
                rawX.add(x)
                rawY.add(y)
                rawZ.add(z)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}