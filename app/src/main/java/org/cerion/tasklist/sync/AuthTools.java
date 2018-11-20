package org.cerion.tasklist.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.cerion.tasklist.data.AppDatabase;
import org.cerion.tasklist.data.Prefs;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskDao;
import org.cerion.tasklist.data.TaskList;
import org.cerion.tasklist.data.TaskListDao;

import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;

public class AuthTools {

    private static final String TAG = AuthTools.class.getSimpleName();
    private static final String AUTH_TOKEN_TYPE = "Manage your tasks"; //human readable version
    //private static final String AUTH_TOKEN_TYPE = "https://www.googleapis.com/auth/tasks";

    protected static String getSavedToken(Context context) {

        //When using emulator bypass usual auth methods so it doesn't need a google play account
        Log.d(TAG, Build.FINGERPRINT + "\t" + Build.PRODUCT);
        if(Build.FINGERPRINT.contains("vbox") || Build.PRODUCT.contentEquals("Genymotion")) {
            return "ya29.ZAJDI9hiP1QTGutoj9eGs4-SKkkg-wh_ZFGsxRUy7Gz5SV4XB5hu6tvGZVfPAnqqq8BbqYo";
        }

        //If we have a valid key use it instead of getting a new one
        Prefs prefs = Prefs.getInstance(context);
        String token = prefs.getString(Prefs.KEY_AUTHTOKEN);
        Date dtLastToken = prefs.getDate(Prefs.KEY_AUTHTOKEN_DATE);
        long dtDiff = (System.currentTimeMillis() - dtLastToken.getTime()) / 1000;
        if(token.length() > 0 && dtDiff < 3500) {
            //Token is a little less than 1 hour old so its still good
            Log.d(TAG,"Using existing token, remaining minutes: " + (3600 - dtDiff) / 60);
            return token;
        }

        return null;
    }

    protected static void clearSavedToken(Context context) {
        Prefs.getInstance(context)
                .remove(Prefs.KEY_AUTHTOKEN)
                .remove(Prefs.KEY_AUTHTOKEN_DATE);
    }

    protected static void getTokenAndSync(final Context context, @Nullable Activity activity, final OnSyncCompleteListener callback)
    {
        // TODO handle permission on first time use with new emulator
        Log.d(TAG, "Getting Token");
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        String accountName = Prefs.getInstance(context).getString(Prefs.KEY_ACCOUNT_NAME);
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

                        Prefs.getInstance(context)
                                .setString(Prefs.KEY_AUTHTOKEN, token)
                                .setDate(Prefs.KEY_AUTHTOKEN_DATE,new Date());

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

    //TODO, this is really more of an "unsync" function, maybe move to Sync class
    //Also this should be an async task that can just be ran with callback
    public static void logout(Context context) {
        Log.d(TAG, "onLogout");

        AppDatabase db = AppDatabase.Companion.getInstance(context);
        TaskDao taskDb = db.taskDao();
        TaskListDao listDb = db.taskListDao();

        //Move un-synced task to this default list
        List<TaskList> lists = listDb.getAll();
        TaskList defaultList = TaskList.Companion.getDefault(lists);

        //Delete all non-temp Id records, also remove records marked as deleted
        List<Task> tasks = taskDb.getAll();
        for (Task task : tasks) {
            if (!task.getHasTempId() || task.getDeleted())
                taskDb.delete(task);
            else {
                //Since we are also removing synced lists, check if we need to move this task to an un-synced list
                TaskList list = new TaskList(task.getId(), "");
                if (!list.getHasTempId() && defaultList != null) {
                    //Move this task to default list
                    taskDb.delete(task);
                    task.setListId(defaultList.getId());
                    taskDb.add(task);
                }
            }
        }

        for (TaskList list : lists) {
            if (!list.getHasTempId()) //don't delete un-synced lists
            {
                if (list.isDefault()) { //Keep default but assign temp id
                    listDb.setLastUpdated(list.getId(), new Date(0));
                    listDb.updateId(list.getId(), AppDatabase.Companion.generateTempId());
                } else
                    listDb.delete(list);
            }
        }

        Prefs prefs = Prefs.getInstance(context);
        //Remove prefs related to sync/account
        prefs.remove(Prefs.KEY_LAST_SYNC);
        prefs.remove(Prefs.KEY_ACCOUNT_NAME);
        prefs.remove(Prefs.KEY_AUTHTOKEN);
        prefs.remove(Prefs.KEY_AUTHTOKEN_DATE);

        //Log data which should be empty except for un-synced records
        //db.log();
        prefs.log();
    }

}
