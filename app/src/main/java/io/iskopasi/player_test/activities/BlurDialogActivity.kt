package io.iskopasi.player_test.activities

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import io.iskopasi.player_test.R
import java.util.function.Consumer


class BlurDialogActivity : AppCompatActivity() {
    private val mBackgroundBlurRadius = 80
    private val mBlurBehindRadius = 20

    // We set a different dim amount depending on whether window blur is enabled or disabled
    private val mDimAmountWithBlur = 0.1f
    private val mDimAmountNoBlur = 0.4f

    // We set a different alpha depending on whether window blur is enabled or disabled
    private val mWindowBackgroundAlphaWithBlur = 170
    private val mWindowBackgroundAlphaNoBlur = 255

    // Use a rectangular shape drawable for the window background. The outline of this drawable
    // dictates the shape and rounded corners for the window background blur area.
    private var mWindowBackgroundDrawable: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_left)

        mWindowBackgroundDrawable = AppCompatResources.getDrawable(this, R.drawable.dialog_bg)
        window.setBackgroundDrawable(mWindowBackgroundDrawable)


        if (buildIsAtLeastS()) {
            // Enable blur behind. This can also be done in xml with R.attr#windowBlurBehindEnabled
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)

            // Register a listener to adjust window UI whenever window blurs are enabled/disabled
            setupWindowBlurListener()
        } else {
            // Window blurs are not available prior to Android S
            updateWindowForBlurs(false /* blursEnabled */)
        }

        // Enable dim. This can also be done in xml, see R.attr#backgroundDimEnabled
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    /**
     * Set up a window blur listener.
     *
     * Window blurs might be disabled at runtime in response to user preferences or system states
     * (e.g. battery saving mode). WindowManager#addCrossWindowBlurEnabledListener allows to
     * listen for when that happens. In that callback we adjust the UI to account for the
     * added/missing window blurs.
     *
     * For the window background blur we adjust the window background drawable alpha:
     * - lower when window blurs are enabled to make the blur visible through the window
     * background drawable
     * - higher when window blurs are disabled to ensure that the window contents are readable
     *
     * For window blur behind we adjust the dim amount:
     * - higher when window blurs are disabled - the dim creates a depth of field effect,
     * bringing the user's attention to the dialog window
     * - lower when window blurs are enabled - no need for a high alpha, the blur behind is
     * enough to create a depth of field effect
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private fun setupWindowBlurListener() {
        val windowBlurEnabledListener: Consumer<Boolean> =
            Consumer<Boolean> { blursEnabled: Boolean ->
                this.updateWindowForBlurs(
                    blursEnabled
                )
            }
        window.decorView.addOnAttachStateChangeListener(
            object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    windowManager.addCrossWindowBlurEnabledListener(
                        windowBlurEnabledListener
                    )
                }

                override fun onViewDetachedFromWindow(v: View) {
                    windowManager.removeCrossWindowBlurEnabledListener(
                        windowBlurEnabledListener
                    )
                }
            })
    }

    private fun updateWindowForBlurs(blursEnabled: Boolean) {
        mWindowBackgroundDrawable!!.alpha =
            if (blursEnabled && mBackgroundBlurRadius > 0) mWindowBackgroundAlphaWithBlur else mWindowBackgroundAlphaNoBlur
        window.setDimAmount(if (blursEnabled && mBlurBehindRadius > 0) mDimAmountWithBlur else mDimAmountNoBlur)

        if (buildIsAtLeastS()) {
            // Set the window background blur and blur behind radii
            window.setBackgroundBlurRadius(mBackgroundBlurRadius)
            window.attributes.blurBehindRadius = mBlurBehindRadius
            window.attributes = window.attributes
        }
    }

    private fun buildIsAtLeastS(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

}