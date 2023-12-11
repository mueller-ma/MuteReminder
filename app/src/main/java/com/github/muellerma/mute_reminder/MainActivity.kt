package com.github.muellerma.mute_reminder

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.isGone
import com.github.muellerma.mute_reminder.databinding.ActivityMainBinding
import com.github.muellerma.mute_reminder.databinding.BottomSheetPermissionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {
    private lateinit var mediaAudioManager: MediaAudioManager
    private lateinit var binding: ActivityMainBinding
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        Log.d(TAG, "permissionLauncherCallback")
        ForegroundService.changeState(this, true)
        updatePermissionButton()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        mediaAudioManager = MediaAudioManager(this)
        setupOnClickListeners()
        showPermissionsDialogIfRequired()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManagerCompat.addDynamicShortcuts(this, listOf(getShortcutInfo()))
        }
        ForegroundService.changeState(this, true)
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        updatePermissionButton()
    }

    private fun updatePermissionButton() {
        Log.d(TAG, "checkPermissions()")
        binding.permissions.isGone = getMissingPermissions().isEmpty()
    }

    private fun getMissingPermissions(): Array<String> {
        val requiredPermissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return requiredPermissions
            .filter { !hasPermission(it) }
            .toTypedArray()
    }

    private fun showPermissionsDialogIfRequired() {
        Log.d(TAG, "showPermissionsDialogIfRequired()")

        if (getMissingPermissions().isEmpty()) {
            Log.d(TAG, "No permissions missing, don't show dialog")
            return
        }

        val dialog = BottomSheetDialog(this)
        val bottomSheet = BottomSheetPermissionsBinding.inflate(layoutInflater)
        bottomSheet.button.setOnClickListener {
            dialog.dismiss()
            val requestPermissions = getMissingPermissions()
            Log.d(TAG, "Request permissions: ${requestPermissions.joinToString()}")
            permissionLauncher.launch(requestPermissions)
        }
        dialog.setContentView(bottomSheet.root)
        dialog.show()
    }

    private fun setupOnClickListeners() = with(binding) {
        settings.setOnClickListener {
            Intent(this@MainActivity, PreferenceActivity::class.java).apply {
                startActivity(this)
            }
        }
        permissions.setOnClickListener {
            showPermissionsDialogIfRequired()
        }
        muteMedia.setOnClickListener {
            mediaAudioManager.muteMedia()
        }
    }

    private fun getShortcutInfo(): ShortcutInfoCompat {
        Log.d(TAG, "getShortcutInfo()")
        val intent = Intent(this, InvisibleActivity::class.java)
            .setAction(InvisibleActivity.ACTION_MUTE)
        return ShortcutInfoCompat.Builder(this, "mute")
            .setIntent(intent)
            .setShortLabel(getString(R.string.mute_media_short))
            .setLongLabel(getString(R.string.mute_media))
            .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_shortcut_mute))
            .setAlwaysBadged()
            .build()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}