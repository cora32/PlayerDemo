package io.iskopasi.player_test.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import io.iskopasi.player_test.R
import io.iskopasi.player_test.activities.Consts.MEDIA_REQ_CODE
import io.iskopasi.player_test.databinding.ActivityDeniedPermissionBinding
import io.iskopasi.player_test.databinding.ActivityXmlPlayerBinding
import io.iskopasi.player_test.databinding.LoaderBinding
import io.iskopasi.player_test.models.MediaState
import io.iskopasi.player_test.models.PlayerXMLViewModel
import io.iskopasi.player_test.utils.Utils.e


object Consts {
    const val MEDIA_REQ_CODE = 0
}

@AndroidEntryPoint
class PlayerXMLActivity : AppCompatActivity() {
    private lateinit var binding: ActivityXmlPlayerBinding
    private lateinit var bindingDeniedPermission: ActivityDeniedPermissionBinding
    private lateinit var bindingLoader: LoaderBinding

    //    private val model: PlayerXMLViewModel by viewModels { PlayerXMLViewModel.getFactory(application) }
    private val model: PlayerXMLViewModel by viewModels()
    private val circularProgressDrawable by lazy {
        CircularProgressDrawable(this).apply {
            setColorSchemeColors(Color.WHITE)
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }
    }
    private val detector: GestureDetectorCompat by lazy {
        GestureDetectorCompat(this@PlayerXMLActivity, object : SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                (e1.x - e2.x).let { dx ->
                    when {
                        dx < 0 -> model.prev()
                        dx > 0 -> model.next()
                        else -> {}
                    }
                }

                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private val requester = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        "isGranted: $isGranted".e
        "shouldShowRequestPermissionRationale: ${
            ActivityCompat.shouldShowRequestPermissionRationale(
                this@PlayerXMLActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }".e

        when {
            isGranted -> onGranted()
            !ActivityCompat.shouldShowRequestPermissionRationale(
                this@PlayerXMLActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> onPermaDenied()

            else -> onDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityXmlPlayerBinding.inflate(layoutInflater)
        bindingDeniedPermission = ActivityDeniedPermissionBinding.inflate(layoutInflater)
        bindingLoader = LoaderBinding.inflate(layoutInflater)

        binding.next.setOnClickListener {
            model.next()
        }

        binding.prev.setOnClickListener {
            model.prev()
        }

        binding.play.setOnClickListener {
            model.play()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser)
                    model.onSeek(value)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })

//        bindingDeniedPermission.buttonRequestPermission.setOnClickListener {
//            setContentView(binding.root)
//            requestPermission2()
//        }

        setContentView(bindingLoader.root)

        setObservers()

//        requestPermissionOld()
        requestPermissionContract()
    }

    private fun setObservers() {
        model.isLoading.observe(this) {
            if (it) {
                setContentView(bindingLoader.root)
            } else {
                setContentView(binding.root)
            }
        }

        model.currentTrack.observe(this) {
            binding.name.text = it.name
            binding.album.text = it.album
            binding.artist.text = it.artist
            binding.seekBar.max = it.duration.toInt()
        }

        model.image.observe(this) {
//            binding.iv1.setImageBitmap(it)

            Glide
                .with(this)
                .load(it)
//                .centerCrop()
                .dontTransform()
                .placeholder(circularProgressDrawable)
                .error(R.drawable.album_placeholder)
                .into(binding.iv1)
        }

        model.mediaState.observe(this) {
            if (it == MediaState.PLAYING) {
                binding.play.setImageResource(R.drawable.baseline_pause_24)
            } else {
                binding.play.setImageResource(R.drawable.baseline_play_arrow_24)
            }
        }

        model.position.observe(this) {
            binding.seekBar.progress = it
        }
    }

    private fun onDenied() {
        requestPermissionContract()
    }

    private fun onPermaDenied() {
        model.isLoading.value = false
        setContentView(bindingDeniedPermission.root)
    }

    private fun requestPermissionOld() {
        val c1 = ContextCompat.checkSelfPermission(
            this@PlayerXMLActivity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val c2 = ActivityCompat.shouldShowRequestPermissionRationale(
            this@PlayerXMLActivity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        when {
            ContextCompat.checkSelfPermission(
                this@PlayerXMLActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> onGranted()

            ActivityCompat.shouldShowRequestPermissionRationale(
                this@PlayerXMLActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                AlertDialog.Builder(this@PlayerXMLActivity)
                    .setTitle("We need file permission")
                    .setMessage("Plz!")
                    .setPositiveButton(
                        "OK"
                    ) { dialog, which ->
                        performRequest()
                    }
                    .setNegativeButton("Cancel") { dialog, which ->
                        onPermaDenied()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            }

            else -> performRequest()
        }
    }

    private fun requestPermissionContract() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            AlertDialog.Builder(this@PlayerXMLActivity)
                .setTitle("We need file permission")
                .setMessage("Plz!")
                .setPositiveButton(
                    "OK"
                ) { dialog, which ->
                    requester.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    onPermaDenied()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        } else {
            requester.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun performRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MEDIA_REQ_CODE
        )
    }

    private fun onGranted() {
        model.isLoading.value = false

        model.read()
    }

    private fun onDenied2() {
        AlertDialog.Builder(this@PlayerXMLActivity)
            .setTitle("We need file permission")
            .setMessage("Plz!")
            .setPositiveButton(
                "OK"
            ) { dialog, which ->
                performRequest()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MEDIA_REQ_CODE -> {
                permissions.forEachIndexed { index, perm ->
                    "perm: $index $perm ${grantResults[index]}".e
                    if (perm == Manifest.permission.READ_EXTERNAL_STORAGE && grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                        onGranted()
                    } else {
                        onDenied2()
                    }
                }
            }
        }
    }
}