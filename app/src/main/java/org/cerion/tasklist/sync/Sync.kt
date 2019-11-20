package org.cerion.tasklist.sync


import android.content.Context
import android.util.Log
import org.cerion.tasklist.database.*
import org.cerion.tasklist.googleapi.GoogleApi
import org.cerion.tasklist.googleapi.GoogleApiException
import org.cerion.tasklist.googleapi.GoogleTasklistsApi
import org.cerion.tasklist.googleapi.GoogleTasksApi
import java.util.*

internal class Sync(private val listDb: TaskListDao, private val taskDb: TaskDao, //Instance variables
        //private GoogleApi mAPI = null;
                    private val listApi: GoogleTasklistsApi, private val taskApi: GoogleTasksApi, private val prefs: Prefs) {
    val googleToDb = intArrayOf(0, 0, 0, 0, 0, 0) //Add Change Delete Lists / Tasks
    val dbToGoogle = intArrayOf(0, 0, 0, 0, 0, 0)

    @Throws(GoogleApiException::class)
    fun run(): Boolean {
        val googleLists = listApi.getAll().toMutableList()
        if (googleLists.isEmpty())
            return false

        var dbLists = listDb.getAll()

        //Google->Local (Added or Updated)
        for (curr in googleLists) {
            var dbList = dbLists.getById(curr.id)
            //--- UPDATE
            if (dbList != null) {
                if (!curr.title.contentEquals(dbList.title)) {
                    //Name mismatch, update only if local list was not renamed
                    if (!dbList.isRenamed) {
                        listDb.update(curr)
                        googleToDb[SYNC_CHANGE_LIST]++
                    }
                }
            } else if (curr.isDefault) {
                dbList = dbLists.getDefault()
                if (dbList != null) {
                    if (!dbList.isRenamed) {
                        dbList.title = curr.title
                        listDb.update(dbList)
                    }

                    listDb.updateId(dbList.id, curr.id) //assign ID
                    dbList.id = curr.id
                } else
                    Log.e(TAG, "missing default list")
            } else { //Does not exist locally, add it
                listDb.add(curr)
                googleToDb[SYNC_ADD_LIST]++

                // Then add all its tasks
                syncTasks(curr, Date(0))
            }//--- ADD
            //--- MERGE default, first sync only
        }

        //--- DELETE
        //Verify database list still exists on web, otherwise it was deleted
        for (curr in dbLists) {
            if (curr.hasTempId)
            //Temp ids don't exist on web since they have not been created yet
                continue

            val googleList = googleLists.getById(curr.id)
            if (googleList == null) {
                // TODO this will cascade delete so log tasks count its removing...
                listDb.delete(curr)
                googleToDb[SYNC_DELETE_LIST]++
            }

        }

        // TODO always reload?
        //If any changes made to local database, just reload it
        if (googleToDb[SYNC_DELETE_LIST] > 0 || googleToDb[SYNC_ADD_LIST] > 0)
            dbLists = listDb.getAll()

        //Local ----> Google
        for (dbList in dbLists) {
            //--- ADD
            if (dbList.hasTempId) {
                val addedList = listApi.insert(dbList)
                if (addedList != null) {
                    // If current displayed list is this one update the ID so it reloads with this list active
                    val lastId = prefs.getString(Prefs.KEY_LAST_SELECTED_LIST_ID)
                    if (lastId != null && lastId.contentEquals(dbList.id))
                        prefs.setString(Prefs.KEY_LAST_SELECTED_LIST_ID, addedList.id)

                    listDb.updateId(dbList.id, addedList.id)
                    dbList.id = addedList.id

                    googleLists.add(addedList)
                    dbToGoogle[SYNC_ADD_LIST]++
                } else {
                    Log.e(TAG, "Failed to add list")
                    return false
                }
            }

            //--- UPDATE
            if (dbList.isRenamed) {
                val googleList = googleLists.getById(dbList.id)

                if (googleList != null) {
                    if (listApi.update(dbList)) {
                        //Save state in db to indicate rename was successful
                        dbList.isRenamed = false
                        listDb.update(dbList)
                        dbToGoogle[SYNC_CHANGE_LIST]++
                    } else
                        Log.d(TAG, "Failed to rename list")
                } else {
                    //This shouldn't be possible, if list does not exist it should have deleted database list in earlier code
                    Log.e(TAG, "Error: Failed to find list to update")
                    return false
                }

            }

            //--- DELETE LATER, allow task lists to be deleted locally, remove entry from array if success so we don't loop below
        }

        for (list in googleLists) {
            val dbList = dbLists.getById(list.id)
            if (dbList != null)
                syncTasks(list, dbList.updated)
            else
                Log.e(TAG, "Unable to find database list") //TODO throw exception
        }

        // If any local lists are empty and marked for deletion, delete from web
        for (list in dbLists) {
            if (list.deleted) {
                val tasks = taskDb.getAllByList(list.id)
                if (tasks.size == 0) {
                    if (listApi.delete(list)) {
                        listDb.delete(list)
                        dbToGoogle[SYNC_DELETE_LIST]++
                    }
                } else {
                    // TODO this means a deleted list had tasks added on web which may invalidate the delete, consider marking as not deleted
                }
            }
        }

        Log.d(TAG, "Google to DB: Lists (" + googleToDb[0] + "," + googleToDb[1] + "," + googleToDb[2] + ") Tasks (" + googleToDb[3] + "," + googleToDb[4] + "," + googleToDb[5] + ")")
        Log.d(TAG, "DB to Google: Lists (" + dbToGoogle[0] + "," + dbToGoogle[1] + "," + dbToGoogle[2] + ") Tasks (" + dbToGoogle[3] + "," + dbToGoogle[4] + "," + dbToGoogle[5] + ")")
        prefs.setDate(Prefs.KEY_LAST_SYNC, Date())

        return true
    }

    @Throws(GoogleApiException::class)
    private fun syncTasks(list: TaskList, savedUpdatedNEW: Date) {

        if (list.updated.time == 0L) {
            Log.e(TAG, "invalid updated time") //TODO, need new exception for this class
            return
        }

        var webUpdated = list.updated
        //Date dtLastUpdated = null;
        var listId = list.id

        Log.d(TAG, "syncTasks() $listId\t$savedUpdatedNEW\t$webUpdated")

        var webTasks: List<Task>? = null
        if (savedUpdatedNEW.time == 0L) {
            Log.d(TAG, "New list, getting all")
            webTasks = taskApi.list(listId, null)
        } else if (RESYNC_WEB) {
            Log.d(TAG, "Re-syncing web, getting all")
            webTasks = taskApi.list(listId, null)
        } else if (webUpdated.after(savedUpdatedNEW)) {
            //The default list can get its modified time updated without having any new tasks, we'll get 0 tasks here sometimes but not much we can do about it
            Log.d(TAG, "Getting updated Tasks")
            Log.d(TAG, "Web   = $webUpdated")
            Log.d(TAG, "Saved = $savedUpdatedNEW")

            //Increase by 1 second to avoid getting previous updated record which already synced
            webTasks = taskApi.list(listId, Date(savedUpdatedNEW.time + 1000))
        }

        /**************
         * Web -> Database
         */
        val dbTasks = taskDb.getAllByList(listId).toMutableList()
        if (webTasks != null) {
            for (task in webTasks) {
                val dbTask = getTask(dbTasks, task.id)
                if (dbTask != null) {
                    if (task.deleted) {
                        taskDb.delete(task)
                        googleToDb[SYNC_DELETE_TASK]++
                        dbTasks.remove(dbTask)
                    } else if (dbTask.deleted) {
                        Log.d(TAG, "Ignoring update since deleted on local") //Local task is deleted and will be handled on next phase
                    } else {
                        //Conflict
                        if (dbTask.updated.time > task.updated.time)
                            Log.e(TAG, "Conflict: Local task was updated most recently")
                        else {
                            dbTasks.remove(dbTask)
                            taskDb.update(task)
                            googleToDb[SYNC_CHANGE_TASK]++
                        }

                    }
                } else if (task.deleted) {
                    Log.d(TAG, "Ignoring web delete since record was never added locally")
                } else {
                    taskDb.add(task)
                    googleToDb[SYNC_ADD_TASK]++
                }
            }
        }


        /**************
         * Database -> Web
         */
        var bListUpdated = false
        for (task in dbTasks) {
            //Log.d(TAG,"Title = " + task.title + "\t" + task.updated);
            var bModified = false
            if (savedUpdatedNEW.time == 0L || task.updated.after(savedUpdatedNEW))
                bModified = true

            if (task.deleted) {
                //Deleted tasks get processed then deleted from database
                //Modified time is not required but should be logged for potential bugs elsewhere
                if (!bModified)
                    Log.e(TAG, "ERROR, deleted task not modified")

                if (task.hasTempId)
                //If never added just delete from local database
                {
                    taskDb.delete(task)
                } else if (taskApi.delete(task)) {
                    taskDb.delete(task)
                    dbToGoogle[SYNC_DELETE_TASK]++
                    bListUpdated = true
                }
            } else if (task.hasTempId) {
                val updated = taskApi.insert(task)
                if (updated != null) {
                    taskDb.delete(task)
                    task.id = updated.id
                    task.listId = updated.listId
                    taskDb.add(task)

                    dbToGoogle[SYNC_ADD_TASK]++
                    bListUpdated = true

                    if (listId.isEmpty())
                    //When adding a new task without a list this will be the default task list id
                        listId = updated.listId
                }
            } else if (bModified) {
                if (taskApi.update(task)) {
                    dbToGoogle[SYNC_CHANGE_TASK]++
                    bListUpdated = true
                }
            }

        }

        //If this function updated the list, need to retrieve it again to get new updated time
        if (bListUpdated) {
            val updatedList = listApi.get(list.id)
            if (updatedList != null) {
                Log.d(TAG, "New Updated = " + updatedList.updated)
                webUpdated = updatedList.updated //Updated modified time
            }
        }

        if (webUpdated.time > savedUpdatedNEW.time) {
            listDb.setLastUpdated(list.id, webUpdated)
        }
    }

    companion object {

        private val TAG = Sync::class.java.simpleName
        private val SYNC_ADD_LIST = 0
        private val SYNC_CHANGE_LIST = 1
        private val SYNC_DELETE_LIST = 2
        private val SYNC_ADD_TASK = 3
        private val SYNC_CHANGE_TASK = 4
        private val SYNC_DELETE_TASK = 5

        private val RESYNC_WEB = false

        fun getInstance(context: Context, sAuthKey: String): Sync {
            val db = AppDatabase.getInstance(context)
            val api = GoogleApi(sAuthKey)

            return Sync(db!!.taskListDao(), db.taskDao(), api.taskListsApi, api.tasksApi, Prefs.getInstance(context))
        }

        private fun getTask(tasks: List<Task>, sId: String): Task? {
            for (task in tasks) {
                if (task.id.contentEquals(sId))
                    return task
            }

            return null
        }
    }
}
