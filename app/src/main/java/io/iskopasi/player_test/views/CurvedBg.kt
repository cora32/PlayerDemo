package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import io.iskopasi.player_test.R
import io.iskopasi.player_test.utils.spToPx
import io.iskopasi.player_test.utils.toPx

class CurvedBg @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val path = Path()
    private val paint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            color = ResourcesCompat.getColor(resources, R.color.splash_bg, null)
            strokeCap = Paint.Cap.ROUND
            strokeWidth = context.toPx(1)
            isAntiAlias = true
            textSize = context.spToPx(10)
            isDither = true
//            shader = gradient
            isAntiAlias = true
        }
    }
    private val radius = context.toPx(85)
    private val topY = context.toPx(20)
    private val xOffset = context.toPx(8)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)

        val centerX = width / 2f
        val centerY = height / 2f

        val x1 = centerX - radius + centerX / 4f
        val x2 = centerX - radius - centerX / 2f + xOffset
        val x3 = centerX + radius + centerX / 2f - xOffset
        val x4 = centerX + radius - centerX / 4f

        path.moveTo(0f, 0f)
//        path.lineTo(x1, 0f)
        path.cubicTo(
            x1, -topY,
            x2, height.toFloat() - topY,
            centerX, height.toFloat()
        )
        path.cubicTo(
            x3, height.toFloat() - topY,
            x4, -topY,
            width.toFloat(), 0f
        )
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawPath(path, paint)
    }
}