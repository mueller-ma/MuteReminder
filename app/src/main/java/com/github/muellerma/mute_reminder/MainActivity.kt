package com.github.muellerma.mute_reminder

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.isGone
import com.github.muellerma.mute_reminder.databinding.ActivityMainBinding
import com.github.muellerma.mute_reminder.databinding.BottomSheetPermissionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.intellij.lang.annotations.Language
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var mediaAudioManager: MediaAudioManager
    private lateinit var binding: ActivityMainBinding
    private lateinit var locale: Locale
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

            var languagelist = ArrayList<String>()

            languagelist.add("Select")
            languagelist.add("English")
            languagelist.add("Hindi")
            languagelist.add("French")
            languagelist.add("Japanese")
            languagelist.add("Chinese")
            languagelist.add("Polish")
            languagelist.add("Italian")
            languagelist.add("Russian")
            languagelist.add("Portugese")
            languagelist.add("Norwegian")
            languagelist.add("Russian")
            languagelist.add("Czech")
            languagelist.add("Arabic")
            languagelist.add("Dutch")

            var adapter =
                ArrayAdapter(this, androidx.transition.R.layout.support_simple_spinner_dropdown_item, languagelist)
            binding.spinnerLanguage.adapter = adapter

            binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    when (p2) {
                        0 -> {

                        }
                        1 -> setLocale("en")
                        2 -> setLocale("hi")
                        3 -> setLocale("fr")
                        4 -> setLocale("ja")
                        5 -> setLocale("zh-rCN")
                        6 -> setLocale("cs")
                        7 -> setLocale("cs")
                        8 -> setLocale("pl")
                        9 -> setLocale("it")
                        10 -> setLocale("ru")
                        11 -> setLocale("pt-rBR")
                        12 -> setLocale("nb")
                        13 -> setLocale("ar")
                        14 -> setLocale("nl")
                        else -> setLocale("en")
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                    Toast.makeText(this@MainActivity, "Please select a language ", Toast.LENGTH_SHORT).show()
                }

            }


        }


    }

    private fun setLocale(languageName: String) {
        locale = Locale(languageName)
        var res = resources
        var dm = res.displayMetrics
        var conf = res.configuration
        conf.locale = locale
        res.updateConfiguration(conf, dm)

        var refresh = Intent(this@MainActivity, MainActivity::class.java)
        startActivity(refresh)
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