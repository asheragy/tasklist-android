package org.cerion.tasklist.data;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Sync
{
    private static final String TAG = "Sync";
    private static final int SYNC_ADD_LIST = 0;
    private static final int SYNC_CHANGE_LIST = 1;
    private static final int SYNC_DELETE_LIST = 2;
    private static final int SYNC_ADD_TASK = 3;
    private static final int SYNC_CHANGE_TASK = 4;
    private static final int SYNC_DELETE_TASK = 5;

    private static final String AUTH_TOKEN_TYPE = "Manage your tasks"; //human readable version

    //Instance variables
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private TasksAPI mAPI = null;
    private Database mDb = null;
    private final int[] googleToDb = { 0, 0, 0, 0, 0, 0 }; //Add Change Delete Lists / Tasks
    private final int[] dbToGoogle = { 0, 0, 0, 0, 0, 0 };
    private Map<String,String> mSyncKeys = null;

    public interface Callback
    {
        void onAuthError(Exception e); //Unable to verify account or get sync token
        void onSyncFinish(boolean bSuccess, Exception e);
    }

    public static void syncTaskLists(Context context, Callback callback)
    {
        getTokenAndSync(context,callback);
    }

    private Sync(Context context, String sAuthKey)
    {
        mDb = Database.getInstance(context);
        mAPI = new TasksAPI(sAuthKey);
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mSyncKeys = mDb.getSyncKeys();
    }

    private boolean run() throws TasksAPI.TasksAPIException {
        ArrayList<TaskList> googleLists = mAPI.taskLists.getList();
        if(googleLists.size() == 0)
            return false;

        ArrayList<TaskList> dbLists = mDb.taskLists.getList();

        //Google->Local (Added or Updated)
        for(TaskList curr : googleLists) {
            TaskList dbList = TaskList.get(dbLists,curr.id);
            //--- UPDATE
            if(dbList != null) {
                if(!curr.title.contentEquals(dbList.title)) {
                    //Name mismatch, update only if local list was not renamed
                    if(!dbList.isRenamed()) {
                        mDb.taskLists.update(curr);
                        googleToDb[SYNC_CHANGE_LIST]++;
                    }
                }
            }
            //--- MERGE default, first sync only
            else if(curr.bDefault) {
                dbList = TaskList.getDefault(dbLists);
                if(dbList != null) {
                    if(!dbList.isRenamed()) {
                        dbList.title = curr.title;
                        mDb.taskLists.update(dbList);
                    }

                    mDb.setTaskListId(dbList,curr.id); //assign ID
                    dbList.id = curr.id;
                }
                else
                    Log.e(TAG,"missing default list");
            }
            //--- ADD
            else { //Does not exist locally, add it
                mDb.taskLists.add(curr);
                googleToDb[SYNC_ADD_LIST]++;

            }
        }

        //--- DELETE
        //Verify database list still exists on web, otherwise it was deleted
        for(TaskList curr : dbLists) {
            if(curr.hasTempId()) //Temp ids don't exist on web since they have not been created yet
                continue;

            TaskList googleList = TaskList.get(googleLists,curr.id);
            if(googleList == null) {
                mDb.taskLists.delete(curr);
                googleToDb[SYNC_DELETE_LIST]++;
            }

        }

        //If any changes made to local database, just reload it
        if(googleToDb[SYNC_DELETE_LIST] > 0 || googleToDb[SYNC_ADD_LIST] > 0)
            dbLists = mDb.taskLists.getList();

        //Local ----> Google
        for(TaskList dbList : dbLists) {
            //--- ADD
            if(dbList.hasTempId()) {
                TaskList addedList = mAPI.taskLists.add(dbList);
                if(addedList != null) {
                    mDb.setTaskListId(dbList, addedList.id);
                    dbList.id = addedList.id;
                    googleLists.add(addedList);
                    dbToGoogle[SYNC_ADD_LIST]++;
                }
                else {
                    Log.e(TAG, "Failed to add list");
                    return false;
                }
            }

            //--- UPDATE
            if(dbList.isRenamed())
            {
                TaskList googleList = TaskList.get(googleLists,dbList.id);

                if (googleList != null) {
                    if(mAPI.taskLists.update(dbList)) {
                        //Save state in db to indicate rename was successful
                        dbList.clearRenamed();
                        mDb.taskLists.update(dbList);
                        dbToGoogle[SYNC_CHANGE_LIST]++;
                    }
                    else
                        Log.d(TAG,"Failed to rename list");
                }
                else {
                    //This shouldn't be possible, if list does not exist it should have deleted database list in earlier code
                    Log.e(TAG,"Error: Failed to find list to update");
                    return false;
                }

            }

            //--- DELETE LATER, allow task lists to be deleted locally, remove entry from array if success so we don't loop below
        }

        //Loop web lists since it has updated time set which we need for comparisons
        for(TaskList list : googleLists)
            syncTasks(list);

        //mDb.print();

        Log.d(TAG, "Google to DB: Lists (" + googleToDb[0] + "," + googleToDb[1] + "," + googleToDb[2] + ") Tasks (" + googleToDb[3] + "," + googleToDb[4] + "," + googleToDb[5] + ")");
        Log.d(TAG, "DB to Google: Lists (" + dbToGoogle[0] + "," + dbToGoogle[1] + "," + dbToGoogle[2] + ") Tasks (" + dbToGoogle[3] + "," + dbToGoogle[4] + "," + dbToGoogle[5] + ")");

        return true;
    }


    private void syncTasks(TaskList list) throws TasksAPI.TasksAPIException {
        Date dtLastUpdated = null;
        ArrayList<Task> dbTasks;
        String listId = list.id;

        String key = "updated_" + listId;
        String lastUpdatedSaved = mSyncKeys.get(key);

        Log.d(TAG, "syncTasks() " + listId + "\t" + lastUpdatedSaved + "\t" + list.updated);

        try {
            if (lastUpdatedSaved != null)
                dtLastUpdated = mDateFormat.parse(lastUpdatedSaved);
        } catch (ParseException e) {
            Log.e(TAG,"exception",e);
        }

        ArrayList<Task> webTasks = null;
        if (dtLastUpdated == null)
        {
            Log.d(TAG, "New list, getting all");
            webTasks = mAPI.tasks.getList(listId, null);

        } else if (list.updated.after(dtLastUpdated)) {
            //The default list can get its modified time updated without having any new tasks, we'll get 0 tasks here sometimes but not much we can do about it
            Log.d(TAG, "Getting updated Tasks");
            Log.d(TAG, "Web   = " + list.updated);
            Log.d(TAG, "Saved = " + dtLastUpdated);
            dtLastUpdated.setTime(dtLastUpdated.getTime() + 1000); //Increase by 1 second to avoid getting previous updated record which already synced
            webTasks = mAPI.tasks.getList(listId, dtLastUpdated);
        }
        //else
        //    Log.d(TAG, "No changes");

        dbTasks = mDb.tasks.getList(listId);
        if (webTasks != null) {
            for (Task task : webTasks) {
                Task dbTask = getTask(dbTasks, task.id);
                if (dbTask != null) {
                    if (task.deleted) {
                        mDb.tasks.delete(task);
                        googleToDb[SYNC_DELETE_TASK]++;
                        dbTasks.remove(dbTask);
                    } else if(dbTask.deleted) {
                        Log.d(TAG,"Ignoring update since deleted on local"); //Local task is deleted and will be handled on next phase
                    }
                    else {
                        //Conflict
                        if(dbTask.updated.getTime() > task.updated.getTime())
                            Log.e(TAG,"Conflict: Local task was updated most recently");
                        else {
                            dbTasks.remove(dbTask);
                            mDb.tasks.update(task);
                            googleToDb[SYNC_CHANGE_TASK]++;
                        }

                    }
                } else if (task.deleted) {
                    Log.d(TAG,"Ignoring web delete since record was never added locally");
                } else {
                    mDb.tasks.add(task);
                    googleToDb[SYNC_ADD_TASK]++;
                }
            }
        }



        /**************
        Database -> Web
         **************/
        boolean bListUpdated = false;
        for (Task task : dbTasks)
        {
            //Log.d(TAG,"Title = " + task.title + "\t" + task.updated);
            boolean bModified = false;
            if(dtLastUpdated == null || task.updated.after(dtLastUpdated))
                bModified = true;

            if(task.deleted)
            {
                //Deleted tasks get processed then deleted from database
                //Modified time is not required but should be logged for potential bugs elsewhere
                if(!bModified)
                    Log.e(TAG,"ERROR, deleted task not modified");

                if(task.hasTempId()) //If never added just delete from local database
                {
                    mDb.tasks.delete(task);
                }
                else if (mAPI.tasks.delete(task))
                {
                    mDb.tasks.delete(task);
                    dbToGoogle[SYNC_DELETE_TASK]++;
                    bListUpdated = true;
                }

            }
            else if(task.hasTempId())
            {
                Task updated = mAPI.tasks.add(task);
                if(updated != null) {
                    mDb.setTaskIds(task,updated.id,updated.listId);
                    dbToGoogle[SYNC_ADD_TASK]++;
                    bListUpdated = true;

                    if(listId == null) //When adding a new task without a list this will be the default task list id
                        listId = updated.listId;

                }
            }
            else if(bModified)
            {
                if(mAPI.tasks.update(task)) {
                    dbToGoogle[SYNC_CHANGE_TASK]++;
                    bListUpdated = true;
                }
            }

        }

        Date updated = list.updated;
        if (bListUpdated) {
            TaskList updatedList = mAPI.taskLists.get(list.id);
            if (updatedList != null) {
                Log.d(TAG, "New Updated = " + updatedList.updated);
                updated = updatedList.updated; //Updated modified time
            }
        }

        if(updated != null)
            mDb.setSyncKey("updated_" + listId, mDateFormat.format(updated));
    }

    private static void getTokenAndSync(final Context context, final Callback callback)
    {
        Log.d(TAG, Build.FINGERPRINT + "\t" + Build.PRODUCT);
        if(Build.PRODUCT.contains("vbox")) //Emulator, use manual code
        {
            String token = "ya29.4gG18bQqBP_gmMteRgL4GmP4VpObYjuvfNMvuPgpxO7OShpc1N9Y4lHx2k8TFHFYVF3D-P4";
            SyncTask task = new SyncTask(context,token,callback);
            task.execute();
            return;
        }

        Date dtLastToken = Prefs.getPrefDate(context, Prefs.KEY_AUTHTOKEN_DATE);
        long dtDiff = (System.currentTimeMillis() - dtLastToken.getTime()) / 1000;
        if(dtDiff < 3500) //Token is a little less than 1 hour old
        {
            Log.d(TAG,"Using existing token, remaining minutes: " + (3600 - dtDiff) / 60);
            String token = Prefs.getPref(context, Prefs.KEY_AUTHTOKEN);
            SyncTask task = new SyncTask(context,token,callback);
            task.execute();
            return;
        }

        Log.d(TAG,"Getting Token");
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        String accountName = Prefs.getPref(context,Prefs.KEY_ACCOUNT_NAME);
        Account account = null;

        for(Account tmpAccount: accounts) {
            if(tmpAccount.name.contentEquals(accountName))
                account = tmpAccount;
        }

        if(account != null) {
            accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, null, true, new AccountManagerCallback<Bundle>()
            {
                public void run(AccountManagerFuture<Bundle> future)
                {
                    try {
                        // If the user has authorized your application to use the tasks API a token is available.
                        String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
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
            }, null);
        }
        else
            callback.onAuthError(null);


        Log.d(TAG, "Getting Token END");
    }

    private static class SyncTask extends AsyncTask<Void, Void, Void>
    {
        private final Context mContext;
        private final String mAuthToken;
        private final Callback mCallback;
        private boolean mResult = false;
        private int mChanges = 0;
        private Exception mError = null;

        public SyncTask(Context context, String sAuth, Callback callback)
        {
            mContext = context;
            mAuthToken = sAuth;
            mCallback = callback;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Sync sync = new Sync(mContext,mAuthToken);

            try {
                mResult = sync.run();
            } catch (TasksAPI.TasksAPIException e) {
                mError = e;
                mResult = false;
            }

            for(int i = 0; i < sync.dbToGoogle.length; i++) {
                mChanges += sync.dbToGoogle[i];
                mChanges += sync.googleToDb[i];
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            Log.d(TAG,"Result=" + mResult + " Changes=" + mChanges);
            if(mResult)
                Prefs.savePrefDate(mContext,Prefs.KEY_LAST_SYNC, new Date());

            mCallback.onSyncFinish(mResult,mError);
        }
    }


    private static Task getTask(ArrayList<Task> tasks, String sId)
    {
        for(Task task : tasks)
        {
            if(task.id.contentEquals(sId))
                return task;
        }

        return null;
    }

}
