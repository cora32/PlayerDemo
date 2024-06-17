package io.iskopasi.player_test.views

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import io.iskopasi.player_test.R

class AnimatedBGFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var oldColor: Int =
        ResourcesCompat.getColor(context.resources, R.color.gradient_end, null)
    private val colorStart =
        ResourcesCompat.getColor(context.resources, R.color.gradient_start, null)
    private val colorEnd = ResourcesCompat.getColor(context.resources, R.color.gradient_end, null)

    fun setColor(newColor: Int) {
        val oldBlendStart = ColorUtils.blendARGB(colorStart, colorStart, 0.1F)
        val oldBlendEnd = ColorUtils.blendARGB(colorEnd, colorEnd, 0.7F)

        ValueAnimator.ofObject(ArgbEvaluator(), oldColor, newColor).let { animator ->
            animator.duration = 250L

            animator.addUpdateListener {
                val value = animator.animatedValue as Int
                val newBlendStart = ColorUtils.blendARGB(oldBlendStart, value, 0.1F)
                val newBlendEnd = ColorUtils.blendARGB(oldBlendEnd, value, 0.3F)

                background = GradientDrawable().apply {
                    colors = intArrayOf(
                        newBlendStart,
                        newBlendEnd,
                    )
                    orientation = GradientDrawable.Orientation.TOP_BOTTOM
                    gradientType = GradientDrawable.LINEAR_GRADIENT
                    shape = GradientDrawable.RECTANGLE
                }
            }

            animator.start()
        }

        oldColor = newColor
    }

}