
package com.example.voicedsp

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.*
import kotlin.math.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private var isRecording = false
    private lateinit var recordButton: Button
    private lateinit var saveButton: Button
    private lateinit var spectrumView: SpectrumView
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recordButton = findViewById(R.id.recordBtn)
        saveButton = findViewById(R.id.saveBtn)
        spectrumView = findViewById(R.id.spectrumView)

        recordButton.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }

        saveButton.setOnClickListener {
            Toast.makeText(this, "Save not implemented yet", Toast.LENGTH_SHORT).show()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    private fun startRecording() {
        isRecording = true
        recordButton.text = "Stop"

        thread {
            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize)

            val buffer = ShortArray(bufferSize)
            val fftBuffer = FloatArray(bufferSize)

            audioRecord.startRecording()

            while (isRecording) {
                audioRecord.read(buffer, 0, buffer.size)
                val filtered = bandPassFilter(buffer.map { it.toFloat() }.toFloatArray(), 300f, 3400f, sampleRate)
                val magnitudes = fftMagnitude(filtered)
                runOnUiThread {
                    spectrumView.updateSpectrum(magnitudes)
                }
            }

            audioRecord.stop()
            audioRecord.release()
        }
    }

    private fun stopRecording() {
        isRecording = false
        recordButton.text = "Start"
    }

    private fun bandPassFilter(input: FloatArray, low: Float, high: Float, sr: Int): FloatArray {
        val result = FloatArray(input.size)
        val rcLow = 1.0f / (2 * Math.PI * high)
        val dt = 1.0f / sr
        val alphaLow = (dt / (rcLow + dt)).toFloat()

        val rcHigh = 1.0f / (2 * Math.PI * low)
        val alphaHigh = (rcHigh / (rcHigh + dt)).toFloat()

        var lowPass = 0f
        var highPass = 0f

        for (i in input.indices) {
            lowPass += alphaLow * (input[i] - lowPass)
            highPass = alphaHigh * (highPass + input[i] - (if (i > 0) input[i - 1] else 0f))
            result[i] = highPass - lowPass
        }
        return result
    }

    private fun fftMagnitude(data: FloatArray): FloatArray {
        val n = data.size
        val windowed = data.mapIndexed { i, x -> x * 0.5f * (1 - cos(2 * Math.PI * i / n)).toFloat() }.toFloatArray()
        val real = windowed.copyOf()
        val imag = FloatArray(n)
        fft(real, imag)
        return FloatArray(n / 2) { i -> sqrt(real[i] * real[i] + imag[i] * imag[i]) }
    }

    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n == 1) return

        val evenReal = FloatArray(n / 2)
        val evenImag = FloatArray(n / 2)
        val oddReal = FloatArray(n / 2)
        val oddImag = FloatArray(n / 2)

        for (i in 0 until n / 2) {
            evenReal[i] = real[i * 2]
            evenImag[i] = imag[i * 2]
            oddReal[i] = real[i * 2 + 1]
            oddImag[i] = imag[i * 2 + 1]
        }

        fft(evenReal, evenImag)
        fft(oddReal, oddImag)

        for (k in 0 until n / 2) {
            val tReal = cos(-2 * Math.PI * k / n).toFloat() * oddReal[k] - sin(-2 * Math.PI * k / n).toFloat() * oddImag[k]
            val tImag = sin(-2 * Math.PI * k / n).toFloat() * oddReal[k] + cos(-2 * Math.PI * k / n).toFloat() * oddImag[k]

            real[k] = evenReal[k] + tReal
            imag[k] = evenImag[k] + tImag
            real[k + n / 2] = evenReal[k] - tReal
            imag[k + n / 2] = evenImag[k] - tImag
        }
    }
}
