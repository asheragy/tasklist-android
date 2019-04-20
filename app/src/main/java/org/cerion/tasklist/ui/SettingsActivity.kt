package org.cerion.tasklist.ui

import android.os.Bundle
import android.preference.PreferenceActivity

import org.cerion.tasklist.R
import org.cerion.tasklist.data.Prefs

class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle) {
        if (Prefs.getInstance(this).isDarkTheme)
            setTheme(R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)

        this.fragmentManager
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
    }
}
