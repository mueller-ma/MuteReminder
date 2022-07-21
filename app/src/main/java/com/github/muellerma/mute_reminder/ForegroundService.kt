package com.github.muellerma.mute_reminder


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService


class ForegroundService : Service() {
    private lateinit var audioManager: AudioManager
    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun deliverSelfNotifications() = false

        override fun onChange(selfChange: Boolean) {
            Log.d(TAG, "volumeObserver onChange()")
            handleVolumeChanged()
        }
    }
    private val muteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "muteListener onReceive()")

            val bluetoothActions = listOf(
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED
            )
            if (intent.action in bluetoothActions) {
                // audioManager.getDevices() may still return the bluetooth device
                // when ACTION_ACL_DISCONNECTED is received here
                Thread.sleep(1_000)
            }
            handleVolumeChanged()
        }
    }

    private fun handleVolumeChanged() {
        val ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.d(TAG, "handleVolumeChanged(): ring: $ringVolume, media: $mediaVolume")

        val hasBluetoothOutput = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bluetoothOutputs = audioManager
                .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { device -> device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }

            Log.d(TAG, "Connected to ${bluetoothOutputs.joinToString(", ") { it.productName }}")
            bluetoothOutputs.isNotEmpty()
        } else {
            false
        }

        val nm = getSystemService<NotificationManager>()!!

        val mediaNotMutedButOnBluetooth = mediaVolume > 0 && hasBluetoothOutput
        if (ringVolume > 0 || mediaVolume == 0 || mediaNotMutedButOnBluetooth) {
            Log.d(TAG, "Hide notification")
            nm.cancel(NOTIFICATION_ALERT_ID)
        } else {
            Log.d(TAG, "Show notification")

            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ALERT_ID)
                .setContentTitle(getString(R.string.notification_reminder_text))
                .setTicker(getString(R.string.notification_reminder_text))
                .setSmallIcon(R.drawable.ic_baseline_volume_up_24)
                .setOngoing(false)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .setColor(ContextCompat.getColor(applicationContext, R.color.purple_700))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)

            nm.notify(NOTIFICATION_ALERT_ID, notificationBuilder.build())
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        audioManager = getSystemService()!!
        // Register for volume changes
        applicationContext.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )

        val intentFilter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            }
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        }
        // Register for DND and bluetooth changes
        registerReceiver(muteListener, intentFilter)

        handleVolumeChanged()

        return START_STICKY
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val nm = getSystemService<NotificationManager>()!!

        with(
            NotificationChannel(
                NOTIFICATION_CHANNEL_SERVICE_ID,
                getString(R.string.notification_background_title),
                NotificationManager.IMPORTANCE_MIN
            )
        ) {
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
            setSound(null, null)
            description = getString(R.string.notification_background_summary)
            nm.createNotificationChannel(this)
        }

        with(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ALERT_ID,
                getString(R.string.notification_reminder_title),
                NotificationManager.IMPORTANCE_HIGH
            )
        ) {
            setShowBadge(true)
            enableVibration(false)
            enableLights(true)
            lightColor = Color.RED
            setSound(null, null)
            nm.createNotificationChannel(this)
        }
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        super.onCreate()

        createNotificationChannels()

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_SERVICE_ID)
            .setContentTitle(getString(R.string.notification_background_title))
            .setTicker(getString(R.string.notification_background_title))
            .setContentText(getString(R.string.notification_background_summary))
            .setSmallIcon(R.drawable.ic_baseline_volume_mute_24)
            .setOngoing(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        startForeground(NOTIFICATION_SERVICE_ID, notificationBuilder.build())
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
        applicationContext.contentResolver.unregisterContentObserver(volumeObserver)
        unregisterReceiver(muteListener)
    }

    companion object {
        private val TAG = ForegroundService::class.java.simpleName
        private const val NOTIFICATION_SERVICE_ID = 1
        private const val NOTIFICATION_ALERT_ID = 2
        private const val NOTIFICATION_CHANNEL_SERVICE_ID = "service"
        private const val NOTIFICATION_CHANNEL_ALERT_ID = "alert"

        fun changeState(context: Context, start: Boolean) {
            Log.d(TAG, "changeState($start)")
            val intent = Intent(context, ForegroundService::class.java)
            if (start) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.stopService(intent)
            }
        }
    }
}