package org.cerion.tasklist.ui


import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.cerion.tasklist.R
import org.cerion.tasklist.data.Prefs

class SettingsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Prefs.getInstance(this).isDarkTheme)
            setTheme(R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)

        if (savedInstanceState != null)
            return

        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }
}
