package com.github.muellerma.mute_reminder


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService


class ForegroundService : Service() {
    private lateinit var mediaAudioManager: MediaAudioManager
    private lateinit var prefs: Prefs
    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun deliverSelfNotifications() = false

        override fun onChange(selfChange: Boolean) {
            Log.d(TAG, "volumeObserver onChange()")
            handleVolumeChanged()
        }
    }
    private val muteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "muteListener onReceive(intent=${intent.action})")

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
    private val callListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : TelephonyCallback(), TelephonyCallback.CallStateListener
        {
            override fun onCallStateChanged(state: Int) {
                handleVolumeChanged()
            }
        }
    } else {
        null
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Prefs.NOTIFY_ONLY_WHEN_MUTED || key == Prefs.AUTO_MUTE) handleVolumeChanged()
    }

    private fun handleVolumeChanged() {
        Log.d(TAG, "handleVolumeChanged()")
        val nm = getSystemService<NotificationManager>()!!
        val shouldNotify = mediaAudioManager.shouldNotify()
        when {
            shouldNotify && isNotificationShown(nm) -> {
                Log.d(TAG, "Should notify, notification is already shown")
            }
            shouldNotify -> {

                if (prefs.autoMute) {
                    Log.d(TAG, "Should notify, auto mute enabled, muting media")
                    mediaAudioManager.muteMedia()
                } else {
                    Log.d(TAG, "Should notify, show notification")
                    nm.notify(NOTIFICATION_ALERT_ID, getNotification())
                }
            }
            else -> {
                Log.d(TAG, "Should not notify, hide notification")
                nm.cancel(NOTIFICATION_ALERT_ID)
            }
        }
    }

    private fun getNotification(): Notification {
        val intent = Intent(this, ForegroundService::class.java)
        intent.action = ACTION_MUTE_MEDIA
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent_Immutable
        )
        val muteAction = NotificationCompat.Action(
            R.drawable.ic_baseline_volume_mute_24,
            getString(R.string.mute_media),
            pendingIntent
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ALERT_ID)
            .setContentTitle(getString(R.string.notification_reminder_text))
            .setTicker(getString(R.string.notification_reminder_text))
            .setSmallIcon(R.drawable.ic_baseline_volume_up_24)
            .setOngoing(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setColor(ContextCompat.getColor(applicationContext, R.color.md_theme_light_primary))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(muteAction)
            .build()
    }

    private fun isNotificationShown(nm: NotificationManager): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        return nm.activeNotifications.any { it.id == NOTIFICATION_ALERT_ID }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        if (intent?.action == ACTION_MUTE_MEDIA) {
            //already running
            mediaAudioManager.muteMedia()
            return START_STICKY
        }
        mediaAudioManager = MediaAudioManager(this)
        prefs = Prefs(this)
        // Register for volume changes
        applicationContext.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            applicationContext.hasPermission(android.Manifest.permission.READ_PHONE_STATE)
        ) {
            Log.d(TAG, "Start TelephonyManager listener")
            val telephonyManager = getSystemService<TelephonyManager>()
            callListener?.let { telephonyManager?.registerTelephonyCallback(mainExecutor, it) }
        }

        val intentFilter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(AudioManager.ACTION_HDMI_AUDIO_PLUG)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addAction(AudioManager.ACTION_SPEAKERPHONE_STATE_CHANGED)
            }
        }
        // Register for DND and bluetooth changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(muteListener, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(muteListener, intentFilter)
        }
        prefs.sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
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
            lightColor = ContextCompat.getColor(this@ForegroundService, R.color.md_theme_light_primary)
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
        prefs.sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        unregisterReceiver(muteListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val telephonyManager = getSystemService<TelephonyManager>()
            callListener?.let { telephonyManager?.unregisterTelephonyCallback(it) }
        }
    }

    companion object {
        private val TAG = ForegroundService::class.java.simpleName
        private const val NOTIFICATION_SERVICE_ID = 1
        private const val NOTIFICATION_ALERT_ID = 2
        private const val NOTIFICATION_CHANNEL_SERVICE_ID = "service"
        private const val NOTIFICATION_CHANNEL_ALERT_ID = "alert"

        private const val ACTION_MUTE_MEDIA = "ACTION_MUTE_MEDIA"

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
