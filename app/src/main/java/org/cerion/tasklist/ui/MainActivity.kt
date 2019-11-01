package org.cerion.tasklist.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import org.cerion.tasklist.R
import org.cerion.tasklist.data.Prefs


class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Prefs.getInstance(this).isDarkTheme)
            setTheme(R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navController = this.findNavController(R.id.nav_host_fragment)
        val navView = findViewById<NavigationView>(R.id.navView)

        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
        NavigationUI.setupWithNavController(navView, navController)

        navController.addOnDestinationChangedListener { nc: NavController, nd: NavDestination, args: Bundle? ->
            if (nd.id == nc.graph.startDestination) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    override fun onBackPressed() {
        if(findNavController(R.id.nav_host_fragment).currentDestination?.id == R.id.taskListFragment)
            finishAndRemoveTask()
        else
            super.onBackPressed()
    }

}
