package org.cerion.tasklist.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Prefs;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Prefs.getInstance(this).isDarkTheme())
            setTheme(R.style.AppTheme_Dark);

        super.onCreate(savedInstanceState);

        this.getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
