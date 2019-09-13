package org.cerion.tasklist.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import org.cerion.tasklist.data.AppDatabase
import org.cerion.tasklist.data.Prefs
import org.cerion.tasklist.data.TaskList
import java.util.*

object AuthTools {

    private val TAG = AuthTools::class.java.simpleName
    private val AUTH_TOKEN_TYPE = "Manage your tasks" //human readable version
    //private static final String AUTH_TOKEN_TYPE = "https://www.googleapis.com/auth/tasks";

    interface AuthTokenCallback {
        fun onSuccess(token: String)
        fun onError(e: Exception?)
    }

    fun getAuthToken(context: Context, activity: Activity?, callback: AuthTokenCallback) {
        val token = getSavedToken(context)

        if (token != null)
            callback.onSuccess(token)
        else
            getNewToken(context, activity, callback)
    }

    private fun getSavedToken(context: Context): String? {

        //If we have a valid key use it instead of getting a new one
        val prefs = Prefs.getInstance(context)
        val token = prefs.getString(Prefs.KEY_AUTHTOKEN)
        val dtLastToken = prefs.getDate(Prefs.KEY_AUTHTOKEN_DATE)
        val dtDiff = (System.currentTimeMillis() - dtLastToken.time) / 1000
        if (token.length > 0 && dtDiff < 3500) {
            //Token is a little less than 1 hour old so its still good
            Log.d(TAG, "Using existing token, remaining minutes: " + (3600 - dtDiff) / 60)
            return token
        }

        return null
    }

    internal fun clearSavedToken(context: Context) {
        Prefs.getInstance(context)
                .remove(Prefs.KEY_AUTHTOKEN)
                .remove(Prefs.KEY_AUTHTOKEN_DATE)
    }

    private fun getNewToken(context: Context, activity: Activity?, callback: AuthTokenCallback) {
        // TODO handle permission on first time use with new emulator
        Log.d(TAG, "Getting Token")
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType("com.google")
        val accountName = Prefs.getInstance(context).getString(Prefs.KEY_ACCOUNT_NAME)
        var account: Account? = null

        for (tmpAccount in accounts) {
            if (tmpAccount.name!!.contentEquals(accountName))
                account = tmpAccount
        }

        if (account != null) {

            //What to run after getting a key
            val accountManagerCallback = AccountManagerCallback<Bundle> {
                try {
                    // If the user has authorized your application to use the tasks API a token is available.
                    val bundle = it.result
                    val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)

                    Prefs.getInstance(context)
                            .setString(Prefs.KEY_AUTHTOKEN, token)
                            .setDate(Prefs.KEY_AUTHTOKEN_DATE, Date())

                    callback.onSuccess(token!!)
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }

            //If these fail it will need to prompt for permissions
            if (activity != null)
            //Show permissions dialog (requires activity to start from)
                accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, null, activity, accountManagerCallback, null)
            else
            //Show permissions as notification (when no activity is available)
                accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, null, true, accountManagerCallback, null)

        } else
            callback.onError(null)

        Log.d(TAG, "Getting Token END")
    }

    //TODO, this is really more of an "unsync" function, maybe move to Sync class
    //Also this should be an async task that can just be ran with callback
    fun logout(context: Context) {
        Log.d(TAG, "onLogout")

        val db = AppDatabase.getInstance(context)
        val taskDb = db!!.taskDao()
        val listDb = db.taskListDao()

        //Move un-synced task to this default list
        val lists = listDb.getAll()
        val defaultList = TaskList.getDefault(lists)

        //Delete all non-temp Id records, also remove records marked as deleted
        val tasks = taskDb.getAll()
        for (task in tasks) {
            if (!task.hasTempId || task.deleted)
                taskDb.delete(task)
            else {
                //Since we are also removing synced lists, check if we need to move this task to an un-synced list
                val list = TaskList(task.id, "")
                if (!list.hasTempId && defaultList != null) {
                    //Move this task to default list
                    taskDb.delete(task)
                    task.listId = defaultList.id
                    taskDb.add(task)
                }
            }
        }

        for (list in lists) {
            if (!list.hasTempId)
            //don't delete un-synced lists
            {
                if (list.isDefault) { //Keep default but assign temp id
                    listDb.setLastUpdated(list.id, Date(0))
                    listDb.updateId(list.id, AppDatabase.generateTempId())
                } else
                    listDb.delete(list)
            }
        }

        val prefs = Prefs.getInstance(context)
        //Remove prefs related to sync/account
        prefs.remove(Prefs.KEY_LAST_SYNC)
        prefs.remove(Prefs.KEY_ACCOUNT_NAME)
        prefs.remove(Prefs.KEY_AUTHTOKEN)
        prefs.remove(Prefs.KEY_AUTHTOKEN_DATE)

        //Log data which should be empty except for un-synced records
        db.log()
        prefs.log()
    }

}
