package io.iskopasi.player_test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.iskopasi.player_test.Consts.MEDIA_REQ_CODE
import io.iskopasi.player_test.Utils.e
import io.iskopasi.player_test.databinding.ActivityDeniedPermissionBinding
import io.iskopasi.player_test.databinding.ActivityXmlPlayerBinding
import io.iskopasi.player_test.databinding.LoaderBinding
import io.iskopasi.player_test.models.PlayerXMLViewModel


object Consts {
    const val MEDIA_REQ_CODE = 0
}

class PlayerXMLActivity : AppCompatActivity() {
    private lateinit var binding: ActivityXmlPlayerBinding
    private lateinit var bindingDeniedPermission: ActivityDeniedPermissionBinding
    private lateinit var bindingLoader: LoaderBinding
    private val model: PlayerXMLViewModel by viewModels { PlayerXMLViewModel.Factory }

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

        Icons.Default.PlayArrow

        binding = ActivityXmlPlayerBinding.inflate(layoutInflater)
        bindingDeniedPermission = ActivityDeniedPermissionBinding.inflate(layoutInflater)
        bindingLoader = LoaderBinding.inflate(layoutInflater)

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
            "model.isLoading: ${model.isLoading.value}".e
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
                    .setNegativeButton("Cancel", { dialog, which ->
                        onPermaDenied()
                    })
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

        model.read(this)
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