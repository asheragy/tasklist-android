package org.cerion.tasklist.sync


import android.content.Context
import android.util.Log
import org.cerion.tasklist.database.*
import org.cerion.tasklist.googleapi.GoogleApi
import org.cerion.tasklist.googleapi.GoogleApiException
import org.cerion.tasklist.googleapi.GoogleTasksRepository
import java.util.*


internal class Sync(private val listDb: TaskListDao, private val taskDb: TaskDao, private val googleRepo: GoogleTasksRepository, private val prefs: Prefs) {

    private lateinit var googleToDb: SyncChanges
    private lateinit var dbToGoogle: SyncChanges

    fun run(): SyncResult {
        googleToDb = SyncChanges()
        dbToGoogle = SyncChanges()
        val result = SyncResult(false, googleToDb, dbToGoogle)

        val googleLists = googleRepo.getLists().toMutableList()
        if (googleLists.isEmpty())
            return result

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
                        googleToDb.listChange++
                    }
                }
            }
            else if (curr.isDefault) {
                dbList = dbLists.getDefault()
                if (dbList != null) {
                    if (!dbList.isRenamed) {
                        dbList.title = curr.title
                        listDb.update(dbList)
                    }

                    listDb.updateId(dbList.id, curr.id) //assign ID
                    dbList.id = curr.id
                }
                else
                    Log.e(TAG, "missing default list")
            }
            else { //Does not exist locally, add it
                listDb.add(curr)
                googleToDb.listAdd++

                // Then add all its tasks
                syncTasks(curr, Date(0))
            }//--- ADD
            //--- MERGE default, first sync only
        }

        //--- DELETE
        //Verify database list still exists on web, otherwise it was deleted
        for (curr in dbLists) {
            //Temp ids don't exist on web since they have not been created yet
            if (curr.hasTempId)
                continue

            val googleList = googleLists.getById(curr.id)
            if (googleList == null) {
                // TODO this will cascade delete so log tasks count its removing...
                listDb.delete(curr)
                googleToDb.listDelete++
            }
        }

        // TODO always reload?
        //If any changes made to local database, just reload it
        if (googleToDb.listDelete > 0 || googleToDb.listAdd > 0)
            dbLists = listDb.getAll()

        //Local ----> Google
        for (dbList in dbLists) {
            //--- ADD
            if (dbList.hasTempId) {
                val addedList = googleRepo.createList(dbList)
                if (addedList != null) {
                    // If current displayed list is this one update the ID so it reloads with this list active
                    val lastId = prefs.getString(Prefs.KEY_LAST_SELECTED_LIST_ID)
                    if (lastId != null && lastId.contentEquals(dbList.id))
                        prefs.setString(Prefs.KEY_LAST_SELECTED_LIST_ID, addedList.id)

                    listDb.updateId(dbList.id, addedList.id)
                    dbList.id = addedList.id

                    googleLists.add(addedList)
                    dbToGoogle.listAdd++
                }
                else {
                    Log.e(TAG, "Failed to add list")
                    return result
                }
            }

            //--- UPDATE
            if (dbList.isRenamed) {
                val googleList = googleLists.getById(dbList.id)

                if (googleList != null) {
                    if (googleRepo.updateList(dbList)) {
                        //Save state in db to indicate rename was successful
                        dbList.isRenamed = false
                        listDb.update(dbList)
                        dbToGoogle.listChange++
                    }
                    else
                        Log.d(TAG, "Failed to rename list")
                }
                else {
                    //This shouldn't be possible, if list does not exist it should have deleted database list in earlier code
                    Log.e(TAG, "Error: Failed to find list to update")
                    return result
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
        dbLists.forEach { list ->
            if (list.deleted) {
                val tasks = taskDb.getAllByList(list.id)
                if (tasks.isEmpty()) {
                    if (googleRepo.deleteList(list)) {
                        listDb.delete(list)
                        dbToGoogle.listDelete++
                    }
                }
                else {
                    // TODO this means a deleted list had tasks added on web which may invalidate the delete, consider marking as not deleted
                }
            }
        }

        Log.d(TAG, "To Local : ${googleToDb.totalChanges} $googleToDb")
        Log.d(TAG, "To Google: ${dbToGoogle.totalChanges} $dbToGoogle")
        prefs.setDate(Prefs.KEY_LAST_SYNC, Date())

        result.success = true
        return result
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
            webTasks = googleRepo.getTasks(listId)
        }
        else if (RESYNC_WEB) {
            Log.d(TAG, "Re-syncing web, getting all")
            webTasks = googleRepo.getTasks(listId)
        }
        else if (webUpdated.after(savedUpdatedNEW)) {
            //The default list can get its modified time updated without having any new tasks, we'll get 0 tasks here sometimes but not much we can do about it
            Log.d(TAG, "Getting updated Tasks")
            Log.d(TAG, "Web   = $webUpdated")
            Log.d(TAG, "Saved = $savedUpdatedNEW")

            //Increase by 1 second to avoid getting previous updated record which already synced
            webTasks = googleRepo.getTasks(listId, Date(savedUpdatedNEW.time + 1000))
        }

        /**************
         * Web -> Database
         */
        val dbTasks = taskDb.getAllByList(listId).toMutableList()

        webTasks?.forEach { task ->
            val dbTask = dbTasks.getById(task.id)
            if (dbTask != null) {
                if (task.deleted) {
                    taskDb.delete(task)
                    googleToDb.taskDelete++
                    dbTasks.remove(dbTask)
                }
                else if (dbTask.deleted) {
                    Log.d(TAG, "Ignoring update since deleted on local") //Local task is deleted and will be handled on next phase
                }
                else {
                    //Conflict
                    if (dbTask.updated.time > task.updated.time)
                        Log.e(TAG, "Conflict: Local task was updated most recently")
                    else {
                        dbTasks.remove(dbTask)
                        taskDb.update(task)
                        googleToDb.taskChange++
                    }
                }
            }
            else if (task.deleted) {
                Log.d(TAG, "Ignoring web delete since record was never added locally")
            }
            else {
                taskDb.add(task)
                googleToDb.taskAdd++
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

                if (task.hasTempId) { //If never added just delete from local database
                    taskDb.delete(task)
                }
                else if (googleRepo.deleteTask(task)) {
                    taskDb.delete(task)
                    dbToGoogle.taskDelete++
                    bListUpdated = true
                }
            }
            else if (task.hasTempId) {
                googleRepo.createTask(task)?.let { updated ->
                    taskDb.delete(task)
                    task.id = updated.id
                    task.listId = updated.listId
                    taskDb.add(task)

                    dbToGoogle.taskAdd++
                    bListUpdated = true

                    // TODO what is this assignment used for?
                    if (listId.isEmpty())
                    //When adding a new task without a list this will be the default task list id
                        listId = updated.listId
                }
            }
            else if (bModified) {
                if (googleRepo.updateTask(task)) {
                    dbToGoogle.taskChange++
                    bListUpdated = true
                }
            }

        }

        //If this function updated the list, need to retrieve it again to get new updated time
        if (bListUpdated) {
            val updatedList = googleRepo.getList(list.id)
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
        private const val RESYNC_WEB = false

        fun getInstance(context: Context, sAuthKey: String): Sync {
            val db = AppDatabase.getInstance(context)
            val api = GoogleApi(sAuthKey)
            val repo = GoogleTasksRepository(api)

            return Sync(db!!.taskListDao(), db.taskDao(), repo, Prefs.getInstance(context))
        }
    }
}

data class SyncResult(var success: Boolean, val toLocal: SyncChanges, val toRemote: SyncChanges)

data class SyncChanges(
        var listAdd: Int = 0,
        var listChange: Int = 0,
        var listDelete: Int = 0,
        var taskAdd: Int = 0,
        var taskChange: Int = 0,
        var taskDelete: Int = 0) {

    val totalChanges: Int
        get() = listAdd + listChange + listDelete + taskAdd + taskChange + taskDelete
}