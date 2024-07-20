package io.iskopasi.player_test.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.BounceInterpolator
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import io.iskopasi.player_test.R
import kotlin.math.max
import kotlin.math.min


class SliderArrowHorizontal @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    name: String = ""
) : SliderArrow(context, attrs, defStyleAttr, 100.dp.value, 90.dp.value, name)

class SliderArrowVertical @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    name: String = ""
) : SliderArrow(context, attrs, defStyleAttr, 100.dp.value, 120.dp.value, name)

open class SliderArrow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val width: Float = 0f,
    val height: Float = 0f,
    val name: String = ""
) : View(context, attrs, defStyleAttr) {
    var initialX = 0f
    var initialY = 0f
    private var lastDeltaX = 0f
    private var lastDeltaY = 0f
    private var deltaX: Float = 0f
    private var deltaY: Float = 0f
    private val lineWidth = 5f
    private var hideAnimator: ViewPropertyAnimator? = null
    private var arrowXAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 0f).apply {
        interpolator = BounceInterpolator()
        addUpdateListener { anim ->
            this@SliderArrow.deltaX = anim.animatedValue as Float
            invalidate()
        }
        start()
    }
    private var arrowYAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 0f).apply {
        interpolator = BounceInterpolator()
        addUpdateListener { anim ->
            this@SliderArrow.deltaY = anim.animatedValue as Float
            invalidate()
        }
        start()
    }
    private val paint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            color = ResourcesCompat.getColor(resources, R.color.text_color_1_trans, null)
            strokeCap = Paint.Cap.ROUND
            strokeWidth = lineWidth
            isAntiAlias = true
        }
    }
    private val isHorizontal: Boolean
        get() = width > height
    private var startX = 0f
    private var startY = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var endX = 0f
    private var endY = 0f
    private var padding = 8.dp.value
    private val maxXDeviation = 20.dp.value
    private val maxYDeviation = 8.dp.value

    init {
        id = name.hashCode()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val xCenter = width / 2
        val yCenter = height / 2

        // horizontal line
        if (isHorizontal) {
            startX = padding
            centerX = xCenter
            endX = width - padding

            startY = yCenter - lineWidth / 2
            centerY = yCenter - lineWidth / 2
            endY = yCenter - lineWidth / 2
        } else {
            // vertical line
            startX = xCenter - lineWidth / 2
            centerX = xCenter - lineWidth / 2
            endX = xCenter - lineWidth / 2

            startY = padding
            centerY = yCenter
            endY = height - padding
        }

        setMeasuredDimension(width.toInt(), height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isHorizontal) {
            deltaY = if (deltaY > 0) {
                min(deltaY, maxYDeviation)
            } else if (deltaY < 0) {
                max(deltaY, -maxYDeviation)
            } else 0f

            canvas.drawLine(
                startX, startY,
                centerX - lineWidth / 3, centerY + deltaY,
                paint
            )
            canvas.drawLine(
                centerX + lineWidth / 3, centerY + deltaY,
                endX, endY,
                paint
            )
        } else {
            deltaX = if (deltaX > 0) {
                min(deltaX, maxXDeviation)
            } else if (deltaX < 0) {
                max(deltaX, -maxXDeviation)
            } else 0f

            canvas.drawLine(
                startX, startY,
                centerX + deltaX, centerY - lineWidth / 3,
                paint
            )
            canvas.drawLine(
                centerX + deltaX, centerY + lineWidth / 3,
                endX, endY,
                paint
            )
        }
    }

    fun flingX(deltaX: Float) {
        arrowXAnimator.apply {
            setFloatValues(deltaX - lastDeltaX, 0f)
            start()
        }
        lastDeltaX = deltaX
    }

    fun flingY(deltaY: Float) {
        arrowYAnimator.apply {
            setFloatValues(deltaY - lastDeltaY, 0f)
            start()
        }
        lastDeltaY = deltaY
    }

    fun show() {
        hideAnimator?.cancel()
        animate().setDuration(100L).alpha(1f)
    }

    fun hide() {
        hideAnimator = animate().apply {
            setDuration(500L).alpha(0f)
        }
    }
}