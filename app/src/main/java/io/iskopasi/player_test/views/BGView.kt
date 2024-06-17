package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.iskopasi.player_test.R
import io.iskopasi.player_test.utils.Utils.e

class BGView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private var testValue: String
    private var color: Int
    private var bgColor: Int
    private val paint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val paint2 = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
    }
    private var centerX = 0f
    private var centerY = 0f
    private val radius = 100f
    private var xOffset = 0f
    private var yOffset = centerY
    private var rOffset = radius

    init {
        if (!isInEditMode) {
            context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.BGView, defStyleAttr, defStyleRes
            ).apply {
                try {
                    testValue = getString(R.styleable.BGView_test_value) ?: ""
                    color = getColor(R.styleable.BGView_color, Color.BLACK)
                    bgColor = getColor(R.styleable.BGView_bg_color, Color.WHITE)
                } finally {
                    recycle()
                }
            }

            paint.color = color
        } else {
            testValue = "test"
            color = Color.BLACK
            bgColor = Color.WHITE
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)


//        val desiredWidth = 100
//        val desiredHeight = 100


//        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
//        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
//        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
//        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
//
//        val width = when (widthMode) {
//            MeasureSpec.EXACTLY -> widthSize
//            MeasureSpec.AT_MOST -> kotlin.math.min(
//                desiredWidth,
//                widthSize
//            )
//            else -> desiredWidth
//        }
//
//        val height = when (heightMode) {
//            MeasureSpec.EXACTLY -> heightSize
//            MeasureSpec.AT_MOST -> kotlin.math.min(
//                desiredHeight,
//                heightSize
//            )
//            else -> desiredHeight
//        }
//
//        setMeasuredDimension(width, height)
//
        "--->BGView measure widthMeasureSpec: $widthMeasureSpec heightMeasureSpec:$heightMeasureSpec; width:$width height:$height".e
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        centerX = width / 2f
        centerY = height / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas?.drawColor(bgColor)

        canvas?.drawLine(0f, 9f, 1000f, 1000f, paint)
        canvas?.drawCircle(0f, 0f, 100f, paint)
        canvas?.drawCircle(220.0f, 200.0f, 120f, paint)

        canvas?.drawCircle(440.0f, 440.0f, 140f, paint)

        for (i in 1..10) {
            "---> wtf?: $xOffset $yOffset $rOffset".e
            canvas?.drawCircle(xOffset, yOffset, rOffset, paint)
            canvas?.drawCircle(xOffset, yOffset, rOffset, paint2)

            xOffset += radius * 2 + 20
            yOffset += rOffset * 2
            rOffset += 20
        }

        canvas?.drawCircle(660.0f, 660.0f, 160f, paint2)
    }
}