package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import io.iskopasi.player_test.R
import io.iskopasi.player_test.utils.spToPx

open class FFTBaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val frameLineWidth = 2f
    private val framePaint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            color = ResourcesCompat.getColor(resources, R.color.text_color_1, null)
            strokeCap = Paint.Cap.ROUND
            strokeWidth = frameLineWidth
            isAntiAlias = true
            textSize = context.spToPx(15)
            isDither = true
            isAntiAlias = true
        }
    }
    private val paintNoData by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            color = ResourcesCompat.getColor(resources, R.color.text_color_1_trans2, null)
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
            textSize = context.spToPx(20)
        }
    }
    private val noDataText = ContextCompat.getString(context, R.string.no_data)
    val textWidth = paintNoData.measureText(noDataText)
    var frameRect = Rect(0, 0, 0, 0)

    open fun drawFrame(canvas: Canvas) {
        canvas.drawRect(frameRect, framePaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)

        frameRect.apply {
            left = 0
            top = 0
            right = width
            bottom = height
        }
    }

    fun drawNoData(canvas: Canvas, x: Float, y: Float) {
        canvas.drawText(
            noDataText,
            x, y,
            paintNoData
        )
    }
}