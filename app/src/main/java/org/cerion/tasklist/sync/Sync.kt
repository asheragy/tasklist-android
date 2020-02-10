package org.cerion.tasklist.sync


import android.content.Context
import android.util.Log
import org.cerion.tasklist.common.TAG
import org.cerion.tasklist.database.*
import org.cerion.tasklist.googleapi.GoogleApi
import org.cerion.tasklist.googleapi.GoogleTasksRepository
import java.util.*


internal class Sync(private val listDb: TaskListDao, private val taskDb: TaskDao, private val googleRepo: GoogleTasksRepository, private val prefs: Prefs) {

    fun lists(): SyncResult {
        // TODO add force sync flag if there was local changes never do this check
        if(Date().time - prefs.getDate(Prefs.KEY_LAST_LIST_SYNC).time < 1000 * 60) {
            Log.d(TAG,"Skipping lists since less than 1 minute")
            return SyncResult(true)
        }

        val result = SyncResult(false)

        val googleLists = googleRepo.getLists()
        if (googleLists.isEmpty())
            return result

        val dbLists = listDb.getAll()

        // Pair up default list Ids on first sync
        val defaultList = dbLists.getDefault()!!
        if (defaultList.hasTempId) {
            val googleId = googleLists.first { it.default }.id
            listDb.updateId(defaultList.id, googleId)
            defaultList.id = googleId
        }

        // Group lists in pair of <Google, Local>
        val map1 = googleLists.associateBy { it.id }
        val map2 = dbLists.associateBy { it.id }
        val pairs = (map1.keys + map2.keys).associateWith { Pair(map1[it], map2[it]) }.values

        pairs.forEach { pair ->
            val googleList = pair.first
            val localList = pair.second

            if (googleList != null && localList != null) {
                if (localList.deleted) {
                    val tasks = taskDb.getAllByList(localList.id)
                    if (tasks.isEmpty()) {
                        if (googleRepo.deleteList(localList)) {
                            listDb.delete(localList)
                            result.toRemote.delete++
                        }
                    }
                }
                else if (localList.updated != googleList.updated) {
                    var newUpdated = googleList.updated
                    if (localList.title != googleList.title) {
                        if (localList.updated > googleList.updated) {
                            newUpdated = googleRepo.updateList(localList).updated
                            result.toRemote.change++
                        }
                        else {
                            localList.title = googleList.title
                            result.toLocal.change++
                        }
                    }

                    localList.updated = newUpdated
                    listDb.update(localList)
                }
            }
            else if (googleList != null) {
                //Does not exist locally, add it
                listDb.add(TaskList(googleList.id, googleList.title).apply {
                    updated = googleList.updated
                })

                result.toLocal.add++
            }
            else if (localList != null) {
                if (localList.hasTempId) {
                    val addedList = googleRepo.createList(localList)

                    // If current displayed list is this one update the ID so it reloads with this list active
                    val lastId = prefs.getString(Prefs.KEY_LAST_SELECTED_LIST_ID)
                    if (lastId != null && lastId.contentEquals(localList.id))
                        prefs.setString(Prefs.KEY_LAST_SELECTED_LIST_ID, addedList.id)

                    // TODO added list will have a different updated time, that should be set here too
                    listDb.updateId(localList.id, addedList.id)
                    result.toRemote.add++
                }
                else {
                    // TODO this will cascade delete so log tasks count its removing...
                    listDb.delete(localList)
                    result.toLocal.delete++
                }
            }
            else
                throw UnsupportedOperationException() // should never happen
        }

        Log.d(TAG, "Lists to Local : ${result.toLocal.totalChanges} ${result.toLocal}")
        Log.d(TAG, "Lists to Google: ${result.toRemote.totalChanges} ${result.toRemote}")
        prefs.setDate(Prefs.KEY_LAST_LIST_SYNC, Date())

        result.success = true
        return result
    }

