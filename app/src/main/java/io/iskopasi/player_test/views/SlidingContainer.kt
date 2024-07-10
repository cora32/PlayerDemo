package io.iskopasi.player_test.views

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import androidx.window.layout.WindowMetricsCalculator
import io.iskopasi.player_test.databinding.MenuLayoutBinding
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
    var menuOnPlay: ((Int) -> Unit)? = null
    var menuOnInfo: ((Int) -> Unit)? = null
    var menuOnShare: ((Int) -> Unit)? = null

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
    private val vibrator by lazy {
        ContextCompat.getSystemService(
            context.applicationContext,
            Vibrator::class.java
        )
    }

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
        if (yScreenIndex > 0) {
            yScreenIndex--
        }
    }

    private fun goDown() {
        if (yScreenIndex < 2) {
            yScreenIndex++
        }
    }

    private fun goRight() {
        if (xScreenIndex < 2) {
            xScreenIndex++
        }
    }

    private fun goLeft() {
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

    private fun adjustContainerSize() {
        val xFactor = maxXOffset + abs(minXOffset) + 1
        val yFactor = maxYOffset + abs(minYOffset) + 1

        containerWidth = frameWidth * xFactor
        containerHeight = frameHeight * yFactor

        "--> xFactor: $xFactor; yFactor: $yFactor;  container width: ${frameWidth * xFactor}; height: ${frameHeight * yFactor}".e
        layoutParams = LayoutParams(frameWidth * xFactor, frameHeight * yFactor)
    }

    private val menuBinding by lazy {
        MenuLayoutBinding.inflate(LayoutInflater.from(context.applicationContext), this, false)
//        LayoutInflater.from(context.applicationContext).inflate(R.layout.menu_layout, this, false)
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

        // Setting viewport to center of container
        x = -(containerWidth / 2f - frameWidth / 2f)
        y = -(containerHeight / 2f - frameHeight / 2f)
        "--> Setting initial coordinates to $x $y".e

        // Initial container coordinates
        oldRootX = x
        oldRootY = y

        // Adding menu layout
        addView(menuBinding.root)
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

    private fun setMenuSize(menuWidth: Int, menuHeight: Int) {
        val menuView = menuBinding.root
        val currentVisibleScreen = viewMap[currentState.name]!!

        val offscreenOffset = currentVisibleScreen.x - (menuView.x - menuWidth)
        if (offscreenOffset > 0) {
            menuView.x += offscreenOffset
        }

        val oldX = menuView.x
        val animationX = ValueAnimator.ofInt(0, menuWidth).apply {
            interpolator = DecelerateInterpolator()
            duration = 150L
            addUpdateListener { anim ->
                menuView.x = oldX - anim.animatedValue as Int
            }
        }
        val animationWidth = ValueAnimator.ofInt(0, menuWidth).apply {
            interpolator = DecelerateInterpolator()
            duration = 150L
            addUpdateListener { anim ->
                menuView.layoutParams.width = anim.animatedValue as Int
            }
        }
        val animationHeight = ValueAnimator.ofInt(0, menuHeight).apply {
            interpolator = DecelerateInterpolator()
            duration = 150L
            addUpdateListener { anim ->
                menuView.post {
                    menuView.layoutParams.height = anim.animatedValue as Int
                    menuView.requestLayout()
                }
            }
        }

        AnimatorSet().apply {
            playTogether(animationX, animationWidth, animationHeight)
            start()
        }
    }

    private fun setMenuPosition(event: MotionEvent) {
        val menuView = menuBinding.root
        val currentVisibleScreen = viewMap[currentState.name]
        menuView.x = currentVisibleScreen!!.x + event.rawX
        menuView.y = currentVisibleScreen.y + event.rawY

        // Correcting X and Y coordinates so the menu wont go out of screen
        val offscreenX = currentVisibleScreen.x - menuView.x
        val offscreenY = (menuView.y + menuView.height) - (currentVisibleScreen.y + frameHeight)
        if (offscreenX > 0) {
            menuView.x += offscreenX
        }
        if (offscreenY > 0) {
            menuView.y -= offscreenY
        }
    }

    fun showMenu(event: MotionEvent, index: Int) {
        val menuView = menuBinding.root

        menuBinding.menu1.setOnClickListener {
            menuOnPlay?.invoke(index)
            hideMenu()
        }
        menuBinding.menu2.setOnClickListener {
            menuOnInfo?.invoke(index)
            hideMenu()
        }
        menuBinding.menu3.setOnClickListener {
            menuOnShare?.invoke(index)
            hideMenu()
        }

        menuView.visibility = GONE

        // Vibrate on menu show
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50L, 255))
        } else {
            vibrator?.vibrate(50L)
        }

        // Saving actual size
        val menuWidth = menuView.width
        val menuHeight = menuView.height

        // Setting size to 0 to animate to actual size
        menuView.layoutParams.width = 0
        menuView.layoutParams.height = 0

        menuView.visibility = VISIBLE

        setMenuPosition(event)
        setMenuSize(menuWidth, menuHeight)
    }

    fun hideMenu() {
        val menuView = menuBinding.root
        if (menuView.visibility != GONE) {
            menuView.visibility = GONE
        }
    }
}