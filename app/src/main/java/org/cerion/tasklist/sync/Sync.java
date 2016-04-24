package org.cerion.tasklist.sync;


import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.cerion.tasklist.data.Database;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskList;
import org.cerion.tasklist.data.TasksAPI;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Sync {

    private static final String TAG = Sync.class.getSimpleName();
    private static final int SYNC_ADD_LIST = 0;
    private static final int SYNC_CHANGE_LIST = 1;
    private static final int SYNC_DELETE_LIST = 2;
    private static final int SYNC_ADD_TASK = 3;
    private static final int SYNC_CHANGE_TASK = 4;
    private static final int SYNC_DELETE_TASK = 5;


    //Instance variables
    private TasksAPI mAPI = null;
    private Database mDb = null;
    final int[] googleToDb = { 0, 0, 0, 0, 0, 0 }; //Add Change Delete Lists / Tasks
    final int[] dbToGoogle = { 0, 0, 0, 0, 0, 0 };


    /**
     * Get token and run sync process in background
     * @param context Context
     * @param activity Use for permissions prompt if starting from activity
     * @param callback Listener for when sync completes
     */
    public static void run(Context context, @Nullable Activity activity, OnSyncCompleteListener callback)
    {
        String token = Auth.getSavedToken(context);

        if(token != null) {
            SyncTask task = new SyncTask(context, token, callback);
            task.execute();
        } else {
            //Get new token then run sync
            Auth.getTokenAndSync(context, activity, callback);
        }
    }

    Sync(Context context, String sAuthKey)
    {
        mDb = Database.getInstance(context);
        mAPI = new TasksAPI(sAuthKey);
    }

    boolean run() throws TasksAPI.TasksAPIException {
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

                    mDb.taskLists.setId(dbList,curr.id); //assign ID
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
                    mDb.taskLists.setId(dbList, addedList.id);
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

        for(TaskList list : googleLists) {
            TaskList dbList = TaskList.get(dbLists, list.id);
            if(dbList != null)
                syncTasks(list, dbList.getUpdated());
            else
                Log.e(TAG,"Unable to find database list"); //TODO throw exception
        }

        //mDb.print();

        Log.d(TAG, "Google to DB: Lists (" + googleToDb[0] + "," + googleToDb[1] + "," + googleToDb[2] + ") Tasks (" + googleToDb[3] + "," + googleToDb[4] + "," + googleToDb[5] + ")");
        Log.d(TAG, "DB to Google: Lists (" + dbToGoogle[0] + "," + dbToGoogle[1] + "," + dbToGoogle[2] + ") Tasks (" + dbToGoogle[3] + "," + dbToGoogle[4] + "," + dbToGoogle[5] + ")");

        return true;
    }

    private void syncTasks(TaskList list, Date savedUpdatedNEW) throws TasksAPI.TasksAPIException {

        if(list.getUpdated().getTime() == 0) {
            Log.e(TAG,"invalid updated time"); //TODO, need new exception for this class
            return;
        }

        Date webUpdated = list.getUpdated();
        //Date dtLastUpdated = null;
        String listId = list.id;

        Log.d(TAG, "syncTasks() " + listId + "\t" + savedUpdatedNEW + "\t" + webUpdated);

        ArrayList<Task> webTasks = null;
        if (savedUpdatedNEW.getTime() == 0) {
            Log.d(TAG, "New list, getting all");
            webTasks = mAPI.tasks.getList(listId, null);
        } else if (webUpdated.after(savedUpdatedNEW)) {
            //The default list can get its modified time updated without having any new tasks, we'll get 0 tasks here sometimes but not much we can do about it
            Log.d(TAG, "Getting updated Tasks");
            Log.d(TAG, "Web   = " + webUpdated);
            Log.d(TAG, "Saved = " + savedUpdatedNEW);

            //Increase by 1 second to avoid getting previous updated record which already synced
            webTasks = mAPI.tasks.getList(listId, new Date(savedUpdatedNEW.getTime() + 1000)  );
        }

        List<Task> dbTasks = mDb.tasks.getList(listId);
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
            if(savedUpdatedNEW.getTime() == 0 || task.updated.after(savedUpdatedNEW))
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

        //If this function updated the list, need to retrieve it again to get new updated time
        if (bListUpdated) {
            TaskList updatedList = mAPI.taskLists.get(list.id);
            if (updatedList != null) {
                Log.d(TAG, "New Updated = " + updatedList.getUpdated());
                webUpdated = updatedList.getUpdated(); //Updated modified time
            }
        }

        if(webUpdated.getTime() > savedUpdatedNEW.getTime()) {
            mDb.taskLists.setLastUpdated(list, webUpdated);
        }

    }


    private static Task getTask(List<Task> tasks, String sId)
    {
        for(Task task : tasks)
        {
            if(task.id.contentEquals(sId))
                return task;
        }

        return null;
    }
}
