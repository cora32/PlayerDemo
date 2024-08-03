package io.iskopasi.player_test.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import io.iskopasi.player_test.utils.Utils.ui
import kotlinx.coroutines.delay
import kotlin.math.abs

class TransparentRecyclerViewY @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {
    private val container: SlidingContainer
        get() = (parent?.parent as SlidingContainer)
    private var scrollDeltaY = 0
    private var scrollDirection = 0
    private var keepScrolling = false

    init {
        ViewCompat.setNestedScrollingEnabled(this, false)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                keepScrolling = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!keepScrolling) {
                    startScrolling()
                }
            }

            MotionEvent.ACTION_UP -> {
                keepScrolling = false

                if (abs(scrollDeltaY) > 10) fling(0, scrollDirection * 5000)
            }
        }

        super.dispatchTouchEvent(event)

        return true
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        scrollDeltaY = t - oldt

        scrollDirection = if (scrollDeltaY > 0) 1 else -1
    }

    private fun startScrolling() {
        keepScrolling = true

        ui {
            while (keepScrolling) {
                scrollBy(0, -container.tempDeltaY.toInt())
                delay(10)
            }
        }
    }
}

class TransparentRecyclerViewX @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {
    private val container: SlidingContainer
        get() = (parent?.parent?.parent as SlidingContainer)
    private var scrollDeltaX = 0
    private var scrollDirection = 0
    private var keepScrolling = false

    init {
        ViewCompat.setNestedScrollingEnabled(this, false)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                keepScrolling = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!keepScrolling) {
                    startScrolling()
                }
            }

            MotionEvent.ACTION_UP -> {
                keepScrolling = false

                if (abs(scrollDeltaX) > 10) fling(scrollDirection * 5000, 0)
            }
        }

        super.dispatchTouchEvent(event)

        return true
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        scrollDeltaX = l - oldl

        scrollDirection = if (scrollDeltaX > 0) 1 else -1
    }

    private fun startScrolling() {
        keepScrolling = true

        ui {
            while (keepScrolling) {
                scrollBy(-container.tempDeltaX.toInt(), 0)
                delay(10)
            }
        }
    }
}