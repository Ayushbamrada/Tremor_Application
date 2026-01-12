package com.example.yourcare.ui.viewmodel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

// Data class to hold live sensor values
data class TremorData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = 0f
)

// Data class to hold final results
data class TremorMetrics(
    val averageRms: Float,
    val peakAmplitude: Float,
    val minAmplitude: Float
)

class TestViewModel : ViewModel(), SensorEventListener {

    private var sensorManager: SensorManager? = null

    // Live stream of data for UI
    private val _currentData = MutableStateFlow(TremorData())
    val currentData = _currentData.asStateFlow()

    // Test Status
    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    // Store readings to calculate average later
    private val recordedMagnitudes = mutableListOf<Float>()

    fun startTest(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (gyro != null) {
            recordedMagnitudes.clear()
            _isTesting.value = true
            // SENSOR_DELAY_UI is sufficient for visual feedback
            sensorManager?.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopTest(): TremorMetrics {
        _isTesting.value = false
        sensorManager?.unregisterListener(this)

        if (recordedMagnitudes.isEmpty()) return TremorMetrics(0f, 0f, 0f)

        val maxTremor = recordedMagnitudes.maxOrNull() ?: 0f
        val minTremor = recordedMagnitudes.minOrNull() ?: 0f

        // Calculate RMS (Root Mean Square) for Average Vibration
        val sumSquares = recordedMagnitudes.sumOf { (it * it).toDouble() }
        val averageRMS = sqrt(sumSquares / recordedMagnitudes.size).toFloat()

        return TremorMetrics(averageRMS, maxTremor, minTremor)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (_isTesting.value) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                // Calculate total magnitude of rotation
                val magnitude = sqrt(x*x + y*y + z*z)

                // Update UI
                viewModelScope.launch {
                    _currentData.emit(TremorData(x, y, z, magnitude))
                }
                // Store for calculation
                recordedMagnitudes.add(magnitude)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this use case
    }
}