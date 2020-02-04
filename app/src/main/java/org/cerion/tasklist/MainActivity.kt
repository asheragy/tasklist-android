package org.cerion.tasklist

import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import org.cerion.tasklist.database.Prefs
import org.cerion.tasklist.database.Theme
import org.cerion.tasklist.ui.TaskListFragmentDirections


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode( when(Prefs.getInstance(this).theme) {
            Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        })

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val navView = findViewById<NavigationView>(R.id.navView)

        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
        NavigationUI.setupWithNavController(navView, navController)

        navController.addOnDestinationChangedListener { nc: NavController, nd: NavDestination, _: Bundle? ->
            if (nd.id == nc.graph.startDestination) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        }

        navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.select_theme -> onSelectTheme()
            R.id.settingsFragment -> navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToSettingsFragment())
            R.id.view_log -> navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToLogViewFragment())
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        hideKeyboard()
        val navController = this.findNavController(R.id.nav_host_fragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    override fun onBackPressed() {
        if(findNavController(R.id.nav_host_fragment).currentDestination?.id == R.id.taskListFragment)
            finishAndRemoveTask()
        else
            super.onBackPressed()
    }

    private fun hideKeyboard() {
        val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        // check if no view has focus:
        val currentFocusedView = currentFocus
        if (currentFocusedView != null) {
            inputManager.hideSoftInputFromWindow(currentFocusedView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    private fun onSelectTheme() {
        val values = resources.getStringArray(R.array.pref_background_entries)
        val prefs = Prefs.getInstance(this)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select theme")
        builder.setNegativeButton("Cancel", null)
        builder.setSingleChoiceItems(values, prefs.theme.ordinal) { _, which ->
            if (which != prefs.theme.ordinal) {
                prefs.theme = Theme.values()[which]
                TaskStackBuilder.create(this)
                        .addNextIntent(Intent(this, MainActivity::class.java))
                        .addNextIntent(this.intent)
                        .startActivities()
            }
        }

        val dialog = builder.create()
        dialog.show()
    }
}
