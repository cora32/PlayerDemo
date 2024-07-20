package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import io.iskopasi.player_test.R


data class FFTChartData(
    val map: MutableMap<Int, Float> = mutableMapOf(),
    val maxAmplitude: Float = 0f,
    val bitmap: Bitmap? = null,
)

class FFTView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FFTBaseView(context, attrs, defStyleAttr) {
    companion object {
        // Taken from: https://en.wikipedia.org/wiki/Preferred_number#Audio_frequencies
        val FREQUENCY_BAND_LIMITS = arrayOf(
            20, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
            800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000,
            12500, 16000, 20000
        )
    }

    private val gradient by lazy {
        LinearGradient(
            0f, 0f, 0f, (height - 100.dp.value).toFloat(),
            ContextCompat.getColor(context, R.color.fft_bg),
            ContextCompat.getColor(context, R.color.trans), Shader.TileMode.CLAMP
        )
    }
    private val bgPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
//            color = ResourcesCompat.getColor(resources, R.color.fft_bg, null)
            strokeCap = Paint.Cap.ROUND
            strokeWidth = lineWidth
            isAntiAlias = true
            textSize = 25.sp.value
            isDither = true
            shader = gradient
            isAntiAlias = true
        }
    }
    private val paint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            color = ResourcesCompat.getColor(resources, R.color.text_color_1, null)
            strokeCap = Paint.Cap.ROUND
            strokeWidth = lineWidth
            isAntiAlias = true
            textSize = 25.sp.value
            isDither = true
            isAntiAlias = true
        }
    }
    private val barPaint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            color = ResourcesCompat.getColor(resources, R.color.text_color_1, null)
            strokeCap = Paint.Cap.ROUND
            strokeWidth = barWidth
            isAntiAlias = true
            textSize = 25.sp.value
            isDither = true
            isAntiAlias = true
        }
    }
    private var centerX = 0f
    private var centerY = 0f
    private var step = 0f
    private val lineWidth = 2.dp.value
    private val lOffset = 5.dp.value
    private val path = Path()
    private var yFactor = 0f
    private var barWidth = 20.dp.value
    private val labelOffset = 10.dp.value
    private val xPadding = 60.dp.value
    private val yPadding = 120.dp.value
    private val bgRect = Rect(0, 0, 0, 0)

    var data = FFTChartData()
        set(value) {
            field = value

            if (value.map.isNotEmpty()) {
                yFactor = (height - yPadding * 1.2f) / value.maxAmplitude
                step = (width - xPadding) / value.map.size.toFloat()

                invalidate()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)

        centerX = width / 2f
        centerY = height / 2f

        frameRect.bottom = (height - labelOffset).toInt()
        bgRect.apply {
            left = 0
            top = 0
            right = width
            bottom = height
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(bgRect, bgPaint)

        if (data.map.isEmpty()) {
            drawNoData(
                canvas,
                centerX - textWidth / 2f,
                centerY - labelOffset * 3,
            )
        } else {
//        drawFrequencies(canvas)
            drawBars(canvas)
        }
        drawFrame(canvas)
    }

    override fun drawFrame(canvas: Canvas) {
        canvas.drawLine(
            0f, 0f, 0f,
            height.toFloat() - yPadding, paint
        )
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, paint)
        canvas.drawLine(
            width.toFloat(), 0f,
            width.toFloat(),
            height.toFloat() - yPadding,
            paint
        )
    }

    private fun drawBars(canvas: Canvas) {
        var xValue = step
        data.map.onEachIndexed { i, entry ->
            val frequency = entry.key
            val value = entry.value
            val amplitude = height - yPadding - value * yFactor

            canvas.drawLine(xValue, amplitude, xValue, height.toFloat() - yPadding, barPaint)

            if (i % 5 == 0 || i == FREQUENCY_BAND_LIMITS.size - 1) {
                val text = "$frequency"
                val tY = height - yPadding + labelOffset * 3
                val textY = tY + lOffset * 5f

                canvas.rotate(45f, xValue, textY)
                canvas.drawText(text, xValue, textY, paint)
                canvas.rotate(-45f, xValue, textY)
                canvas.drawLine(xValue, tY - lOffset, xValue, tY + lOffset, paint)
            }

            xValue += step
        }
    }

    private fun drawFrequencies(canvas: Canvas) {
        path.reset()
        path.moveTo(0f, centerY)

        var xValue = step
        data.map.onEachIndexed { i, entry ->
            val frequency = entry.key
            val value = entry.value
            val amplitude = centerY - value * yFactor

            path.lineTo(xValue, amplitude)

            if (i % 5 == 0 || i == FREQUENCY_BAND_LIMITS.size - 1) {
                val text = "$frequency"
                val textWidth = paint.measureText(text)
                val tX = xValue
                val tY = centerY + labelOffset * 3

                canvas.rotate(45f, tX, tY + lOffset)
                canvas.drawText(text, tX, tY + lOffset * 10, paint)
                canvas.rotate(-45f, tX, tY + lOffset)
                canvas.drawLine(xValue, tY - lOffset, xValue, tY + lOffset, paint)
            }

            xValue += step
        }

        canvas.drawPath(path, paint)
    }
}