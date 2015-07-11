package org.cerion.todolist;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.AsyncTask;
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
    //public static final int RESULT_UNKNOWN = -1;
    //public static final int RESULT_NOCHANGE = 0;
    //public static final int RESULT_CHANGES = 1;
    //public static final int RESULT_AUTHERROR = 2;

    //Instance variablesr
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private TasksAPI mAPI = null;
    private Database mDb = null;
    private int[] googleToDb = { 0, 0, 0, 0, 0, 0 }; //Add Change Delete Lists / Tasks
    private int[] dbToGoogle = { 0, 0, 0, 0, 0, 0 };
    private Map<String,String> mSyncKeys = null;

    public interface Callback
    {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void syncTaskLists(Context context, Callback callback)
    {
        getTokenAndSync(context,callback);
    }

    public Sync(Context context, String sAuthKey)
    {
        mDb = Database.getInstance(context);
        mAPI = new TasksAPI(sAuthKey);
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mSyncKeys = mDb.getSyncKeys();
    }

    private void run()
    {
        ArrayList<TaskList> googleLists = mAPI.getTaskLists();
        if(googleLists.size() == 0)
            return;// RESULT_AUTHERROR;

        ArrayList<TaskList> dbLists = mDb.getTaskLists();

        //Google->Local (Added or Updated)
        for(TaskList curr : googleLists)
        {
            TaskList dbList = TaskList.get(dbLists,curr.id);
            if(dbList != null)
            {
                if(!curr.title.contentEquals(dbList.title))
                {
                    //Name mismatch, update only if local list was not renamed
                    if(!dbList.isRenamed())
                    {
                        googleToDb[SYNC_CHANGE_LIST]++;
                        mDb.updateTaskList(curr);
                    }
                }
            }
            else //Does not exist locally, add it
            {
                googleToDb[SYNC_ADD_LIST]++;
                mDb.addTaskList(curr);
            }
        }

        //Deleted
        //Verify database list still exists on web, otherwise it was deleted
        for(TaskList curr : dbLists)
        {
            if(curr.hasTempId()) //Temp ids don't exist on web since they have not been created yet
                continue;

            TaskList googleList = TaskList.get(googleLists,curr.id);
            if(googleList == null)
            {
                googleToDb[SYNC_DELETE_LIST]++;
                mDb.deleteTaskList(curr);
            }

        }

        //If any changes made to local database, just reload it
        if(googleToDb[SYNC_DELETE_LIST] > 0 || googleToDb[SYNC_ADD_LIST] > 0)
            dbLists = mDb.getTaskLists();

        //Local -> Google
        for(TaskList dbList : dbLists)
        {
            if(dbList.hasTempId())
            {
                TaskList addedList = mAPI.addTaskList(dbList);
                if(addedList != null)
                {
                    mDb.setTaskListId(dbList, addedList.id);
                    dbList.id = addedList.id;
                    googleLists.add(addedList);
                }

                dbToGoogle[SYNC_ADD_LIST]++;
            }

            if(dbList.isRenamed())
            {

                TaskList googleList = TaskList.get(googleLists,dbList.id);

                if (googleList != null)
                {
                    if(mAPI.updateTaskList(dbList))
                    {
                        //Save state in db to indicate rename was successful
                        dbList.setRenamed(false);
                        mDb.updateTaskList(dbList);
                        dbToGoogle[SYNC_CHANGE_LIST]++;
                    }
                    else
                        Log.d(TAG,"Failed to rename list");
                }
                else //This shouldn't be possible, if list does not exist it should have deleted database list in earlier code
                    Log.d(TAG,"Error: Failed to find list to update");
            }

        }


        //Check for task changes
        if(googleLists.size() != dbLists.size())//TODO, when adding/deleting list modify other array so both match at this point
            Log.e(TAG,"ERROR: list sizes do not match");
        //Use web lists since it has updated time set which we need for comparisons
        ArrayList<TaskList> lists = googleLists;

        for(TaskList list : lists)
            syncTasks(list);

        //TODO, full sync where we don't check saved values

        //Save results
        Date lastSync = new Date();
        mDb.setSyncKey("lastSync", lastSync.toString());

        mDb.print();

        int changes = googleToDb[0] + googleToDb[1] + googleToDb[2] + dbToGoogle[0] + dbToGoogle[1] + dbToGoogle[2];
        Log.d(TAG,"Add/Change/Deletes = " + changes);
        Log.d(TAG, "Google to DB: Lists (" + googleToDb[0] + "," + googleToDb[1] + "," + googleToDb[2] + ") Tasks (" + googleToDb[3] + "," + googleToDb[4] + "," + googleToDb[5] + ")");
        Log.d(TAG, "DB to Google: Lists (" + dbToGoogle[0] + "," + dbToGoogle[1] + "," + dbToGoogle[2] + ") Tasks (" + dbToGoogle[3] + "," + dbToGoogle[4] + "," + dbToGoogle[5] + ")");

        //if(changes > 0)
        //    return RESULT_CHANGES;
        //else
        //    return RESULT_NOCHANGE;
    }

    private void syncTasks(TaskList list)
    {
        String key = "updated_" + list.id;
        String lastUpdatedSaved = mSyncKeys.get(key);

        String listId = list.id;
        Log.d(TAG, "syncTasks() " + listId + "\t" + lastUpdatedSaved + "\t" + list.updated);
        Date dtLastUpdated = null;

        try
        {
            if(lastUpdatedSaved != null)
                dtLastUpdated = mDateFormat.parse(lastUpdatedSaved);
        }
        catch (ParseException e)
        {
            //e.printStackTrace();
        }

        ArrayList<Task> webTasks = null;
        if(dtLastUpdated == null) //TODO or reread
        {
            Log.d(TAG, "New list, getting all");
            webTasks = mAPI.getTasks(listId,null);

        }
        else if(list.updated.after(dtLastUpdated))
        {
            Log.d(TAG, "Getting updated Tasks");
            Log.d(TAG, "Web   = " + list.updated);
            Log.d(TAG, "Saved = " + dtLastUpdated);
            dtLastUpdated.setTime(dtLastUpdated.getTime() + 1000); //Increase by 1 second to avoid getting previous updated record which already synced
            webTasks = mAPI.getTasks(listId,dtLastUpdated);
        }
        //else
        //    Log.d(TAG, "No changes");

        ArrayList<Task> dbTasks = mDb.getTasks(listId);

        if(webTasks != null)
        {
            for (Task task : webTasks)
            {
                Task dbTask = getTask(dbTasks,task.id);
                if(dbTask != null)
                {
                    if (task.deleted)
                    {
                        googleToDb[SYNC_DELETE_TASK]++;
                        mDb.deleteTask(task);
                        //TODO, Remove from list
                    }
                    else
                    {
                        googleToDb[SYNC_CHANGE_TASK]++;
                        mDb.updateTask(task);
                        //TODO conflicts
                        //TODO, if web wins delete from dbTasks so we don't process on db->web phase
                    }
                }
                else if(task.deleted)
                {
                    //Task was deleted from web before it was ever added to our database
                }
                else
                {
                    googleToDb[SYNC_ADD_TASK]++;
                    mDb.addTask(task);
                }
            }
        }

        //Database -> Web
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
                    Log.d(TAG,"ERROR, deleted task not modified");

                if(task.hasTempId()) //If never added just delete from local database
                {
                    mDb.deleteTask(task);
                }
                else if (mAPI.deleteTask(task))
                {
                    dbToGoogle[SYNC_DELETE_TASK]++;
                    bListUpdated = true;
                    mDb.deleteTask(task);
                }

            }
            else if(task.hasTempId())
            {
                dbToGoogle[SYNC_ADD_TASK]++;
                bListUpdated = true;
                Log.d(TAG,"TODO add task");
            }
            else if(bModified)
            {
                mAPI.updateTask(task);
                dbToGoogle[SYNC_CHANGE_TASK]++;
                bListUpdated = true;
                Log.d(TAG,"Modified: " + task.title);
            }

        }

        if(bListUpdated)
        {
            //TODO, get single list instead of all
            ArrayList<TaskList> lists = mAPI.getTaskLists();
            TaskList updatedList = TaskList.get(lists,listId);
            if(updatedList != null)
            {
                Log.d(TAG,"New Updated = " + updatedList.updated);
                list.updated = updatedList.updated; //Updated modified time
            }

        }


        //TODO add db dirty flag for anytime user change is made, after sync reset variable??

        //TODO, skip this if something failed above
        mDb.setSyncKey("updated_" + list.id, mDateFormat.format(list.updated));
    }

    private static void getTokenAndSync(final Context context, final Callback callback)
    {
        /*
        if(true) //Emulator, use manual code
        {
            String token = "ya29.pgHfcRyQeYabfAfvynoVnoBYUS3sbGZy40Sw408oyQ1ikcjLPvEbj6652uVby6aFWLLNX2eh60j2zg";
            SyncTask task = new SyncTask(context,token,callback);
            task.execute();
            return;
        }
        */

        Date dtLastToken = Prefs.getPrefDate(context,Prefs.PREF_AUTHTOKEN_DATE);
        long dtDiff = (System.currentTimeMillis() - dtLastToken.getTime()) / 1000;
        if(dtDiff < 3500) //Token is a little less than 1 hour old
        {
            Log.d(TAG,"Using existing token, remaining minutes: " + (3600 - dtDiff) / 60);
            String token = Prefs.getPref(context, Prefs.PREF_AUTHTOKEN);
            SyncTask task = new SyncTask(context,token,callback);
            task.execute();
            return;
        }

        Log.d(TAG,"Getting Token");
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        String accountName = Prefs.getPref(context,context.getString(R.string.pref_key_account));
        Account account = null;

        for(Account tmpAccount: accounts)
        {
            if(tmpAccount.name.contentEquals(accountName))
                account = tmpAccount;
        }

        if(account != null)
        {
            accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, null, true, new AccountManagerCallback<Bundle>()
            {
                public void run(AccountManagerFuture<Bundle> future)
                {
                    try
                    {
                        // If the user has authorized your application to use the tasks API a token is available.
                        String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                        Prefs.savePref(context, Prefs.PREF_AUTHTOKEN, token);
                        Prefs.savePrefDate(context,Prefs.PREF_AUTHTOKEN_DATE,new Date());

                        //syncTaskLists(context,token);
                        Log.d(TAG,"Starting SyncTask");
                        SyncTask task = new SyncTask(context,token,callback);
                        task.execute();

                    }
                    catch (Exception e)
                    {
                        callback.onFailure(e);
                        Log.d("TEST","ERROR");
                    }
                }
            }, null);
        }

        Log.d(TAG, "Getting Token END");
    }

    private static class SyncTask extends AsyncTask<Void, Void, Void>
    {
        private Context mContext;
        private String mAuthToken;
        private Callback mCallback;
        public SyncTask(Context context, String sAuth, Callback callback)
        {
            mContext = context;
            mAuthToken = sAuth;
            mCallback = callback;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            Sync sync = new Sync(mContext,mAuthToken);
            sync.run();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            /*
            if(mResult == Sync.RESULT_CHANGES)
            {
                mStatus.setText("Updated");
                loadTaskLists(); //Refresh UI
            }
            else if(mResult == Sync.RESULT_NOCHANGE)
                mStatus.setText("No Changes");
            else
                mStatus.setText("Error");
                */

            mCallback.onSuccess();
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