    fun tasks(listId: String): SyncResult {
        if (listId.isEmpty())
            return SyncResult(true)

        val result = SyncResult(false)

        val list = listDb.getAll().first { it.id == listId }
        val updatedMin = if(list.lastSync.time == 0L) null else list.lastSync
        val hasFullList = updatedMin == null
        val googleTasks = googleRepo.getTasks(listId, updatedMin)
        val localTasks = taskDb.getAllByList(listId)

        val map1 = googleTasks.associateBy { it.id }
        val map2 = localTasks.associateBy { it.id }
        val pairs = (map1.keys + map2.keys).associateWith { Pair(map1[it], map2[it]) }.values

        pairs.forEach { pair ->
            val googleTask = pair.first
            val localTask = pair.second

            if (googleTask != null && localTask != null) {
                if (googleTask.deleted) {
                    taskDb.delete(googleTask)
                    result.toLocal.delete++
                }
                else if (localTask.deleted) {
                    if (googleRepo.deleteTask(localTask)) {
                        taskDb.delete(localTask)
                        result.toRemote.delete++
                    }
                }
                else if (googleTask.updated != localTask.updated) {
                    var updateLocalTask = localTask
                    if (!googleTask.equals(localTask)) {
                        if (localTask.updated > googleTask.updated) {
                            updateLocalTask.updated = googleRepo.updateTask(localTask).updated
                            result.toRemote.change++
                        }
                        else {
                            updateLocalTask = googleTask
                            result.toLocal.change++
                        }
                    }
                    else { // no change but update last modified time
                        updateLocalTask.updated = googleTask.updated
                        Log.d(TAG, "No changes, updating last modified time")
                    }

                    taskDb.update(updateLocalTask)
                }
            }
            else if (googleTask != null) {
                if (googleTask.deleted) {
                    // Nothing, task already does not exist locally
                }
                else {
                    taskDb.add(googleTask)
                    result.toLocal.add++
                }
            }
            else if (localTask != null) {
                if (localTask.hasTempId && localTask.deleted) {
                    taskDb.delete(localTask)
                }
                else if (localTask.hasTempId) {
                    googleRepo.createTask(localTask)?.let { updated ->
                        taskDb.updateId(localTask.listId, localTask.id, updated.id, updated.updated)
                        result.toRemote.add++
                    }
                }
                else if (localTask.deleted) {
                    if (!hasFullList) {
                        googleRepo.deleteTask(localTask)
                        result.toRemote.delete++
                    }

                    taskDb.delete(localTask)
                }
                else if (hasFullList) { // Not in google, so must be deletion
                        result.toLocal.delete++
                        taskDb.delete(localTask)
                }
                else if (localTask.updated > list.lastSync) {
                    localTask.updated = googleRepo.updateTask(localTask).updated
                    taskDb.update(localTask)
                    result.toRemote.change++
                }
            }
        }

        Log.d(TAG, "Tasks to Local : ${result.toLocal.totalChanges} ${result.toLocal}")
        Log.d(TAG, "Tasks to Google: ${result.toRemote.totalChanges} ${result.toRemote}")

        // Update last sync time
        list.lastSync = Date()
        listDb.update(list)

        result.success = true
        return result
    }

    companion object {
        fun getInstance(context: Context, sAuthKey: String): Sync {
            val db = AppDatabase.getInstance(context)
            val api = GoogleApi(sAuthKey)
            val repo = GoogleTasksRepository(api)

            return Sync(db!!.taskListDao(), db.taskDao(), repo, Prefs.getInstance(context))
        }
    }
}

data class SyncResult(var success: Boolean) {
    val toLocal = SyncChanges()
    val toRemote = SyncChanges()

    val totalChanges: Int
        get() = toLocal.totalChanges + toRemote.totalChanges
}

data class SyncChanges(var add: Int = 0, var change: Int = 0, var delete: Int = 0) {
    val totalChanges: Int
        get() = add + change + delete
}