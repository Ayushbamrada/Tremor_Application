package com.example.yourcare.utils

import kotlin.math.*

object FftHelper {

    // Returns the Dominant Frequency in Hz
    fun calculateDominantFrequency(data: List<Float>, sampleRate: Float): Float {
        val n = data.size
        if (n == 0) return 0f

        // 1. Prepare Complex Arrays (Real & Imaginary)
        // FFT requires size to be a power of 2. We pad with zeros if needed.
        val m = ceil(ln(n.toDouble()) / ln(2.0)).toInt()
        val size = 2.0.pow(m).toInt()

        val real = DoubleArray(size) { i -> if (i < n) data[i].toDouble() else 0.0 }
        val imag = DoubleArray(size) { 0.0 }

        // 2. Perform FFT
        fft(real, imag)

        // 3. Calculate Magnitude for each frequency bin
        // We only look at the first half (Nyquist limit)
        var maxMagnitude = -1.0
        var maxIndex = 0

        // Ignore index 0 (DC offset/Gravity)
        for (i in 1 until size / 2) {
            val magnitude = sqrt(real[i] * real[i] + imag[i] * imag[i])
            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude
                maxIndex = i
            }
        }

        // 4. Convert Index to Frequency (Hz)
        // Frequency = Index * (SampleRate / TotalPoints)
        val frequency = maxIndex * (sampleRate / size)
        return frequency.toFloat()
    }

    // Standard Cooley-Tukey FFT Algorithm
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