package org.cerion.tasklist.sync;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.cerion.tasklist.data.AppDatabase;
import org.cerion.tasklist.data.GoogleTasksAPI;
import org.cerion.tasklist.data.IGoogleTasksAPI;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskDao;
import org.cerion.tasklist.data.TaskList;
import org.cerion.tasklist.data.TaskListDao;

import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;

public class Sync {

    private static final String TAG = Sync.class.getSimpleName();
    private static final int SYNC_ADD_LIST = 0;
    private static final int SYNC_CHANGE_LIST = 1;
    private static final int SYNC_DELETE_LIST = 2;
    private static final int SYNC_ADD_TASK = 3;
    private static final int SYNC_CHANGE_TASK = 4;
    private static final int SYNC_DELETE_TASK = 5;

    private static final boolean RESYNC_WEB = false;

    //Instance variables
    private GoogleTasksAPI mAPI = null;
    private TaskDao taskDb;
    private TaskListDao listDb;
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
        String token = AuthTools.getSavedToken(context);

        if(token != null) {
            SyncTask task = new SyncTask(context, token, callback);
            task.execute();
        } else {
            //Get new token then run sync
            AuthTools.getTokenAndSync(context, activity, callback);
        }
    }

    Sync(Context context, String sAuthKey) {
        AppDatabase db = AppDatabase.getInstance(context);
        taskDb = db.taskDao();
        listDb = db.taskListDao();
        mAPI = new GoogleTasksAPI(sAuthKey);
    }

    boolean run() throws IGoogleTasksAPI.APIException {
        List<TaskList> googleLists = mAPI.taskLists.list();
        if(googleLists.size() == 0)
            return false;

        List<TaskList> dbLists = listDb.getAll();

        //Google->Local (Added or Updated)
        for(TaskList curr : googleLists) {
            TaskList dbList = TaskList.Companion.get(dbLists,curr.getId());
            //--- UPDATE
            if(dbList != null) {
                if(!curr.getTitle().contentEquals(dbList.getTitle())) {
                    //Name mismatch, update only if local list was not renamed
                    if(!dbList.isRenamed()) {
                        listDb.update(curr);
                        googleToDb[SYNC_CHANGE_LIST]++;
                    }
                }
            }
            //--- MERGE default, first sync only
            else if(curr.isDefault()) {
                dbList = TaskList.Companion.getDefault(dbLists);
                if(dbList != null) {
                    if(!dbList.isRenamed()) {
                        dbList.setTitle(curr.getTitle());
                        listDb.update(dbList);
                    }

                    listDb.updateId(dbList.getId(), curr.getId()); //assign ID
                    dbList.setId(curr.getId());
                }
                else
                    Log.e(TAG,"missing default list");
            }
            //--- ADD
            else { //Does not exist locally, add it
                listDb.add(curr);
                googleToDb[SYNC_ADD_LIST]++;

            }
        }

        //--- DELETE
        //Verify database list still exists on web, otherwise it was deleted
        for(TaskList curr : dbLists) {
            if(curr.getHasTempId()) //Temp ids don't exist on web since they have not been created yet
                continue;

            TaskList googleList = TaskList.Companion.get(googleLists,curr.getId());
            if(googleList == null) {
                // TODO this will cascade delete so log tasks count its removing...
                listDb.delete(curr);
                googleToDb[SYNC_DELETE_LIST]++;
            }

        }

        // TODO always reload?
        //If any changes made to local database, just reload it
        if(googleToDb[SYNC_DELETE_LIST] > 0 || googleToDb[SYNC_ADD_LIST] > 0)
            dbLists = listDb.getAll();

        //Local ----> Google
        for(TaskList dbList : dbLists) {
            //--- ADD
            if(dbList.getHasTempId()) {
                TaskList addedList = mAPI.taskLists.insert(dbList);
                if(addedList != null) {
                    listDb.updateId(dbList.getId(), addedList.getId());
                    dbList.setId(addedList.getId());
                    googleLists.add(addedList);
                    dbToGoogle[SYNC_ADD_LIST]++;
                }
                else {
                    Log.e(TAG, "Failed to add list");
                    return false;
                }
            }

            //--- UPDATE
            if(dbList.isRenamed()) {
                TaskList googleList = TaskList.Companion.get(googleLists,dbList.getId());

                if (googleList != null) {
                    if(mAPI.taskLists.update(dbList)) {
                        //Save state in db to indicate rename was successful
                        dbList.setRenamed(false);
                        listDb.update(dbList);
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
            TaskList dbList = TaskList.Companion.get(dbLists, list.getId());
            if(dbList != null)
                syncTasks(list, dbList.getUpdated());
            else
                Log.e(TAG,"Unable to find database list"); //TODO throw exception
        }

        Log.d(TAG, "Google to DB: Lists (" + googleToDb[0] + "," + googleToDb[1] + "," + googleToDb[2] + ") Tasks (" + googleToDb[3] + "," + googleToDb[4] + "," + googleToDb[5] + ")");
        Log.d(TAG, "DB to Google: Lists (" + dbToGoogle[0] + "," + dbToGoogle[1] + "," + dbToGoogle[2] + ") Tasks (" + dbToGoogle[3] + "," + dbToGoogle[4] + "," + dbToGoogle[5] + ")");

        return true;
    }

    private void syncTasks(TaskList list, Date savedUpdatedNEW) throws IGoogleTasksAPI.APIException {

        if(list.getUpdated().getTime() == 0) {
            Log.e(TAG,"invalid updated time"); //TODO, need new exception for this class
            return;
        }

        Date webUpdated = list.getUpdated();
        //Date dtLastUpdated = null;
        String listId = list.getId();

        Log.d(TAG, "syncTasks() " + listId + "\t" + savedUpdatedNEW + "\t" + webUpdated);

        List<Task> webTasks = null;
        if (savedUpdatedNEW.getTime() == 0) {
            Log.d(TAG, "New list, getting all");
            webTasks = mAPI.tasks.list(listId, null);
        } else if(RESYNC_WEB) {
            Log.d(TAG, "Re-syncing web, getting all");
            webTasks = mAPI.tasks.list(listId, null);
        }
        else if (webUpdated.after(savedUpdatedNEW)) {
            //The default list can get its modified time updated without having any new tasks, we'll get 0 tasks here sometimes but not much we can do about it
            Log.d(TAG, "Getting updated Tasks");
            Log.d(TAG, "Web   = " + webUpdated);
            Log.d(TAG, "Saved = " + savedUpdatedNEW);

            //Increase by 1 second to avoid getting previous updated record which already synced
            webTasks = mAPI.tasks.list(listId, new Date(savedUpdatedNEW.getTime() + 1000)  );
        }

        /**************
         Web -> Database
         **************/
        List<Task> dbTasks = taskDb.getAllbyList(listId);
        if (webTasks != null) {
            for (Task task : webTasks) {
                Task dbTask = getTask(dbTasks, task.getId());
                if (dbTask != null) {
                    if (task.getDeleted()) {
                        taskDb.delete(task);
                        googleToDb[SYNC_DELETE_TASK]++;
                        dbTasks.remove(dbTask);
                    } else if(dbTask.getDeleted()) {
                        Log.d(TAG,"Ignoring update since deleted on local"); //Local task is deleted and will be handled on next phase
                    }
                    else {
                        //Conflict
                        if(dbTask.getUpdated().getTime() > task.getUpdated().getTime())
                            Log.e(TAG,"Conflict: Local task was updated most recently");
                        else {
                            dbTasks.remove(dbTask);
                            taskDb.update(task);
                            googleToDb[SYNC_CHANGE_TASK]++;
                        }

                    }
                } else if (task.getDeleted()) {
                    Log.d(TAG,"Ignoring web delete since record was never added locally");
                } else {
                    taskDb.add(task);
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
            if(savedUpdatedNEW.getTime() == 0 || task.getUpdated().after(savedUpdatedNEW))
                bModified = true;

            if(task.getDeleted())
            {
                //Deleted tasks get processed then deleted from database
                //Modified time is not required but should be logged for potential bugs elsewhere
                if(!bModified)
                    Log.e(TAG,"ERROR, deleted task not modified");

                if(task.getHasTempId()) //If never added just delete from local database
                {
                    taskDb.delete(task);
                }
                else if (mAPI.tasks.delete(task))
                {
                    taskDb.delete(task);
                    dbToGoogle[SYNC_DELETE_TASK]++;
                    bListUpdated = true;
                }
            }
            else if(task.getHasTempId())
            {
                Task updated = mAPI.tasks.insert(task);
                if(updated != null) {
                    taskDb.delete(task);
                    task.setId(updated.getId());
                    task.setListId(updated.getListId());
                    taskDb.add(task);

                    dbToGoogle[SYNC_ADD_TASK]++;
                    bListUpdated = true;

                    if(listId.isEmpty()) //When adding a new task without a list this will be the default task list id
                        listId = updated.getListId();
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
            TaskList updatedList = mAPI.taskLists.get(list.getId());
            if (updatedList != null) {
                Log.d(TAG, "New Updated = " + updatedList.getUpdated());
                webUpdated = updatedList.getUpdated(); //Updated modified time
            }
        }

        if(webUpdated.getTime() > savedUpdatedNEW.getTime()) {
            listDb.setLastUpdated(list.getId(), webUpdated);
        }
    }

    private static Task getTask(List<Task> tasks, String sId) {
        for(Task task : tasks) {
            if(task.getId().contentEquals(sId))
                return task;
        }

        return null;
    }
}
