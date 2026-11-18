
package com.nothing.musicwidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaProjection
import android.media.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlin.math.sqrt

class AudioCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "projection_data"
        const val CHANNEL_ID = "audio_capture"
        private const val NOTIFICATION_ID = 1001
    }

    private var projection: MediaProjection? = null
    private var recorder: AudioRecord? = null
    private var captureThread: Thread? = null

    @Volatile
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (Build.VERSION.SDK_INT < 29) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0

        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }

        if (data == null || resultCode == 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

            projection = manager.getMediaProjection(resultCode, data)

            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture()
                    stopSelf()
                }
            }, null)

            startCapture()

        } catch (e: Exception) {
            Log.e("AudioCaptureService", "Capture failed", e)
            stopCapture()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startCapture() {
        val config = AudioPlaybackCaptureConfiguration.Builder(projection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val sampleRate = 44100

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBuffer <= 0) {
            stopSelf()
            return
        }

        recorder = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuffer * 2)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            stopSelf()
            return
        }

        recorder?.startRecording()
        running = true

        captureThread = Thread {
            val buffer = ShortArray(minBuffer)

            while (running) {
                val count = recorder?.read(buffer, 0, buffer.size) ?: 0

                if (count > 0) {
                    var sum = 0.0

                    for (i in 0 until count) {
                        val value = buffer[i].toDouble()
                        sum += value * value
                    }

                    val rms = sqrt(sum / count)
                    val level = (rms / 32768.0 * 100)
                        .toInt()
                        .coerceIn(0, 100)

                    val update = Intent(
                        MusicWidgetProvider.ACTION_LEVEL
                    ).apply {
                        setPackage(packageName)
                        putExtra(
                            MusicWidgetProvider.EXTRA_LEVEL,
                            level
                        )
                    }

                    sendBroadcast(update)

                    try {
                        Thread.sleep(250)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }.also {
            it.start()
        }
    }

    private fun stopCapture() {
        running = false

        try {
            recorder?.stop()
        } catch (_: Exception) {
        }

        recorder?.release()
        recorder = null

        captureThread?.interrupt()
        captureThread = null

        projection?.stop()
        projection = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio visualizer",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("nothing(R) Music")
            .setContentText("Audio capture is active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
