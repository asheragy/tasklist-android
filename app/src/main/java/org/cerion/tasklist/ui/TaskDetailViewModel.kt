package org.cerion.tasklist.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.cerion.tasklist.R
import org.cerion.tasklist.common.NonNullMutableLiveData
import org.cerion.tasklist.common.ResourceProvider
import org.cerion.tasklist.common.TAG
import org.cerion.tasklist.database.Task
import org.cerion.tasklist.database.TaskDao
import java.text.SimpleDateFormat
import java.util.*


class TaskDetailViewModel(private val resources: ResourceProvider, private val db: TaskDao) : ViewModel() {

    private lateinit var originalTask: Task

    val _task = MutableLiveData<TaskModel>()
    val task: LiveData<TaskModel>
        get() = _task

    val _windowTitle = MutableLiveData<String>()
    val windowTitle: LiveData<String>
        get() = _windowTitle

    val _modified = MutableLiveData<String>()
    val modified: LiveData<String>
        get() = _modified

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun addTask(taskListId: String) {
        _windowTitle.value = "Add new task"
        val task = Task(taskListId)
        loadTaskFields(task)
    }

    fun setTask(listId: String, id: String) {
        _windowTitle.value = "Edit task"
        val task = db.get(listId, id)
        loadTaskFields(task)
    }

    private fun loadTaskFields(task: Task) {
        originalTask = task
        _task.value = TaskModel(task, resources)
        _modified.value = task.updated.toString()
    }

    fun save(): Boolean {
        task.value!!.let { task ->
            if (task.isModified) {
                if (task.isNew)
                    db.add(task.modifiedTask)
                else
                    db.update(task.modifiedTask)

                return true
            }

            Log.i(TAG, "Ignoring save, no changes")
            return false
        }
    }

    fun removeDueDate() {
        task.value!!.dueDate.value = Date(0)
    }

    class TaskModel(val task: Task, private val resources: ResourceProvider) {

        var title = task.title
        var notes = task.notes
        var completed = task.completed
        val dueDate = NonNullMutableLiveData(task.due)

        val dueString: LiveData<String>
            get() = Transformations.map(dueDate) { date ->
                if(date.time != 0L)
                    dateFormat.format(date)
                else
                    resources.getString(R.string.no_due_date)
            }

        val hasDueDate: LiveData<Boolean>
            get() = Transformations.map(this.dueDate) { value -> value.time > 0}

        val isModified: Boolean
            get() = title != task.title || notes != task.notes || completed != task.completed || dueDate.value != task.due

        val modifiedTask: Task
            get() {
                return task.also { updated ->
                    updated.setModified()
                    updated.title = title
                    updated.notes = notes
                    updated.completed = completed
                    updated.due = dueDate.value
                }
            }

        val isNew: Boolean
            get() = task.hasTempId
    }

    companion object {
        private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    }
}



