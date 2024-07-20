package io.iskopasi.player_test.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import io.iskopasi.player_test.R
import io.iskopasi.player_test.utils.Utils.bg
import io.iskopasi.player_test.utils.Utils.ui
import io.iskopasi.player_test.utils.musicVolumeFlow
import io.iskopasi.player_test.utils.toRadians
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


class VolumeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), OnTouchListener {
    private val vibrator by lazy {
        ContextCompat.getSystemService(
            context.applicationContext,
            Vibrator::class.java
        )
    }
    private val audioService by lazy {
        ContextCompat.getSystemService(
            context.applicationContext,
            AudioManager::class.java
        )
    }
    private val volumeUp by lazy {
        ContextCompat.getDrawable(
            context,
            R.drawable.round_volume_up_24
        )
    }
    private val volumeOff by lazy {
        ContextCompat.getDrawable(
            context,
            R.drawable.round_volume_off_24
        )
    }
    private val drawableColor by lazy {
        ContextCompat.getColor(
            context,
            R.color.text_color_1_trans2
        )
    }
    private val iconSize by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            20f,
            resources.displayMetrics
        )
    }
    private val padding =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18f, resources.displayMetrics)
    private val widthBg =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
            .toInt()
    private val widthBg2 =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
            .toInt()
    private val widthMain =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)
            .toInt()
    private val offsetX =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, resources.displayMetrics)
    private val endPadding =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, resources.displayMetrics)
            .toInt()

    private val paintBg = Paint().apply {
        style = Paint.Style.STROKE
        color = ResourcesCompat.getColor(resources, R.color.text_color_1_trans, null)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = widthBg.toFloat()
        isAntiAlias = true
    }
    private val paintBg2 = Paint().apply {
        style = Paint.Style.STROKE
        color = ResourcesCompat.getColor(resources, R.color.text_color_1_trans, null)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = widthBg2.toFloat()
        isAntiAlias = true
    }
    private val paintMain = Paint().apply {
        style = Paint.Style.STROKE
        color = ResourcesCompat.getColor(resources, R.color.text_color_1_trans3, null)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = widthMain.toFloat()
        isAntiAlias = true
    }
    private val paintMain2 = Paint().apply {
        style = Paint.Style.STROKE
        color = ResourcesCompat.getColor(resources, R.color.text_color_1_trans3, null)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = widthBg2.toFloat()
        isAntiAlias = true
    }
    private var width = 0
    private var height = 0
    private var centerX = 0f
    private var centerY = 0f
    private var distance = 0f
    private var volumeAngle = audioService!!.getStreamVolume(AudioManager.STREAM_MUSIC).toAngle()
    private var outerRadius = 30f
    private var outerXOffset = outerRadius + 10f
    private var allowDraw = false
    private var yThreshold = 0f
    private var prevAngle = 0
    private var lastVibrationTime = 0L
    private val rect = RectF()
    private val rect1 = Rect()

    init {
        setOnTouchListener(this)

        // Listen for volume changes
        bg {
            context.applicationContext.musicVolumeFlow.collect {
                volumeAngle = it.toAngle()

                ui {
                    invalidate()
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        width = MeasureSpec.getSize(widthMeasureSpec)
        height = MeasureSpec.getSize(heightMeasureSpec)
        centerX = width / 2f
        centerY = height / 2f

        rect.left = -offsetX
        rect.top = 0f
        rect.right = width.toFloat() - endPadding
        rect.bottom = height.toFloat()

        distance = offsetX + iconSize / 2f + endPadding
        yThreshold = (volumeAngle + 43).toThreshold()

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawArc(rect, -45f, 90f, false, paintBg)
        canvas.drawArc(rect, 45f, volumeAngle, false, paintMain)

        // Draw volume up
        val angleVolumeUp = (-55f).toRadians()
        var drawableStart = (angleVolumeUp.toX(distance).toInt() - iconSize / 2f).toInt()
        var drawableTop = (angleVolumeUp.toY(distance).toInt() - iconSize / 2f).toInt()
        var drawableRight = (drawableStart + iconSize).toInt()
        var drawableBottom = (drawableTop + iconSize).toInt()
        volumeUp?.apply {
            rect1.left = drawableStart
            rect1.top = drawableTop
            rect1.right = drawableRight
            rect1.bottom = drawableBottom

            bounds = rect1
            setTint(drawableColor)
            draw(canvas)
        }

        // Draw volume off
        val angleVolumeOff = 55.toRadians()
        drawableStart = (angleVolumeOff.toX(distance).toInt() - iconSize / 2f).toInt()
        drawableTop = (angleVolumeOff.toY(distance).toInt() - iconSize / 2f).toInt()
        drawableRight = (drawableStart + iconSize).toInt()
        drawableBottom = (drawableTop + iconSize).toInt()
        volumeOff?.apply {
            rect1.left = drawableStart
            rect1.top = drawableTop
            rect1.right = drawableRight
            rect1.bottom = drawableBottom

            bounds = rect1
            setTint(drawableColor)
            draw(canvas)
        }

        // Draw outer scale
        val outerDistance = distance + outerRadius
        for (i in (-42..42) step 2) {
            val angle = i.toRadians()
            val startX = angle.toX(outerDistance) - outerXOffset
            val startY = angle.toY(outerDistance)
            val endX = angle.toX(outerDistance + 10) - outerXOffset
            val endY = angle.toY(outerDistance + 10)

            canvas.drawLine(
                startX, startY, endX, endY,
                if (startY >= yThreshold) paintMain2 else paintBg2
            )
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event?.apply {
            when (action) {
                ACTION_DOWN -> {
                    // If tap on volume_off - set min value
                    if (y > height - 100) {
                        allowDraw = false
                        volumeAngle = 0f
                        yThreshold = Float.MAX_VALUE // Reducing outer scale

                        // set volume
                        adjustVolume(volumeAngle)

                        invalidate()
                    } else {
                        allowDraw = x > centerX

                        performDraw(y)
                    }
                }

                ACTION_MOVE -> {
                    if (allowDraw) {
                        performDraw(y)
                    }
                }

                ACTION_UP -> {
                    allowDraw = false
                }
            }
        }

        return false
    }

    private fun performDraw(y: Float) {
        // Set angle of new volume level
        volumeAngle = -(height - y) * (90 / (height - padding * 2))
        if (volumeAngle < -90f) {
            volumeAngle = -90f
        } else if (volumeAngle > 0f) {
            volumeAngle = 0f
        }

        yThreshold = (volumeAngle + 43).toThreshold()

        // set volume
        adjustVolume(volumeAngle)

        invalidate()
    }

    private fun adjustVolume(volumeAngle: Float) {
        val max = audioService?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val roundedAngle = (volumeAngle).roundToInt()

        if (prevAngle != roundedAngle) {
            val vValue = (-roundedAngle * (max!! / 90f)).roundToInt()

            audioService?.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                vValue,
                AudioManager.FLAG_PLAY_SOUND
            )
            prevAngle = roundedAngle

            // Vibrate on each new volume level
            tryVibrate(roundedAngle)
        }
    }

    private fun tryVibrate(roundedAngle: Int) {
        val elapsedTime = System.currentTimeMillis() - lastVibrationTime

        if (elapsedTime > 100) {
            if (roundedAngle % 2 == 0) {
                lastVibrationTime = System.currentTimeMillis()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(20L, 100))
                } else {
                    vibrator?.vibrate(20L)
                }
            }
        }
    }

    private fun Float.toX(distance: Float): Float = rect.centerX() + distance * cos(this)
    private fun Float.toY(distance: Float): Float = rect.centerY() + distance * sin(this)

    private fun Int.toAngle(): Float {
        val max = audioService?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return -this * (90f / max!!)
    }

    private fun Float.toThreshold(): Float = toRadians().toY(distance + outerRadius)
}



