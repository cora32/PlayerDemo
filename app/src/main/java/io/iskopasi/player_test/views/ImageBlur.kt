package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi


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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val FROSTED_GLASS_SHADER = RuntimeShader(
        """
    uniform shader inputShader;
    uniform float height;
    uniform float width;
            
    vec4 main(vec2 coords) {
        vec4 currValue = inputShader.eval(coords);
        float top = height - 100;
        if (coords.y < top) {
            return currValue;
        } else {
            // Avoid blurring edges
            if (coords.x > 1 && coords.y > 1 &&
                    coords.x < (width - 1) &&
                    coords.y < (height - 1)) {
                // simple box blur - average 5x5 grid around pixel
                vec4 boxSum =
                    inputShader.eval(coords + vec2(-2, -2)) + 
                    // ...
                    currValue +
                    // ...
                    inputShader.eval(coords + vec2(2, 2));
                currValue = boxSum / 25;
            }
            
            const vec4 white = vec4(1);            // top-left corner of label area
            vec2 lefttop = vec2(0, top);
            float lightenFactor = min(1.0, .6 *
                    length(coords - lefttop) /
                    (0.85 * length(vec2(width, 100))));
            // White in upper-left, blended increasingly
            // toward lower-right
            return mix(currValue, white, 1 - lightenFactor);
        }
    }
"""
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val effect = RenderEffect.createRuntimeShaderEffect(FROSTED_GLASS_SHADER, "inputShader")

    fun blur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            FROSTED_GLASS_SHADER.setFloatUniform("height", height.toFloat())
            FROSTED_GLASS_SHADER.setFloatUniform("width", width.toFloat())

            this.setRenderEffect(effect)
        } else {
            TODO("VERSION.SDK_INT < TIRAMISU")
        }
//        "--> blur $rootView $x $y $width $height".e
//        val bgBitmap = Utils.getBitmapSector(rootView, x, y + 50, width, height)
//
//        setImageBitmap(bgBitmap)
//
//        invalidate()
//
//        val radius = 32f
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            val blurRenderEffect = RenderEffect.createBlurEffect(
//                radius, radius,
//                Shader.TileMode.MIRROR
//            )
//
//            setRenderEffect(blurRenderEffect)
//        } else {
//            TODO("VERSION.SDK_INT < S")
//        }

    }
}