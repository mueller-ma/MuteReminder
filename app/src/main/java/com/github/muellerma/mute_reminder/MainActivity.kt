package com.github.muellerma.mute_reminder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.github.muellerma.mute_reminder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mediaAudioManager: MediaAudioManager
    private lateinit var binding: ActivityMainBinding
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::handleNotificationPermission
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        mediaAudioManager = MediaAudioManager(this)
        setupOnClickListeners()
        checkNotificationPermissionState()
    }

    override fun onResume() {
        super.onResume()
        handleNotificationPermission(hasNotificationPermission())
    }

    private fun setupOnClickListeners() = with(binding) {
        settings.setOnClickListener {
            Intent(this@MainActivity, PreferenceActivity::class.java).apply {
                startActivity(this)
            }
        }
        notificationPermissions.setOnClickListener { openNotificationSettings() }
        muteMedia.setOnClickListener {
            mediaAudioManager.muteMedia()
        }
    }

    private fun checkNotificationPermissionState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            handleNotificationPermission(true)
            return
        }
        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun handleNotificationPermission(isGranted: Boolean) {
        binding.notificationPermissions.isVisible = !isGranted
        if (isGranted) {
            ForegroundService.changeState(this, true)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return hasPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        startActivity(settingsIntent)
    }
}