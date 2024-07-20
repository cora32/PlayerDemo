package io.iskopasi.player_test.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import io.iskopasi.player_test.utils.Utils.e

class TransparentRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {
    private val container: SlidingContainer
        get() = (parent?.parent as SlidingContainer)

    private val gestureDetector by lazy {
        GestureDetector(context.applicationContext,
            object : GestureDetector.OnGestureListener {

                override fun onDown(e: MotionEvent): Boolean {
                    return false
                }

                override fun onShowPress(e: MotionEvent) {

                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return false
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    return false
                }

                override fun onLongPress(e: MotionEvent) {

                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    "-->RV onFling! ${velocityY.toInt()}".e
                    fling(
                        0,
                        -velocityY.toInt() * 2
                    )

                    return false
                }
            })
    }

    init {
        ViewCompat.setNestedScrollingEnabled(this, false)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        container.processTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
            }

            MotionEvent.ACTION_UP -> {
//                val yFling = if ((parent?.parent as SlidingContainer).tempDeltaY.toInt() < 0) 8000
//                else -8000
//                fling(
//                    0,
//                    yFling
//                )

//                return consumed
            }

            MotionEvent.ACTION_MOVE -> {
                scrollBy(0, -(parent?.parent as SlidingContainer).tempDeltaY.toInt())
            }
        }
        val consumed = gestureDetector.onTouchEvent(event)

        return super.dispatchTouchEvent(event)
    }
}