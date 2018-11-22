package org.cerion.tasklist.ui

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableList
import org.cerion.tasklist.data.AppDatabase
import org.cerion.tasklist.data.Prefs
import org.cerion.tasklist.data.Task
import org.cerion.tasklist.data.TaskList
import java.util.*

class TasksViewModel(var context: Context) {

    private val prefs = Prefs.getInstance(context)
    private val db = AppDatabase.getInstance(context)!!
    private val taskDao = db.taskDao()
    private val listDao = db.taskListDao()

    val lists: ObservableList<TaskList> = ObservableArrayList()
    val tasks: ObservableList<Task> = ObservableArrayList()

    var currList: TaskList = TaskList.ALL_TASKS
        private set(value) {
            field = value
        }

    val lastSync = ObservableField("")
    val isOutOfSync = ObservableField(false)

    // TODO make private, should be based on actions that change it which only this class does
    fun refreshTasks() {
        updateLastSync() //Relative time so update it as much as possible

        // TODO filter out blank records here
        val dbTasks =
                if(currList.isAllTasks)
                    taskDao.getAll()
                else
                    taskDao.getAllbyList(currList.id)

        Collections.sort(dbTasks, Comparator { task, t1 ->
            if (task.deleted != t1.deleted)
                return@Comparator if (task.deleted) 1 else -1
            if (task.completed != t1.completed) if (task.completed) 1 else -1 else task.title.compareTo(t1.title, ignoreCase = true)
        })

        tasks.clear()
        tasks.addAll(dbTasks)
    }

    fun setList(list: TaskList) {
        prefs.setString(Prefs.KEY_LAST_SELECTED_LIST_ID, list.id)
        load()
    }

    fun load() {
        Log.d(TAG, "load")
        db.log()
        updateLastSync() //Relative time so update it as much as possible

        val dbLists = getListsFromDatabase().sortedBy { it.title.toLowerCase() }.toMutableList()
        dbLists.add(0, TaskList.ALL_TASKS)

        val lastId = prefs.getString(Prefs.KEY_LAST_SELECTED_LIST_ID)
        val lastSaved = dbLists.firstOrNull { it.id == lastId }
        currList = lastSaved ?: TaskList.ALL_TASKS //If nothing valid is saved default to "all tasks" list

        lists.clear()
        lists.addAll(dbLists)

        refreshTasks()
    }

    fun clearCompleted() {
        Log.d(TAG, "onClearCompleted")

        val tasks = (if(currList.isAllTasks) taskDao.getAll() else taskDao.getAllbyList(currList.id))
                .filter { it.completed && !it.deleted }

        for (task in tasks) {
            if (task.hasTempId)
                taskDao.delete(task)
            else {
                task.setModified()
                task.deleted = true
                taskDao.update(task)
            }
        }

        refreshTasks()
    }

    fun toggleCompleted(task: Task) {
        task.setModified()
        task.completed = !task.completed
        taskDao.update(task)
        refreshTasks()
    }

    fun toggleDeleted(task: Task) {
        if (task.hasTempId && !task.deleted) {
            taskDao.delete(task)
        }
        else {
            task.setModified()
            task.deleted = !task.deleted
            taskDao.update(task)
        }

        refreshTasks()
    }

    fun logDatabase() {
        db.log()
    }

    fun deleteCurrentList() {
        if (currList.hasTempId &&  taskDao.getAllbyList(currList.id).isEmpty()) {
            listDao.delete(currList)
            Toast.makeText(context, "Deleted list " + currList.title, Toast.LENGTH_SHORT).show()
            load()
        }
        else
            Toast.makeText(context, "List must be empty and not synced", Toast.LENGTH_SHORT).show()
    }

    private fun getListsFromDatabase(): List<TaskList> {
        var dbLists = listDao.getAll()
        if (dbLists.isEmpty()) {
            Log.d(TAG, "No lists, adding default")
            val defaultList = TaskList("Default")
            defaultList.isDefault = true
            listDao.add(defaultList)
            dbLists = listDao.getAll() //re-get list
        }

        return dbLists
    }

    fun updateLastSync() {
        var sText = "Last Sync: "
        val lastSyncTime = prefs.getDate(Prefs.KEY_LAST_SYNC)
        if (lastSyncTime == null || lastSyncTime.time == 0L)
            sText += "Never"
        else {
            val now = Date().time

            sText +=
                    if (now - lastSyncTime.time < 60 * 1000)
                        "Less than 1 minute ago"
                    else
                        DateUtils.getRelativeTimeSpanString(lastSyncTime.time, now, DateUtils.SECOND_IN_MILLIS).toString()

            if (now - lastSyncTime.time > 24 * 60 * 60 * 1000)
                isOutOfSync.set(true)
            else
                isOutOfSync.set(false)
        }

        lastSync.set(sText)
    }

    fun getDefaultList(): TaskList {
        return TaskList.getDefault(lists)!!
    }

    companion object {
        private val TAG = TasksViewModel::class.qualifiedName
    }

}
