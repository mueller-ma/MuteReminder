package com.github.muellerma.mute_reminder

import android.app.usage.UsageStatsManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.muellerma.mute_reminder.databinding.ActivityPreferenceBinding
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.LibsConfiguration
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.SpecialButton


class PreferenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add(binding.activityContent.id, MainSettingsFragment())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class MainSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_main)

            getPreference("about").setOnPreferenceClickListener {
                val fragment = LibsBuilder()
                    .withAboutIconShown(true)
                    .withAboutVersionShownName(true)
                    .withSortEnabled(true)
                    .withListener(AboutButtonsListener())
                    .supportFragment()

                parentFragmentManager.commit {
                    addToBackStack("about")
                    val prefActivity = requireActivity() as PreferenceActivity
                    replace(prefActivity.binding.activityContent.id, fragment, "about")
                }
                true
            }

            var debugInfo = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val isIgnoringBatteryOptimizations = requireContext()
                    .getSystemService<PowerManager>()!!
                    .isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
                debugInfo += "isIgnoringBatteryOptimizations = $isIgnoringBatteryOptimizations\n"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val appStandbyBucket = requireContext()
                    .getSystemService<UsageStatsManager>()!!
                    .appStandbyBucket
                debugInfo += "appStandbyBucket = $appStandbyBucket\n"
            }
            getPreference("debug").summary = debugInfo
            if (debugInfo.isEmpty()) {
                preferenceScreen.removePreferenceRecursively("debug")
            }
        }
    }
}

class AboutButtonsListener : LibsConfiguration.LibsListener {
    override fun onExtraClicked(v: View, specialButton: SpecialButton): Boolean {
        val link = when (specialButton) {
            SpecialButton.SPECIAL1 -> "https://github.com/mueller-ma/MuteReminder"
            SpecialButton.SPECIAL2 -> "https://f-droid.org/de/packages/com.github.muellerma.mute_reminder/"
            SpecialButton.SPECIAL3 -> "https://crowdin.com/project/mutereminder"
        }
        link.openInBrowser(v.context)
        return true
    }

    override fun onIconClicked(v: View) {
        // no-op
    }

    override fun onIconLongClicked(v: View): Boolean {
        return false
    }

    override fun onLibraryAuthorClicked(v: View, library: Library): Boolean {
        return false
    }

    override fun onLibraryAuthorLongClicked(v: View, library: Library): Boolean {
        return false
    }

    override fun onLibraryBottomClicked(v: View, library: Library): Boolean {
        return false
    }

    override fun onLibraryBottomLongClicked(v: View, library: Library): Boolean {
        return false
    }

    override fun onLibraryContentClicked(v: View, library: Library): Boolean {
        return false
    }

    override fun onLibraryContentLongClicked(v: View, library: Library): Boolean {
        return false
    }
}

fun PreferenceFragmentCompat.getPreference(key: String) =
    preferenceManager.findPreference<Preference>(key)!!
