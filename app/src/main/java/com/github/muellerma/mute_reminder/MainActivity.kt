package com.github.muellerma.mute_reminder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.muellerma.mute_reminder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.github.setOnClickListener {
            "https://github.com/mueller-ma/MuteReminder".openInBrowser(this)
        }

        binding.version.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        ForegroundService.changeState(this, true)
    }
}