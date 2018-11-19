package org.cerion.tasklist.ui

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableList
import org.cerion.tasklist.data.AppDatabase
import org.cerion.tasklist.data.Prefs
import org.cerion.tasklist.data.Task
import org.cerion.tasklist.data.TaskList
import java.util.*

class TasksViewModel(context: Context) {

    private val TAG = TasksViewModel::class.qualifiedName
    private val prefs = Prefs.getInstance(context)
    private val db = AppDatabase.getInstance(context)
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
        // TODO reload tasks here the list may have just been added via dialog
        currList = list
        refreshTasks()
    }

    fun load() {
        Log.d(TAG, "load")
        db.log()
        updateLastSync() //Relative time so update it as much as possible

        val dbLists = getListsFromDatabase().sortedBy { it.title }.toMutableList()
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

        val tasks = taskDao.getAllbyList(currList.id).filter { it.completed && !it.deleted }
        for (task in tasks) {
            task.setModified()
            task.deleted = true
            taskDao.update(task)
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
        task.setModified()
        task.deleted = !task.deleted
        taskDao.update(task)
        refreshTasks()
    }

    fun logDatabase() {
        //db.log()
        //prefs.log()
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
            if (now - lastSyncTime.time < 60 * 1000)
                sText += "Less than 1 minute ago"
            else
                sText += DateUtils.getRelativeTimeSpanString(lastSyncTime.time, now, DateUtils.SECOND_IN_MILLIS).toString()

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


}
