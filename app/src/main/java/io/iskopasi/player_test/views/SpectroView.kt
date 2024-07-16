package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import io.iskopasi.player_test.R


class SpectroView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FFTBaseView(context, attrs, defStyleAttr) {
    companion object {
        const val MAX_DISPLAYED_SAMPLES = 100
    }
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
    private var bg = ContextCompat.getColor(context, R.color.fft_bg)
    private var centerX = 0f
    private var centerY = 0f
    private val lineWidth = 2.dp.value
    private var yStep = 0f
    private var resizeRect = RectF()
    private var columnWidth = 5.dp.value
    private val dataQueue = mutableListOf<FFTChartData>()
    private var bitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    fun set(data: FFTChartData) {
        data.bitmap?.let {
            bitmap = data.bitmap
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)

        centerX = width / 2f
        centerY = height / 2f

        columnWidth = width / MAX_DISPLAYED_SAMPLES.toFloat()
        resizeRect.apply {
            left = 0f
            top = 0f
            right = width.toFloat()
            bottom = height.toFloat()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(bg)

        bitmap?.let {
            canvas.drawBitmap(it, null, resizeRect, null)
            it.recycle()
        }

        drawFrame(canvas)
    }

    private fun drawSpectrum(canvas: Canvas) {
        var xValue = 0f

        dataQueue.forEach { data ->
            val valueFactor = 255f / data.maxRawAmplitude
            var yValue = height.toFloat()

            for (i in 0 until data.dataList.size / 2 step 2) {
                val amplitude = data.dataList[i] * valueFactor
                val newColor = ColorUtils.setAlphaComponent(paint.color, amplitude.toInt())

                canvas.drawLine(xValue,
                    yValue,
                    xValue + columnWidth,
                    yValue,
                    paint.apply {
                        color = newColor
                    })

                yValue -= yStep * 2
            }

            xValue += columnWidth
        }
    }
}