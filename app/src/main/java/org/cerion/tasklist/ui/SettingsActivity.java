package org.cerion.tasklist.ui;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.TaskStackBuilder;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Prefs;
import org.cerion.tasklist.sync.AuthTools;

public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private Preference mAccountList;
    private Preference mLogout;
    private ListPreference mBackground;
    private Prefs mPrefs;
    private static final int PICK_ACCOUNT_REQUEST = 0;
    private static final int PERMISSIONS_REQUEST_GET_ACCOUNTS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Prefs.getInstance(this).isDarkTheme())
            setTheme(R.style.AppTheme_Dark);

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        mPrefs = Prefs.getInstance(this);

        mLogout = get("logout");
        mAccountList = get(getString(R.string.pref_key_accountName));
        mBackground = (ListPreference)get(getString(R.string.pref_key_background));

        init();
    }

    private void init() {
        //Accounts
        final String currentAccount = mPrefs.getString(mAccountList.getKey());
        mAccountList.setSummary(currentAccount);
        mAccountList.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                onChooseAccount();
                return true;
            }
        });

        //Logout button
        String acct = mPrefs.getString(mAccountList.getKey());
        mLogout.setEnabled(acct != null && acct.length() > 0);
        mLogout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //TODO, progress indicator and async
                AuthTools.logout(SettingsActivity.this);

                //Restart app
                TaskStackBuilder.create(SettingsActivity.this)
                        .addNextIntent(new Intent(SettingsActivity.this, MainActivity.class))
                        .startActivities();
                return true;
            }
        });

        mBackground.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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

        get("viewlog").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, LogViewActivity.class);
                startActivity(intent);
                return true;
            }
        });
    }

    //Temp to reduce warnings to 1 location
    private Preference get(CharSequence key) {
        return findPreference(key);
    }

    private boolean checkAndVerifyPermission() {

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS);
        if(permission != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            // TODO add this
            if (false)//ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.GET_ACCOUNTS))
            {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.GET_ACCOUNTS},
                        PERMISSIONS_REQUEST_GET_ACCOUNTS);
            }

            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_GET_ACCOUNTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onChooseAccount();
                } else {
                    Toast.makeText(this, "Account permission needed to sync", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    private void onChooseAccount() {
        if(!checkAndVerifyPermission())
            return;

        //Find current account
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        String accountName = mPrefs.getString(Prefs.KEY_ACCOUNT_NAME);
        Account account = null;
        for (Account tmpAccount : accounts) {
            if (tmpAccount.name.contentEquals(accountName))
                account = tmpAccount;
        }

        //Display account picker
        try {
            Intent intent = AccountPicker.newChooseAccountIntent(
                    account, null, new String[]{"com.google"},
                    true, //always prompt, if false and there is 1 account it will be auto selected without a prompt which may be confusing
                    null, null, null, null);
            startActivityForResult(intent, PICK_ACCOUNT_REQUEST);
        } catch(Exception e) {
            Toast.makeText(this, "Google play services not available", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + resultCode);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_ACCOUNT_REQUEST) {
                String currentAccount = mPrefs.getString(Prefs.KEY_ACCOUNT_NAME);
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                //If current account is set and different than selected account, logout first
                if (currentAccount.length() > 0 && !currentAccount.contentEquals(accountName))
                    AuthTools.logout(this);

                mAccountList.setSummary(accountName);
                mPrefs.setString(mAccountList.getKey(), accountName);
                mLogout.setEnabled(true);
            }
        }
    }
}
