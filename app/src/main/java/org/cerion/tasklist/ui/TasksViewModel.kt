package org.cerion.tasklist.ui

import android.content.Context
import android.databinding.ObservableArrayList
import android.databinding.ObservableField
import android.databinding.ObservableList
import android.text.format.DateUtils
import android.util.Log
import org.cerion.tasklist.data.Database
import org.cerion.tasklist.data.Prefs
import org.cerion.tasklist.data.Task
import org.cerion.tasklist.data.TaskList
import java.util.*

class TasksViewModel(private val context: Context) {

    private val TAG = TasksViewModel::class.qualifiedName
    private val prefs = Prefs.getInstance(context)
    private val db = Database.getInstance(context)

    val lists: ObservableList<TaskList> = ObservableArrayList()
    val tasks: ObservableList<Task> = ObservableArrayList()

    val currList = ObservableField<TaskList>()
    val lastSync = ObservableField("")
    val isOutOfSync = ObservableField(false)

    // TODO make private, should be based on actions that change it which only this class does
    fun refreshTasks() {
        updateLastSync() //Relative time so update it as much as possible

        val dbTasks = db.tasks.getList(currList.get()!!.id, false) //Get list with blank records excluded

        Collections.sort(dbTasks, Comparator { task, t1 ->
            if (task.deleted != t1.deleted)
                return@Comparator if (task.deleted) 1 else -1
            if (task.completed != t1.completed) if (task.completed) 1 else -1 else task.title.compareTo(t1.title, ignoreCase = true)
        })

        tasks.clear()
        tasks.addAll(dbTasks)
    }

    fun load() {
        Log.d(TAG, "load")
        updateLastSync() //Relative time so update it as much as possible

        val dbLists = getListsFromDatabase()

        //If the current list is not set, try to restore last saved
        if (currList.get() == null)
            currList.set(TaskList.get(dbLists, prefs.getString(Prefs.KEY_LAST_SELECTED_LIST_ID)))

        //If nothing valid is saved default to "all tasks" list
        if (currList.get() == null)
            currList.set(TaskList.ALL_TASKS)

        lists.clear()
        lists.addAll(dbLists)

        Collections.sort(lists, { taskList, t1 -> taskList.title.compareTo(t1.title, ignoreCase = true) })

        lists.add(0, TaskList.ALL_TASKS)

        refreshTasks()
    }

    fun clearCompleted() {
        Log.d(TAG, "onClearCompleted")
        db.tasks.clearCompleted(currList.get())

        refreshTasks()
    }

    private fun getListsFromDatabase(): List<TaskList> {
        var dbLists = db.taskLists.list
        if (dbLists.size == 0) {
            Log.d(TAG, "No lists, adding default")
            val defaultList = TaskList("Default")
            defaultList.bDefault = true
            db.taskLists.add(defaultList)
            dbLists = db.taskLists.list //re-get list
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

    fun getDefaultList(): TaskList? {
        return TaskList.getDefault(lists)
    }


}
