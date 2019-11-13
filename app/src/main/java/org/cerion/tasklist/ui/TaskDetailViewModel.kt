package org.cerion.tasklist.ui

import androidx.lifecycle.*
import org.cerion.tasklist.R
import org.cerion.tasklist.common.ResourceProvider
import org.cerion.tasklist.data.Task
import org.cerion.tasklist.data.TaskDao
import java.text.SimpleDateFormat
import java.util.*


val TAG = TaskDetailViewModel::class.simpleName

class TaskDetailViewModel(private val resources: ResourceProvider, private val db: TaskDao) : ViewModel() {

    private lateinit var task: Task

    private val isNew
        get() = task.hasTempId

    val title = MutableLiveData<String>()
    val notes = MutableLiveData<String>()
    val completed = MutableLiveData<Boolean>()
    val dueDate = MutableLiveData<Date>()
    val isDirty = MediatorLiveData<Boolean>()

    val dueString: LiveData<String>
        get() = Transformations.map(dueDate) { date ->
            if(date.time != 0L)
                dateFormat.format(date)
            else
                resources.getString(R.string.no_due_date)
        }

    val hasDueDate: LiveData<Boolean>
        get() = Transformations.map(this.dueDate) { value -> value.time > 0}

    val _windowTitle = MutableLiveData<String>()
    val windowTitle: LiveData<String>
        get() = _windowTitle

    val _modified = MutableLiveData<String>()
    val modified: LiveData<String>
        get() = _modified

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        isDirty.value = false
        isDirty.addSource(title) { value -> if(!isDirty.value!! && value != task.title) isDirty.value = true }
        isDirty.addSource(notes) { value -> if(!isDirty.value!! && value != task.notes) isDirty.value = true }
        isDirty.addSource(completed) { value -> if(!isDirty.value!! && value != task.completed) isDirty.value = true }
        isDirty.addSource(dueDate) { value -> if(!isDirty.value!! && value != task.due) isDirty.value = true }
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
        isDirty.value = false
        this.task = task

        title.value = task.title
        notes.value = task.notes
        completed.value = task.completed
        dueDate.value = task.due

        // TODO is it ever this value?
        if (task.updated.time > 0)
            _modified.value = task.updated.toString()
        else
            _modified.value = ""
    }

    fun save() {
        task.setModified()
        task.title = title.value!!
        task.notes = notes.value!!
        task.completed = completed.value!!
        task.due = dueDate.value!!

        if (isNew)
            db.add(task)
        else
            db.update(task)
    }

    fun removeDueDate() {
        dueDate.value = Date(0)
    }

    companion object {
        private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    }
}
