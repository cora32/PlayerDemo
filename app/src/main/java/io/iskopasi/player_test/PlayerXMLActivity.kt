package io.iskopasi.player_test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.iskopasi.player_test.Consts.MEDIA_REQ_CODE
import io.iskopasi.player_test.Utils.e
import io.iskopasi.player_test.databinding.ActivityDeniedPermissionBinding
import io.iskopasi.player_test.databinding.ActivityXmlPlayerBinding


object Consts {
    const val MEDIA_REQ_CODE = 0
}

class PlayerXMLActivity : AppCompatActivity() {
    private lateinit var binding: ActivityXmlPlayerBinding
    private lateinit var bindingDeniedPermission: ActivityDeniedPermissionBinding
    private val controller = PlayerController()

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

        bindingDeniedPermission.buttonRequestPermission.setOnClickListener {
            setContentView(binding.root)
            requestPermission2()
        }

        setContentView(binding.root)

//        requestPermission1()
        requestPermission2()
    }

    private fun onDenied() {
        requestPermission2()
    }

    private fun onPermaDenied() {
        setContentView(bindingDeniedPermission.root)
    }

    private fun requestPermission1() {
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
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            }
            else -> performRequest()
        }
    }

    private fun requestPermission2() {
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
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        } else {
            requester.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun performRequest() {
        requestPermissions(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MEDIA_REQ_CODE
        )
    }

    private fun onGranted() {
        controller.read()
    }
}