package com.example.yourcare.utils

import kotlin.math.*

object FftHelper {

    fun calculateDominantFrequency(data: List<Float>, sampleRate: Float): Float {
        val n = data.size
        if (n == 0) return 0f

        // --- FIX: Remove DC Offset (Gravity/Static Bias) ---
        val mean = data.average().toFloat()
        val centeredData = data.map { it - mean }

        // 1. Prepare for FFT (Pad to next power of 2)
        val m = ceil(ln(n.toDouble()) / ln(2.0)).toInt()
        val size = 2.0.pow(m).toInt()

        val real = DoubleArray(size) { i -> if (i < n) centeredData[i].toDouble() else 0.0 }
        val imag = DoubleArray(size) { 0.0 }

        // 2. Run FFT
        fft(real, imag)

        // 3. Find Peak (Ignore first few bins to avoid low-freq noise)
        var maxMagnitude = -1.0
        var maxIndex = 0

        // Start from index 1 (skip 0Hz).
        // We also skip extremely low frequencies (< 2Hz) to filter drift if needed,
        // but index 1 is usually enough if DC is removed.
        for (i in 1 until size / 2) {
            val magnitude = sqrt(real[i] * real[i] + imag[i] * imag[i])
            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude
                maxIndex = i
            }
        }

        // 4. Convert to Hz
        return maxIndex * (sampleRate / size)
    }

    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        if (n <= 1) return

        val half = n / 2
        val evenReal = DoubleArray(half)
        val evenImag = DoubleArray(half)
        val oddReal = DoubleArray(half)
        val oddImag = DoubleArray(half)

        for (i in 0 until half) {
            evenReal[i] = real[2 * i]
            evenImag[i] = imag[2 * i]
            oddReal[i] = real[2 * i + 1]
            oddImag[i] = imag[2 * i + 1]
        }

        fft(evenReal, evenImag)
        fft(oddReal, oddImag)

        for (k in 0 until half) {
            val angle = -2 * PI * k / n
            val cosA = cos(angle)
            val sinA = sin(angle)

            val tReal = cosA * oddReal[k] - sinA * oddImag[k]
            val tImag = sinA * oddReal[k] + cosA * oddImag[k]

            real[k] = evenReal[k] + tReal
            imag[k] = evenImag[k] + tImag
            real[k + half] = evenReal[k] - tReal
            imag[k + half] = evenImag[k] - tImag
        }
    }
}