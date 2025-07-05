
package com.example.voicedsp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class SpectrumView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var spectrum = FloatArray(256)
    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
    }

    fun updateSpectrum(newSpectrum: FloatArray) {
        spectrum = newSpectrum.copyOf()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val barWidth = width.toFloat() / spectrum.size
        for (i in spectrum.indices) {
            val height = spectrum[i] / 1000f * height
            canvas.drawLine(i * barWidth, height.toFloat(), i * barWidth, height.toFloat() - height, paint)
        }
    }
}
