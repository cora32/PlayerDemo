package io.iskopasi.player_test.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView

class TransparentRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {
    private val container: SlidingContainer
        get() = (parent?.parent as SlidingContainer)

    init {
        ViewCompat.setNestedScrollingEnabled(this, false)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
            }

            MotionEvent.ACTION_MOVE -> {
                scrollBy(0, -container.tempDeltaY.toInt())
            }

            MotionEvent.ACTION_UP -> {
            }
        }
        super.dispatchTouchEvent(event)

        return true
    }
}