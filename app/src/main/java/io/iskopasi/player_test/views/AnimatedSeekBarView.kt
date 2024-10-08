package io.iskopasi.player_test.views

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import androidx.core.content.res.ResourcesCompat
import io.iskopasi.player_test.R

class AnimatedSeekBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatSeekBar(context, attrs, defStyleAttr) {
    lateinit var interceptTouches: (Boolean) -> Unit
    private var oldColor: Int =
        ResourcesCompat.getColor(context.resources, R.color.gradient_end, null)

    fun setColor(newColor: Int) {
        val progressDrawable = progressDrawable.mutate()
        val thumbDrawable = thumb.mutate()

        ValueAnimator.ofObject(ArgbEvaluator(), oldColor, newColor).let { animator ->
            animator.duration = 250L

            animator.addUpdateListener {
                val value = animator.animatedValue as Int

                progressDrawable.colorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
                thumbDrawable.colorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_ATOP)
            }

            animator.start()
        }

        oldColor = newColor
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            ACTION_DOWN -> {
                interceptTouches(true)
            }

            ACTION_UP -> {
                interceptTouches(false)
            }
        }

        return super.onTouchEvent(event)
    }
}