package org.cerion.tasklist.ui;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Prefs;

//TODO verify network is available and toast message

public class MainActivity extends FragmentActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Prefs.getInstance(this).isDarkTheme())
            setTheme(R.style.AppTheme_Dark);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, new TaskListFragment())
                .commit();

    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
