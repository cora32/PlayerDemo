package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import io.iskopasi.player_test.R


data class FFTChartData(
    val map: MutableMap<Int, Float> = mutableMapOf(),
    val maxAmplitude: Float = 0f
)

class FFTView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    companion object {
        // Taken from: https://en.wikipedia.org/wiki/Preferred_number#Audio_frequencies
        val FREQUENCY_BAND_LIMITS = arrayOf(
            20, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
            800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000,
            12500, 16000, 20000
        )
    }

    private var centerX = 0f
    private var centerY = 0f
    private var step = 0f
    private val lineWidth = 2.dp.value
    private val path = Path()
    private val paint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            color = ResourcesCompat.getColor(resources, R.color.text_color_1_trans, null)
            strokeCap = Paint.Cap.ROUND
            strokeWidth = lineWidth
            isAntiAlias = true
            textSize = 25.sp.value
        }
    }
    private var maxAmplitude = 0f
    private var yFactor = 0f
    var data = FFTChartData()
        set(value) {
            field = value

            maxAmplitude = value.maxAmplitude
            yFactor = centerY / maxAmplitude

            if (value.map.isNotEmpty()) {
                step = width / value.map.size.toFloat()

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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        path.reset()
        path.moveTo(0f, centerY)

        var xValue = 0f
        data.map.onEachIndexed { i, entry ->
            val frequency = entry.key
            val value = entry.value

            val amplitude = centerY - value * yFactor
            path.lineTo(xValue, amplitude)

            if (i % 5 == 0 || i == FREQUENCY_BAND_LIMITS.size - 1) {
                val text = "$frequency"
                val textWidth = paint.measureText(text)
                val tX = xValue - textWidth / 2f
                val tY = centerY + 100

                canvas.rotate(45f, tX, tY)
                canvas.drawText(text, tX, tY, paint)
                canvas.rotate(-45f, tX, tY)
            }

            xValue += step
        }

        canvas.drawPath(path, paint)
    }
}