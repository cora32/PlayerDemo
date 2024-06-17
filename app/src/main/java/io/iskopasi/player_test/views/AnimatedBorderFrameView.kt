package io.iskopasi.player_test.views

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import io.iskopasi.player_test.R

class AnimatedBorderFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var oldColor: Int =
        ResourcesCompat.getColor(context.resources, R.color.silver, null)
    private val width =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)
            .toInt()

    fun setBorderColor(newColor: Int) {
        (background as GradientDrawable).let { drawable ->
            drawable.mutate()

            ValueAnimator.ofObject(ArgbEvaluator(), oldColor, newColor).apply {
                duration = 250L
                addUpdateListener {
                    drawable.setStroke(width, it.animatedValue as Int)
                }
                start()
            }
        }

        oldColor = newColor
    }

}