package io.iskopasi.player_test

import android.content.Context
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

class AnimatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    AppCompatImageView(context, attrs, defStyleAttr) {

    private var mPlayToPauseAnim: AnimatedVectorDrawableCompat? = null
    private var mPauseToPlay: AnimatedVectorDrawableCompat? = null
    private var mFadeOutAnim: Animation? = null
    private var mFadeInAnim: Animation? = null

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        mPlayToPauseAnim = AnimatedVectorDrawableCompat.create(context, R.drawable.play_to_pause)
        mPauseToPlay = AnimatedVectorDrawableCompat.create(context, R.drawable.pause_to_play)
        mFadeOutAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
        mFadeInAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
//        View.inflate(context, R.layout.custom_layout, this)

//        val image_thumb = findViewById<ImageView>(R.id.image_thumb)
//        val text_title = findViewById<TextView>(R.id.text_title)
//
//        val ta = context.obtainStyledAttributes(attrs, R.styleable.CustomView)
//        try {
//            val text = ta.getString(R.styleable.CustomView_text)
//            val drawableId = ta.getResourceId(R.styleable.CustomView_image, 0)
//            if (drawableId != 0) {
//                val drawable = AppCompatResources.getDrawable(context, drawableId)
//                image_thumb.setImageDrawable(drawable)
//            }
//            text_title.text = text
//        } finally {
//            ta.recycle()
//        }
    }

    fun setState(state: Int) {
        when (state) {
            STATE_PLAY -> {
                setImageDrawable(mPlayToPauseAnim)
                mPlayToPauseAnim!!.start()
            }

            STATE_PAUSE -> {
                setImageDrawable(mPauseToPlay)
                mPauseToPlay!!.start()
            }
        }
    }

    fun fadeOut() {
        startAnimation(mFadeOutAnim)
        mFadeOutAnim!!.fillAfter = true
    }

    fun fadeIn() {
        startAnimation(mFadeInAnim)
        mFadeInAnim!!.fillAfter = true
    }

    companion object {
        const val STATE_PLAY = 1
        const val STATE_PAUSE = 2
    }
}

//class AnimatorView : AppCompatImageView {
//    private var mPlayToPauseAnim: AnimatedVectorDrawableCompat? = null
//    private var mPauseToPlay: AnimatedVectorDrawableCompat? = null
//    private var mFadeOutAnim: Animation? = null
//    private var mFadeInAnim: Animation? = null
//
//    constructor(context: Context) : super(context) {
//        init(context)
//    }
//
//    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
//        init(context)
//    }
//
//    constructor(
//        context: Context,
//        attrs: AttributeSet?,
//        defStyleAttr: Int
//    ) : super(context, attrs, defStyleAttr) {
//        init(context)
//    }
//
//    private fun init(context: Context) {
//        mPlayToPauseAnim = AnimatedVectorDrawableCompat.create(context, R.drawable.play_to_pause)
//        mPauseToPlay = AnimatedVectorDrawableCompat.create(context, R.drawable.pause_to_play)
//        mFadeOutAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
//        mFadeInAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
//    }
//
//    fun setState(state: Int) {
//        when (state) {
//            STATE_PLAY -> {
//                setImageDrawable(mPlayToPauseAnim)
//                mPlayToPauseAnim!!.start()
//            }
//
//            STATE_PAUSE -> {
//                setImageDrawable(mPauseToPlay)
//                mPauseToPlay!!.start()
//            }
//        }
//    }
//
//    fun fadeOut() {
//        startAnimation(mFadeOutAnim)
//        mFadeOutAnim!!.fillAfter = true
//    }
//
//    fun fadeIn() {
//        startAnimation(mFadeInAnim)
//        mFadeInAnim!!.fillAfter = true
//    }
//
//    companion object {
//        const val STATE_PLAY = 1
//        const val STATE_PAUSE = 2
//    }
//}