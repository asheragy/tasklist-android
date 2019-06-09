package org.cerion.tasklist.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import org.cerion.tasklist.R
import org.cerion.tasklist.data.Prefs


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Prefs.getInstance(this).isDarkTheme)
            setTheme(R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
        if(findNavController(R.id.nav_host_fragment).currentDestination?.id == R.id.taskListFragment)
            finishAndRemoveTask()
        else
            super.onBackPressed()
    }

}
