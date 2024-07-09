package io.iskopasi.player_test.views

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding
import androidx.window.layout.WindowMetricsCalculator
import io.iskopasi.player_test.utils.Utils.e
import kotlin.math.abs
import kotlin.math.round
import kotlin.reflect.KClass


enum class SlidingScreenPosition(val xOffset: Int, val yOffset: Int) {
    CENTER(0, 0),
    LEFT(-1, 0),
    RIGHT(1, 0),
    TOP(0, -1),
    BOTTOM(0, 1),
    TOP_LEFT(-1, -1),
    TOP_RIGHT(1, -1),
    BOTTOM_RIGHT(1, 1),
    BOTTOM_LEFT(-1, 1),
}

data class SlidingScreen<T : ViewBinding>(
    val id: Int,
    val position: SlidingScreenPosition,
    val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> T,
)


class SlidingContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var oldRootX = 0f
    private var oldRootY = 0f
    private var startX = 0f
    private var startY = 0f
    private var parallaxDeltaX = 0f
    private var parallaxDeltaY = 0f
    private var containerWidth = 0
    private var containerHeight = 0
    private var currentState: SlidingScreenPosition = SlidingScreenPosition.CENTER
    private var prevState: SlidingScreenPosition = SlidingScreenPosition.CENTER
    private val viewMap = mutableMapOf<String, View>()
    private val velocityThreshold = 2000
    val bindingMap = mutableMapOf<KClass<*>, ViewBinding>()
    private val metrics by lazy {
        WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(context.applicationContext).bounds
    }
    private val frameWidth = metrics.width()
    private val frameHeight = metrics.height()
    private val startXCoordMap = mutableMapOf<View, Float>()
    private val startYCoordMap = mutableMapOf<View, Float>()
    private var tempDeltaX = 0f
    private var tempDeltaY = 0f
    private var globalDeltaX = 0f
    private var globalDeltaY = 0f
    private var savedGlobalDeltaX = 0f
    private var savedGlobalDeltaY = 0f
    private var minXOffset = 0
    private var maxXOffset = 0
    private var minYOffset = 0
    private var maxYOffset = 0
    private var animationX: ValueAnimator = ValueAnimator.ofFloat()
    private var animationY: ValueAnimator = ValueAnimator.ofFloat()
    private val gestureDetector by lazy {
        GestureDetector(context.applicationContext,
            object : GestureDetector.OnGestureListener {

                override fun onDown(e: MotionEvent): Boolean {
                    return false
                }

                override fun onShowPress(e: MotionEvent) {

                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return false
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    return false
                }

                override fun onLongPress(e: MotionEvent) {

                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    var consume = false
                    if (abs(velocityX) > abs(velocityY)) {
                        if (velocityX > velocityThreshold) {
                            goLeft()

                            consume = true
                        } else if (velocityX < -velocityThreshold) {
                            goRight()

                            consume = true
                        } else {
                            "velocityX = 0".e
                        }
                    } else {
                        if (velocityY > velocityThreshold) {
                            goUp()

                            consume = true
                        } else if (velocityY < -velocityThreshold) {
                            goDown()

                            consume = true
                        } else {
                            "velocityY = 0".e
                        }
                    }

                    return consume
                }
            })
    }
    private var xScreenIndex = 1
    private var yScreenIndex = 1
    private val positionMap: List<List<SlidingScreenPosition>> = listOf(
        listOf(
            SlidingScreenPosition.TOP_LEFT,
            SlidingScreenPosition.TOP,
            SlidingScreenPosition.TOP_RIGHT,
        ),
        listOf(
            SlidingScreenPosition.LEFT,
            SlidingScreenPosition.CENTER,
            SlidingScreenPosition.RIGHT,
        ),
        listOf(
            SlidingScreenPosition.BOTTOM_LEFT,
            SlidingScreenPosition.BOTTOM,
            SlidingScreenPosition.BOTTOM_RIGHT,
        )
    )

    init {
        isClickable = true
    }

    private fun resetIndexes() {
        currentState = prevState

        "--> Resetting indexes from $xScreenIndex $yScreenIndex to ${prevState.xOffset + 1} ${prevState.yOffset + 1}".e
        xScreenIndex = prevState.xOffset + 1
        yScreenIndex = prevState.yOffset + 1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val consumed = gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // If user touched view before animations completed, reset frame indexes
                if (stopAnimations()) {
                    resetIndexes()
                }

                startX = event.x
                startY = event.y
                savedGlobalDeltaX = globalDeltaX
                savedGlobalDeltaY = globalDeltaY
            }

            MotionEvent.ACTION_MOVE -> {
                tempDeltaX = event.x - startX
                tempDeltaY = event.y - startY
                globalDeltaX = savedGlobalDeltaX + tempDeltaX
                globalDeltaY = savedGlobalDeltaY + tempDeltaY

                moveX(globalDeltaX)
                moveY(globalDeltaY)
            }

            MotionEvent.ACTION_UP -> {
                prevState = currentState
                currentState = positionMap[yScreenIndex][xScreenIndex]

                runAnimations()
            }
        }

        return true
    }

    private fun runAnimations() {
        animationX = getXAnimator()
        animationY = getYAnimator()

        "--> Animating from $prevState to $currentState $xScreenIndex $yScreenIndex".e

        AnimatorSet().apply {
            playTogether(animationX, animationY)
            start()
        }
    }

    private fun goUp() {
        "--> goUp:".e

        if (yScreenIndex > 0) {
            yScreenIndex--
        }
    }

    private fun goDown() {
        "--> goDown:".e

        if (yScreenIndex < 2) {
            yScreenIndex++
        }
    }

    private fun goRight() {
        "--> goRight:".e

        if (xScreenIndex < 2) {
            xScreenIndex++
        }
    }

    private fun goLeft() {
        "--> goLeft:".e

        if (xScreenIndex > 0) {
            xScreenIndex--
        }
    }

    private fun getXValue(): Float {
        val destinationX = if (currentState.xOffset > prevState.xOffset) {
            -frameWidth.toFloat()
        } else if (currentState.xOffset < prevState.xOffset) {
            frameWidth.toFloat()
        } else 0f

        return destinationX - tempDeltaX
    }

    private fun getYValue(): Float {
        val destinationY = if (currentState.yOffset > prevState.yOffset) {
            -frameHeight.toFloat()
        } else if (currentState.yOffset < prevState.yOffset) {
            frameHeight.toFloat()
        } else 0f

        return destinationY - tempDeltaY
    }

    private fun Float.getXAdjustment(savedGlobalDeltaX: Float): Float {
        val snapped = round((this + savedGlobalDeltaX) / frameWidth) * frameWidth
        return snapped - (this + savedGlobalDeltaX)
    }

    private fun Float.getYAdjustment(savedGlobalDeltaY: Float): Float {
        val snapped = round((this + savedGlobalDeltaY) / frameHeight) * frameHeight
        return snapped - (this + savedGlobalDeltaY)
    }

    private fun getXAnimator(): ValueAnimator {
        savedGlobalDeltaX = globalDeltaX

        val xValue = getXValue()
        val xAdjustment = xValue.getXAdjustment(savedGlobalDeltaX)

        return ValueAnimator.ofFloat(0f, xValue + xAdjustment)
            .apply {
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim ->
                    globalDeltaX = savedGlobalDeltaX + anim.animatedValue as Float
                    moveX(globalDeltaX)
                }
            }
    }

    private fun getYAnimator(): ValueAnimator {
        savedGlobalDeltaY = globalDeltaY

        val yValue = getYValue()
        val yAdjustment = yValue.getYAdjustment(savedGlobalDeltaY)

        return ValueAnimator.ofFloat(0f, yValue + yAdjustment)
            .apply {
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim ->
                    globalDeltaY = savedGlobalDeltaY + anim.animatedValue as Float
                    moveY(globalDeltaY)
                }
            }
    }

    private fun stopAnimations(): Boolean {
        val wasRunning = animationX.isRunning || animationY.isRunning

        animationX.cancel()
        animationY.cancel()

        return wasRunning
    }

    private fun moveX(deltaX: Float) {
        parallaxDeltaX = deltaX / 3f
        x = oldRootX + parallaxDeltaX

        for (view in viewMap.values) {
            view.x = startXCoordMap[view]!! + deltaX - parallaxDeltaX
        }
    }

    private fun moveY(deltaY: Float) {
        parallaxDeltaY = deltaY / 3f
        y = oldRootY + parallaxDeltaY

        for (view in viewMap.values) {
            view.y = startYCoordMap[view]!! + deltaY - parallaxDeltaY
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        containerWidth = MeasureSpec.getSize(widthMeasureSpec)
        containerHeight = MeasureSpec.getSize(heightMeasureSpec)

        // Setting viewport to center of container
        x = -(containerWidth / 2f - frameWidth / 2f)
        y = -(containerHeight / 2f - frameHeight / 2f)
        "--> Setting initial coordinates to $x $y".e

        setMeasuredDimension(containerWidth, containerHeight)

        // Start container coordinates
        oldRootX = x
        oldRootY = y
    }

    private fun adjustContainerSize() {
        val xFactor = maxXOffset + abs(minXOffset) + 1
        val yFactor = maxYOffset + abs(minYOffset) + 1

        "--> xFactor: $xFactor; yFactor: $yFactor;  container width: ${frameWidth * xFactor}; height: ${frameHeight * yFactor}".e
        layoutParams = LayoutParams(frameWidth * xFactor, frameHeight * yFactor)
    }

    inline fun <reified T : ViewBinding> getBinding(): T = bindingMap[T::class] as T

    fun initialize(screens: List<SlidingScreen<ViewBinding>>) {
        val inflater = LayoutInflater.from(context)

        for (screen in screens) {
            val binding = screen.bindingInflater(inflater, this, true)
            val view = binding.root

            viewMap[screen.position.name] = view
            bindingMap[binding::class] = binding

            // Place view on their places
            view.post {
                view.x =
                    containerWidth / 2f - view.width / 2f + (screen.position.xOffset) * frameWidth + abs(
                        x
                    )
                view.y =
                    containerHeight / 2f - view.height / 2f + screen.position.yOffset * frameHeight + abs(
                        y
                    )
                view.layoutParams = LayoutParams(frameWidth, frameHeight)

                // Saving views initial coordinates to be used as starting point in transitions
                startXCoordMap[view] = view.x
                startYCoordMap[view] = view.y

                "--> Frame: $frameWidth, $frameHeight; ${view.x}, ${view.y}".e
            }

            // Find min and max offsets
            findMinMaxOffsets(screen.position)
        }

        // Setting container width/height
        adjustContainerSize()
    }

    private fun findMinMaxOffsets(position: SlidingScreenPosition) {
        if (position.xOffset > maxXOffset) {
            maxXOffset = position.xOffset
        } else if (position.xOffset < minXOffset) {
            minXOffset = position.xOffset
        }

        if (position.yOffset > maxYOffset) {
            maxYOffset = position.yOffset
        } else if (position.yOffset < minYOffset) {
            minYOffset = position.yOffset
        }
    }
}