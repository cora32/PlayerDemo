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
import androidx.core.graphics.ColorUtils
import io.iskopasi.player_test.R
import io.iskopasi.player_test.utils.Utils.e
import io.iskopasi.player_test.views.FFTView.Companion.FREQUENCY_BAND_LIMITS


class SpectroView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
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
    private var yStep = 0f
    private var columnWidth = 5.dp.value
    private val accumData = mutableListOf<FFTChartData>()

//    var data = FFTChartData()
//        set(value) {
//            maxAmplitude = value.maxAmplitude
//            yFactor = centerY / maxAmplitude
//
//            if (value.map.isNotEmpty()) {
//                step = width / value.map.size.toFloat()
//
//                invalidate()
//            }
//        }

    fun set(data: FFTChartData) {
        if (data.maxAmplitude > 0) {
            accumData.add(data)
            "--> set data: ${accumData.size}".e

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

        yStep = (height / FREQUENCY_BAND_LIMITS.size).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var xValue = 0f
        accumData.forEach { data ->
            val valueFactor = 255f / data.maxAmplitude
            var yValue = height.toFloat()

            data.map.onEach {
                val amplitude = it.value * valueFactor
                val newColor = ColorUtils.setAlphaComponent(paint.color, amplitude.toInt())

                canvas.drawLine(xValue,
                    yValue,
                    xValue + columnWidth,
                    yValue,
                    paint.apply {
                        color = newColor
                    })

                yValue -= yStep
            }

            xValue += columnWidth

        }
    }
}