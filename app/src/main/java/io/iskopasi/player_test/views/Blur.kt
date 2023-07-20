package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import io.iskopasi.player_test.utils.Utils
import io.iskopasi.player_test.utils.Utils.e

class Blur @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val dstRect = Rect(0, 0, 0, 0)
    private var internalBitmap: Bitmap? = null
    private lateinit var internalCanvas: Canvas

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)

//        "--> Blur width: $width; height: $height".e

        dstRect.right = width
        dstRect.bottom = height
    }

    fun blur(view: View) {
        "---> blurring $width $height; view.width: ${view.width}; view.height: ${view.height}".e

        internalBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        internalCanvas = Canvas(internalBitmap!!)

        fun listener() {
            view.viewTreeObserver.removeOnGlobalLayoutListener(::listener)

            view.draw(internalCanvas)

            internalBitmap = Utils.blur(internalBitmap!!)

            invalidate()
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(::listener)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        internalBitmap?.apply {
            canvas?.drawBitmap(
                this,
                null,
                dstRect,
                null
            )
        }
    }
}