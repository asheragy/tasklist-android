package org.cerion.tasklist.ui


import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_settings.*
import org.cerion.tasklist.R
import org.cerion.tasklist.data.Prefs

class SettingsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Prefs.getInstance(this).isDarkTheme)
            setTheme(R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState != null)
            return

        toolbar.setNavigationOnClickListener { onBackPressed() }

        supportFragmentManager.beginTransaction().replace(R.id.fragment, SettingsFragment()).commit()
    }
}
