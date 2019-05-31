package org.cerion.tasklist.ui

import android.os.Bundle

import androidx.fragment.app.FragmentActivity

import org.cerion.tasklist.R
import org.cerion.tasklist.data.Prefs

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Prefs.getInstance(this).isDarkTheme)
            setTheme(R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, TaskListFragment())
                .commit()
    }

    override fun onBackPressed() {
        finish()
    }
}
