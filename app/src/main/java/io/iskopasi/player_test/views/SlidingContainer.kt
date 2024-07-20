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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import androidx.window.layout.WindowMetricsCalculator
import io.iskopasi.player_test.databinding.MenuLayoutBinding
import io.iskopasi.player_test.utils.Utils.e
import io.iskopasi.player_test.utils.Utils.ui
import kotlinx.coroutines.delay
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
    private var deltaThreshold = 120f
    private var tempDeltaX = 0f
    var tempDeltaY = 0f
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
                        }
                    } else {
                        if (velocityY > velocityThreshold) {
                            goUp()

                            consume = true
                        } else if (velocityY < -velocityThreshold) {
                            goDown()

                            consume = true
                        } else {
                            "-->y ${e1!!.y} ${e2.y} ".e
                            if (e2.y - e1!!.y > frameHeight / 2f) {
                                goUp()
                            } else if (e1!!.y - e2!!.y > frameHeight / 2f) {
                                goDown()
                            }
                        }
                    }

                    return consume
                }
            })
    }
    private var xScreenIndex = 1
    private var yScreenIndex = 1
    private var centerScreenInitialX = 0f
    private var centerScreenInitialY = 0f
    private val arrowTransitionWidth = (frameWidth * 0.03f) * 2f
    private val arrowTransitionHeight = (frameHeight * 0.07f) * 2f
    private var maxXPosition = 3
    private var maxYPosition = 3
    private var topArrow: SliderArrow? = null
    private var leftArrow: SliderArrow? = null
    private var rightArrow: SliderArrow? = null
    private var bottomArrow: SliderArrow? = null
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
    private val menuBinding by lazy {
        MenuLayoutBinding.inflate(LayoutInflater.from(context.applicationContext), this, false)
//        LayoutInflater.from(context.applicationContext).inflate(R.layout.menu_layout, this, false)
    }
    private var isActionDown = false

    init {
        isClickable = true

        toggleArrows()

        ui {
            // Show arrows at first
            delay(1000L)

            leftArrow?.hide()
            topArrow?.hide()
            rightArrow?.hide()
            bottomArrow?.hide()
        }
    }

    private fun resetIndexes() {
        currentState = prevState

        "--> Resetting indexes from $xScreenIndex $yScreenIndex to ${prevState.xOffset + 1} ${prevState.yOffset + 1}".e
        xScreenIndex = prevState.xOffset + 1
        yScreenIndex = prevState.yOffset + 1
    }

    fun processTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isActionDown = true

                // If user touched view before animations completed, reset frame indexes
                if (stopAnimations()) {
                    resetIndexes()
                }

                startX = event.rawX
                startY = event.rawY
                savedGlobalDeltaX = globalDeltaX
                savedGlobalDeltaY = globalDeltaY

                // Animate visibility of arrows
                toggleArrows()
            }

            MotionEvent.ACTION_MOVE -> {
                tempDeltaX = event.rawX - startX
                tempDeltaY = event.rawY - startY
                globalDeltaX = savedGlobalDeltaX + tempDeltaX
                globalDeltaY = savedGlobalDeltaY + tempDeltaY

                moveX(globalDeltaX)
                moveY(globalDeltaY)
            }

            MotionEvent.ACTION_UP -> {
                isActionDown = false
                val consumed = gestureDetector.onTouchEvent(event)

                if (!consumed) {
                    if (tempDeltaX > deltaThreshold) {
                        goLeft()
                    } else if (tempDeltaX < -deltaThreshold) {
                        goRight()
                    } else if (tempDeltaY > deltaThreshold) {
                        goUp()
                    } else if (tempDeltaY < -deltaThreshold) {
                        goDown()
                    }
                }
                prevState = currentState
                currentState = positionMap[yScreenIndex][xScreenIndex]

                // Animate screen sliding
                runAnimations()

                // Display only available arrows
                toggleArrows()

                // Hide arrows after a sec of action up
                ui {
                    delay(1000L)

                    if (!isActionDown) {
                        leftArrow?.hide()
                        topArrow?.hide()
                        rightArrow?.hide()
                        bottomArrow?.hide()
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        "slider: onTouchEvent: ${event.action}  ${event.rawX}".e

        processTouchEvent(event)

        return true
    }


//    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
//        "slider: onInterceptTouchEvent: ${event.action}".e
//
//        processTouchEvent(event)
//
////        super.onInterceptTouchEvent(event)
//
//        return false
//    }


    private fun toggleArrows() {
        when (currentState) {
            SlidingScreenPosition.CENTER -> {
                if (canGoLeft()) leftArrow?.show()
                if (canGoUp()) topArrow?.show()
                if (canGoRight()) rightArrow?.show()
                if (canGoDown()) bottomArrow?.show()
            }

            SlidingScreenPosition.LEFT -> {
                leftArrow?.show()
                topArrow?.hide()
                rightArrow?.hide()
                bottomArrow?.hide()
            }

            SlidingScreenPosition.RIGHT -> {
                leftArrow?.hide()
                topArrow?.hide()
                rightArrow?.show()
                bottomArrow?.hide()
            }

            SlidingScreenPosition.TOP -> {
                leftArrow?.hide()
                topArrow?.show()
                rightArrow?.hide()
                bottomArrow?.hide()
            }

            SlidingScreenPosition.BOTTOM -> TODO()
            SlidingScreenPosition.TOP_LEFT -> TODO()
            SlidingScreenPosition.TOP_RIGHT -> TODO()
            SlidingScreenPosition.BOTTOM_RIGHT -> TODO()
            SlidingScreenPosition.BOTTOM_LEFT -> TODO()
        }
    }

    private fun runAnimations() {
        animationX = getXAnimator()
        animationY = getYAnimator()

        "--> Animating from $prevState to $currentState $xScreenIndex $yScreenIndex".e

        if (prevState != currentState) {
            hideMenu()
        }

        AnimatorSet().apply {
            playTogether(animationX, animationY)
            start()
        }
    }

    private fun canGoUp(): Boolean {
        if (yScreenIndex < 1) return false

        val expectedPositionName = positionMap[yScreenIndex - 1][xScreenIndex].name
        return viewMap[expectedPositionName] != null
    }

    private fun canGoDown(): Boolean {
        if (xScreenIndex >= maxYPosition - 1) return false

        val expectedPositionName = positionMap[yScreenIndex + 1][xScreenIndex].name
        return viewMap[expectedPositionName] != null
    }

    private fun canGoRight(): Boolean {
        if (xScreenIndex >= maxXPosition - 1) return false

        val expectedPositionName = positionMap[yScreenIndex][xScreenIndex + 1].name
        return viewMap[expectedPositionName] != null
    }

    private fun canGoLeft(): Boolean {
        if (xScreenIndex < 1) return false

        val expectedPositionName = positionMap[yScreenIndex][xScreenIndex - 1].name
        return viewMap[expectedPositionName] != null
    }

    private fun goUp() {
        if (yScreenIndex > 0) {
            if (canGoUp()) {
                yScreenIndex--
            }
        }
    }

    private fun goDown() {
        if (yScreenIndex < 2) {
            if (canGoDown()) {
                yScreenIndex++
            }
        }
    }

    private fun goRight() {
        if (xScreenIndex < 2) {
            if (canGoRight()) {
                xScreenIndex++
            }
        }
    }

    private fun goLeft() {
        if (xScreenIndex > 0) {
            if (canGoLeft()) {
                xScreenIndex--
            }
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

        // Moving screens on x-axis
        for (view in viewMap.values) {
            view.x = startXCoordMap[view]!! + deltaX - parallaxDeltaX
        }

        val currentX = frameWidth - centerScreenInitialX + deltaX
        val fraction = currentX / frameWidth

        // Moving arrows on x-axis
        leftArrow?.let {
            it.x = it.initialX + deltaX - parallaxDeltaX - fraction * arrowTransitionWidth
            it.flingX(deltaX)
        }
        rightArrow?.let {
            it.x = it.initialX + deltaX - parallaxDeltaX - fraction * arrowTransitionWidth
            it.flingX(deltaX)
        }
    }

    private fun moveY(deltaY: Float) {
        parallaxDeltaY = deltaY / 3f
        y = oldRootY + parallaxDeltaY

        for (view in viewMap.values) {
            view.y = startYCoordMap[view]!! + deltaY - parallaxDeltaY
        }

        val currentY = frameHeight - centerScreenInitialY + deltaY - frameHeight / 2f
        val fraction = currentY / frameHeight

        // Moving arrows on y-axis
        topArrow?.let {
            it.y =
                it.initialY + deltaY - parallaxDeltaY - fraction * (arrowTransitionHeight - 80.dp.value)
            it.flingY(deltaY)
        }
        bottomArrow?.let {
            it.y = it.initialY + deltaY - parallaxDeltaY - fraction * arrowTransitionHeight
            it.flingY(deltaY)
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

    inline fun <reified T : ViewBinding> getBinding(): T = bindingMap[T::class] as T

    fun initialize(screens: List<SlidingScreen<ViewBinding>>) {
        val inflater = LayoutInflater.from(context)

        for (screen in screens) {
            val binding = screen.bindingInflater(inflater, this, true)
            val view = binding.root

            viewMap[screen.position.name] = view
            bindingMap[binding::class] = binding

            view.post {
                // Place view in its place
                val screenX =
                    containerWidth / 2f - view.width / 2f + screen.position.xOffset * frameWidth + abs(
                        x
                    )
                val screenY =
                    containerHeight / 2f - view.height / 2f + screen.position.yOffset * frameHeight + abs(
                        y
                    )

                // Saving views initial coordinates to be used as starting point in transitions
                startXCoordMap[view] = screenX
                startYCoordMap[view] = screenY

                view.x = screenX
                view.y = screenY
                view.layoutParams = LayoutParams(frameWidth, frameHeight)

                "--> Frame: $frameWidth, $frameHeight; ${view.x}, ${view.y}".e

                // Placing navigational arrows at center after center screen got all its sizes
                if (screen.position.name == SlidingScreenPosition.CENTER.name) {
                    // Center screen coordinates used as starting point for arrows animations
                    centerScreenInitialX = screenX
                    centerScreenInitialY = screenY

                    topArrow = getTopArrow(screenX, screenY)
                    leftArrow = getLeftArrow(screenX, screenY)
                    rightArrow = getRightArrow(screenX, screenY)
                    bottomArrow = getBottomArrow(screenX, screenY)

                    addView(topArrow)
                    addView(leftArrow)
                    addView(rightArrow)
                    addView(bottomArrow)
                }
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

    private fun getTopArrow(centerScreenX: Float, centerScreenY: Float): SliderArrow =
        SliderArrowHorizontal(context, name = "top").apply {
            x = centerScreenX + frameWidth / 2f - width / 2f
            y = centerScreenY + arrowTransitionHeight / 2f - height / 2f - 50.dp.value

            initialX = x
            initialY = y
        }

    private fun getLeftArrow(centerScreenX: Float, centerScreenY: Float): SliderArrow =
        SliderArrowVertical(context, name = "left").apply {
            x = centerScreenX + arrowTransitionWidth / 2f - width / 2f
            y = centerScreenY + frameHeight / 2f - height / 2f

            initialX = x
            initialY = y
        }

    private fun getRightArrow(centerScreenX: Float, centerScreenY: Float): SliderArrow =
        SliderArrowVertical(context, name = "right").apply {
            x = centerScreenX + frameWidth - arrowTransitionWidth / 2f - width / 2f
            y = centerScreenY + frameHeight / 2f - height / 2f

            initialX = x
            initialY = y
        }

    private fun getBottomArrow(centerScreenX: Float, centerScreenY: Float): SliderArrow =
        SliderArrowHorizontal(context, name = "bottom").apply {
            x = centerScreenX + frameWidth / 2f - width / 2f
            y = centerScreenY + frameHeight - arrowTransitionHeight / 2f - height / 2f

            initialX = x
            initialY = y
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