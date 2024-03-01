package io.iskopasi.player_test.fragments

import android.graphics.RuntimeShader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import io.iskopasi.player_test.databinding.FragmentXml2Binding


class XmlFragment2 : Fragment() {
    private lateinit var binding: FragmentXml2Binding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val FROSTED_GLASS_SHADER = RuntimeShader(
        """
    uniform shader inputShader;
    uniform float height;
    uniform float width;
            
    vec4 main(vec2 coords) {
        vec4 currValue = inputShader.eval(coords);
//        float top = height - 100;
        float top = 0;
        if (coords.y < top) {
            return currValue;
        } else {
            float radius = 5;
            // Avoid blurring edges
            if (coords.x > 1 && coords.y > 1 &&
                    coords.x < (width - 1) &&
                    coords.y < (height - 1)) {
                // simple box blur - average 5x5 grid around pixel
                vec4 boxSum =
                    inputShader.eval(coords + vec2(-radius, -radius)) + 
                    // ...
                    currValue +
                    // ...
                    inputShader.eval(coords + vec2(radius, radius));
                currValue = boxSum / radius * radius;
            }
            
            const vec4 white = vec4(1);            // top-left corner of label area
            vec2 lefttop = vec2(0, 0);
            float lightenFactor = min(1.0, .6 *
                    length(coords - lefttop) /
                    (0.85 * length(vec2(width, 100))));
            // White in upper-left, blended increasingly
            // toward lower-right
            return mix(currValue, white, 1 - lightenFactor);
//            return currValue;
        }
    }
"""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentXml2Binding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            "---> blurring ${binding.blur.height}".e
//            FROSTED_GLASS_SHADER.setFloatUniform("height", 1000f)
//            FROSTED_GLASS_SHADER.setFloatUniform("width", 1000f)
//            val effect = RenderEffect.createRuntimeShaderEffect(FROSTED_GLASS_SHADER, "inputShader")
//
//            binding.blur.setRenderEffect(effect)
//        } else {
//            TODO("VERSION.SDK_INT < TIRAMISU")
//        }
    }
}