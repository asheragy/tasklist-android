package org.cerion.tasklist.ui.settings


import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.common.AccountPicker
import org.cerion.tasklist.R
import org.cerion.tasklist.data.Prefs
import org.cerion.tasklist.sync.AuthTools
import org.cerion.tasklist.ui.MainActivity

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var mAccountList: Preference
    private lateinit var mLogout: Preference
    private lateinit var mBackground: ListPreference
    private lateinit var mPrefs: Prefs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mPrefs = Prefs.getInstance(activity)
        mLogout = findPreference("logout")
        mAccountList = findPreference(getString(R.string.pref_key_accountName))
        mBackground = findPreference(getString(R.string.pref_key_background)) as ListPreference

        init()
    }

    private fun init() {
        //Accounts
        val currentAccount = mPrefs.getString(mAccountList.key)
        mAccountList.summary = currentAccount
        mAccountList.setOnPreferenceClickListener {
            onChooseAccount()
            true
        }

        //Logout button
        val acct = mPrefs.getString(mAccountList.key)
        mLogout.isEnabled = acct != null && acct.isNotEmpty()
        mLogout.setOnPreferenceClickListener {
            //TODO, progress indicator and async
            AuthTools.logout(activity)

            //Restart app
            TaskStackBuilder.create(activity)
                    .addNextIntent(Intent(activity, MainActivity::class.java))
                    .startActivities()
            true
        }

        mBackground.setOnPreferenceChangeListener { preference, o ->
            val curr = (preference as ListPreference).value

            //If setting was changed
            if (!curr.contentEquals(o as String)) {
                //Intent intent = getIntent();
                //finish();
                //startActivity(intent);

                //Recreate main activity followed by this one
                TaskStackBuilder.create(activity)
                        .addNextIntent(Intent(activity, MainActivity::class.java))
                        .addNextIntent(requireActivity().intent)
                        .startActivities()
            }

            true
        }

        findPreference("viewlog").setOnPreferenceClickListener {
            val intent = Intent(activity, LogViewActivity::class.java)
            startActivity(intent)
            true
        }
    }

    private fun onChooseAccount() {
        if (!checkAndVerifyPermission())
            return

        //Find current account
        val accountManager = AccountManager.get(activity)
        val accounts = accountManager.getAccountsByType("com.google")
        val accountName = mPrefs.getString(Prefs.KEY_ACCOUNT_NAME)
        var account: Account? = null
        for (tmpAccount in accounts) {
            if (tmpAccount.name!!.contentEquals(accountName))
                account = tmpAccount
        }

        //Display account picker
        try {
            val intent = AccountPicker.newChooseAccountIntent(
                    account, null, arrayOf("com.google"),
                    true, null, null, null, null)//always prompt, if false and there is 1 account it will be auto selected without a prompt which may be confusing
            startActivityForResult(intent, PICK_ACCOUNT_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(activity, "Google play services not available", Toast.LENGTH_LONG).show()
        }

    }

    private fun checkAndVerifyPermission(): Boolean {

        val permission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.GET_ACCOUNTS)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            // TODO add this
            if (false)
            //ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.GET_ACCOUNTS))
            {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(requireActivity(),
                        arrayOf(Manifest.permission.GET_ACCOUNTS),
                        PERMISSIONS_REQUEST_GET_ACCOUNTS)
            }

            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_GET_ACCOUNTS -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onChooseAccount()
                } else {
                    Toast.makeText(activity, "Account permission needed to sync", Toast.LENGTH_LONG).show()
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $resultCode")

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_ACCOUNT_REQUEST && data != null) {
                val currentAccount = mPrefs.getString(Prefs.KEY_ACCOUNT_NAME)
                val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)

                //If current account is set and different than selected account, logout first
                if (!currentAccount.isNullOrEmpty() && !currentAccount.contentEquals(accountName))
                    AuthTools.logout(activity)

                mAccountList.summary = accountName
                mPrefs.setString(mAccountList.key, accountName)
                mLogout.isEnabled = true
            }
        }
    }

    companion object {
        private val TAG = SettingsFragment::class.java.simpleName
        private const val PICK_ACCOUNT_REQUEST = 0
        private const val PERMISSIONS_REQUEST_GET_ACCOUNTS = 1
    }
}
