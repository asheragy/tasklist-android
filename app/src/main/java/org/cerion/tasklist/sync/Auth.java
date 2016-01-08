package org.cerion.tasklist.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import org.cerion.tasklist.data.Prefs;

import java.util.Date;

class Auth {

    private static final String TAG = Auth.class.getSimpleName();
    private static final String AUTH_TOKEN_TYPE = "Manage your tasks"; //human readable version
    //private static final String AUTH_TOKEN_TYPE = "https://www.googleapis.com/auth/tasks";


    protected static String getSavedToken(Context context) {

        //When using emulator bypass usual auth methods so it doesn't need a google play account
        Log.d(TAG, Build.FINGERPRINT + "\t" + Build.PRODUCT);
        if(Build.PRODUCT.contains("vbox")) {
            return "ya29._AE1TRB2jdYkcClXxQwyUEdX_mxBwLDV7pOwLJzXoPtRkrCWQko5q-dPJoe0Ju4YYwwLWSc";
        }

        //If we have a valid key use it instead of getting a new one
        String token = Prefs.getPref(context, Prefs.KEY_AUTHTOKEN);
        Date dtLastToken = Prefs.getPrefDate(context, Prefs.KEY_AUTHTOKEN_DATE);
        long dtDiff = (System.currentTimeMillis() - dtLastToken.getTime()) / 1000;
        if(token.length() > 0 && dtDiff < 3500) {
            //Token is a little less than 1 hour old so its still good
            Log.d(TAG,"Using existing token, remaining minutes: " + (3600 - dtDiff) / 60);
            return token;
        }

        return null;
    }

    protected static void getTokenAndSync(final Context context, @Nullable Activity activity, final OnSyncCompleteListener callback)
    {
        Log.d(TAG, "Getting Token");
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        String accountName = Prefs.getPref(context, Prefs.KEY_ACCOUNT_NAME);
        Account account = null;

        for(Account tmpAccount: accounts) {
            if(tmpAccount.name.contentEquals(accountName))
                account = tmpAccount;
        }


        if(account != null) {

            //What to run after getting a key
            AccountManagerCallback<Bundle> accountManagerCallback = new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> future) {
                    try {
                        // If the user has authorized your application to use the tasks API a token is available.
                        Bundle bundle = future.getResult();
                        String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                        Prefs.savePref(context, Prefs.KEY_AUTHTOKEN, token);
                        Prefs.savePrefDate(context, Prefs.KEY_AUTHTOKEN_DATE,new Date());

                        Log.d(TAG,"Starting SyncTask");
                        SyncTask task = new SyncTask(context,token,callback);
                        task.execute();
                    }
                    catch (Exception e) {
                        callback.onAuthError(e);
                    }
                }
            };

            //If these fail it will need to prompt for permissions
            if(activity != null) //Show permissions dialog (requires activity to start from)
                accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, null, activity, accountManagerCallback, null);
            else //Show permissions as notification (when no activity is available)
                accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, null, true, accountManagerCallback, null);

        }
        else
            callback.onAuthError(null);

        Log.d(TAG, "Getting Token END");
    }

}
