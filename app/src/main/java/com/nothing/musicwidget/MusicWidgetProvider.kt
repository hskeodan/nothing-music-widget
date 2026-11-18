
package com.nothing.musicwidget

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val captureRequest = 1001
    private val audioPermissionRequest = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        val title = TextView(this).apply {
            text = "nothing(R)"
            textSize = 30f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
        }

        val status = TextView(this).apply {
            text = "VK X  •  🔴 LIVE"
            textSize = 16f
            setTextColor(android.graphics.Color.RED)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 32)
        }

        val startButton = Button(this).apply {
            text = "START VISUALIZER"
            setOnClickListener {
                checkAudioPermission()
            }
        }

        val stopButton = Button(this).apply {
            text = "STOP VISUALIZER"
            setOnClickListener {
                stopService(
                    Intent(this@MainActivity, AudioCaptureService::class.java)
                )
            }
        }

        root.addView(title)
        root.addView(status)
        root.addView(startButton)
        root.addView(stopButton)

        setContentView(root)
    }

    private fun checkAudioPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                audioPermissionRequest
            )
        } else {
            requestCapturePermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == audioPermissionRequest &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            requestCapturePermission()
        }
    }

    private fun requestCapturePermission() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE)
            as MediaProjectionManager

        startActivityForResult(
            manager.createScreenCaptureIntent(),
            captureRequest
        )
    }

    @Deprecated("Legacy result callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == captureRequest &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            val serviceIntent = Intent(
                this,
                AudioCaptureService::class.java
            ).apply {
                putExtra(AudioCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(AudioCaptureService.EXTRA_DATA, data)
            }

            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }
}
