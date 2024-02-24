package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import io.iskopasi.player_test.utils.Utils
import io.iskopasi.player_test.utils.Utils.e


class ImageBlur @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    init {
        fun listener() {
            viewTreeObserver.removeOnDrawListener(::listener)

            blur()
        }

        viewTreeObserver.addOnGlobalLayoutListener(::listener)
    }

    fun blur() {
        "--> blur $rootView $x $y $width $height".e
        val bgBitmap = Utils.getSectorBitmap(rootView, x, y, width, height)

        setImageBitmap(bgBitmap)

        invalidate()

        val radius = 32f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurRenderEffect = RenderEffect.createBlurEffect(
                radius, radius,
                Shader.TileMode.MIRROR
            )

            setRenderEffect(blurRenderEffect)
        } else {
            TODO("VERSION.SDK_INT < S")
        }

    }
}