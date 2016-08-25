package org.cerion.tasklist.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.app.TaskStackBuilder;
import android.os.Bundle;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Prefs;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Prefs.getInstance(this).isDarkTheme())
            setTheme(R.style.AppTheme_Dark);

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        //Set Account list
        final ListPreference accountList = (ListPreference) findPreference(getString(R.string.pref_key_accountName));
        initAccounts(accountList);

        final ListPreference background = (ListPreference)findPreference(getString(R.string.pref_key_background));
        initBackground(background);

        findPreference("viewlog").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, LogViewActivity.class);
                startActivity(intent);
                return false;
            }
        });
    }

    private void initAccounts(ListPreference accountList) {
        final String currentAccount = Prefs.getInstance(this).getString(Prefs.KEY_ACCOUNT_NAME);
        accountList.setSummary(currentAccount);

        //Get accounts from account manager
        AccountManager accountManager = AccountManager.get(SettingsActivity.this);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        CharSequence[] accts = new CharSequence[accounts.length];
        for(int i = 0; i < accounts.length; i++)
            accts[i] = accounts[i].name;

        accountList.setEntries(accts);
        accountList.setEntryValues(accts);
        accountList.setDefaultValue(currentAccount);

        accountList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                //TODO, if changed need to logout and log back in
                return false;
            }
        });
    }

    private void initBackground(ListPreference background) {
        background.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                String curr = ((ListPreference)preference).getValue();

                //If setting was changed
                if(!curr.contentEquals((String)o)) {
                    //Intent intent = getIntent();
                    //finish();
                    //startActivity(intent);

                    //Recreate main activity followed by this one
                    TaskStackBuilder.create(SettingsActivity.this)
                            .addNextIntent(new Intent(SettingsActivity.this, MainActivity.class))
                            .addNextIntent(getIntent())
                            .startActivities();
                }

                return true;
            }
        });
    }
}
