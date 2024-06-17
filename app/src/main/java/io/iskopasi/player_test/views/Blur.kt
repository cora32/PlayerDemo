package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import io.iskopasi.player_test.utils.Utils

class Blur @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val dstRect = Rect(0, 0, 0, 0)
    private var internalBitmap: Bitmap? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val PERLIN_NOISE = RuntimeShader(
        """
   uniform float2 resolution;
   uniform float time;
   uniform shader inputShader;
  
   //
   // Description : Array and textureless GLSL 2D/3D/4D simplex
   //               noise functions.
   //      Author : Ian McEwan, Ashima Arts.
   //  Maintainer : stegu
   //     Lastmod : 20201014 (stegu)
   //     License : Copyright (C) 2011 Ashima Arts. All rights reserved.
   //               Distributed under the MIT License. See LICENSE file.
   //               https://github.com/ashima/webgl-noise
   //               https://github.com/stegu/webgl-noise
   //
  
   vec3 mod289(vec3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
   }
  
   vec4 mod289(vec4 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
   }
  
   vec4 permute(vec4 x) {
       return mod289(((x*34.0)+10.0)*x);
   }
  
   float snoise(vec3 v)
   {
    const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
    const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);
  
     // First corner
    vec3 i  = floor(v + dot(v, C.yyy) );
    vec3 x0 =   v - i + dot(i, C.xxx) ;
  
    // Other corners
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min( g.xyz, l.zxy );
    vec3 i2 = max( g.xyz, l.zxy );
  
    //   x0 = x0 - 0.0 + 0.0 * C.xxx;
    //   x1 = x0 - i1  + 1.0 * C.xxx;
    //   x2 = x0 - i2  + 2.0 * C.xxx;
    //   x3 = x0 - 1.0 + 3.0 * C.xxx;
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy; // 2.0*C.x = 1/3 = C.y
    vec3 x3 = x0 - D.yyy;      // -1.0+3.0*C.x = -0.5 = -D.y
  
    // Permutations
    i = mod289(i);
    vec4 p = permute( permute( permute(
               i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
             + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))
             + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));
  
    // Gradients: 7x7 points over a square, mapped onto an octahedron.
    // The ring size 17*17 = 289 is close to a multiple of 49 (49*6 = 294)
    float n_ = 0.142857142857; // 1.0/7.0
    vec3  ns = n_ * D.wyz - D.xzx;
  
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,7*7)
  
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)
  
    vec4 x = x_ *ns.x + ns.yyyy;
    vec4 y = y_ *ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
  
    vec4 b0 = vec4( x.xy, y.xy );
    vec4 b1 = vec4( x.zw, y.zw );
  
    vec4 s0 = floor(b0)*2.0 + 1.0;
    vec4 s1 = floor(b1)*2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
  
    vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
    vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;
  
    vec3 p0 = vec3(a0.xy,h.x);
    vec3 p1 = vec3(a0.zw,h.y);
    vec3 p2 = vec3(a1.xy,h.z);
    vec3 p3 = vec3(a1.zw,h.w);
  
    //Normalise gradients
    vec4 norm = inversesqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
  
    // Mix final noise value
    vec4 m = max(0.5 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    m = m * m;
    return 105.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1),
                                  dot(p2,x2), dot(p3,x3) ) );
   }
  
   half4 main(in vec2 fragCoord) {
      vec2 uv = (fragCoord.xy / resolution.xy);
      float noise = snoise(vec3(uv.x * 6, uv.y * 6, time * 0.5));
     
      noise *= exp(-length(abs(uv * 1.5))); 
      vec2 offset1 = vec2(noise * 0.02);
      vec2 offset2 = vec2(0.02) / resolution.xy;
      uv += offset1 - offset2;
     
      return inputShader.eval(uv * resolution.xy);
   }
"""
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val GREYSCALE_SHADER = RuntimeShader(
        """
    uniform shader inputShader;
    
    uniform float2 childResolution;
    half4 main(float2 fragCoord){
           half4 color = inputShader.eval( float2(fragCoord.x + childResolution.x, fragCoord.y + childResolution.y));
        color.rgb = half3(dot(color.rgb, half3(0.2126, 0.7152, 0.0722)));
        return color;
    }
"""
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val TEST_SHADER = RuntimeShader(
        """
    uniform shader inputShader;
    
    vec4 main(vec2 coords) {
        return inputShader.eval(coords).bgra;
//        return inputShader.eval(coords).half4(1,0,0,1);
    }
            
        """.trimIndent()
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val BLUR_SHADER = RuntimeShader(
        """
    uniform shader inputShader;
    uniform float height;
    uniform float width;
    uniform float2 iResolution;
    
    const vec3 offset = vec3(-1, 0, 1);
    const mat3 box_blur = mat3(1, 1, 1, 1, 1, 1, 1, 1, 1) * 0.1111;
    const mat3 gaussian_blur = mat3(1, 2, 1, 2, 4, 2, 1, 2, 1) * 0.0625;
    const mat3 sharpen = mat3(0, -1, 0, -1, 5, -1, 0, -1, 0);
    
    vec4 main(vec2 coords) { 
    // Normalized pixel coordinates (from 0 to 1)
//        vec2 uv = coords / iResolution.xy;
        vec4 currValue = vec4(0);
        
//const int radius = 3;
//
//    for(int x = 0; x < radius; x++) {
//        for(int y = 0; y < radius; y++) {
//    //            vec2 offset = vec2(x, y) / iResolution.xy;
////                currValue += (inputShader.eval(coords + vec2(offset[x], offset[y])) * box_blur[x][y]);
////                currValue += (inputShader.eval(vec2(coords.x + offset[x], coords.y + offset[y])) * box_blur[x][y]);
//                currValue += (inputShader.eval(vec2(coords.x + offset[x], coords.y + offset[y])) * gaussian_blur[x][y]);
//        }
//    }
//        currValue /= 9.0;

    const int radius = 50;
    const int rStart = int(-radius / 2);

    for(int x = 0; x < radius; x++) {
        float xOffset = float(rStart + x);

        for(int y = 0; y < radius; y++) {
            float yOffset = float(rStart + y);         
            currValue += (inputShader.eval(vec2(coords.x + xOffset, coords.y + yOffset)));
        }
    }
        currValue /= float(radius * radius);
        
        return currValue;
    }
""".trimIndent()
    )

    init {
        fun listener() {
            viewTreeObserver.removeOnGlobalLayoutListener(::listener)
            blur()
        }

        viewTreeObserver.addOnGlobalLayoutListener(::listener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)

//        "--> onMeasure Blur width: $width; height: $height".e

        dstRect.right = width
        dstRect.bottom = height
    }


    fun blur() {
        blur(this.rootView)
    }

    fun blur(view: View) {
        internalBitmap = Utils.crop(
            view,
            x, y,
            width, height
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            FROSTED_GLASS_SHADER.setFloatUniform("height", height.toFloat())
//            FROSTED_GLASS_SHADER.setFloatUniform("width", width.toFloat())
            BLUR_SHADER.setFloatUniform("iResolution", width.toFloat(), height.toFloat())

            val effect1 = RenderEffect.createRuntimeShaderEffect(BLUR_SHADER, "inputShader")
            val effect2 = RenderEffect.createRuntimeShaderEffect(BLUR_SHADER, "inputShader")
            val effect3 = RenderEffect.createRuntimeShaderEffect(GREYSCALE_SHADER, "inputShader")
//            val effect = RenderEffect.createRuntimeShaderEffect(TEST_SHADER, "inputShader")

//            val effect = RenderEffect.createRuntimeShaderEffect(GREYSCALE_SHADER, "inputShader")
//            val effect = RenderEffect.createRuntimeShaderEffect(PERLIN_NOISE, "inputShader")
            val blend = RenderEffect.createBlendModeEffect(
                effect1,
                effect2,
                BlendMode.HARD_LIGHT
            )

            this.setRenderEffect(blend)
//            this.setRenderEffect(
//                RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.MIRROR)
//            )
        } else {
            TODO("VERSION.SDK_INT < TIRAMISU")
        }
    }

    override fun onDraw(canvas: Canvas) {
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