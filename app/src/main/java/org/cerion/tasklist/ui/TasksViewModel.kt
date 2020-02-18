package org.cerion.tasklist.ui

import android.app.Application
import android.text.format.DateUtils
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cerion.tasklist.R
import org.cerion.tasklist.common.*
import org.cerion.tasklist.database.*
import org.cerion.tasklist.sync.Sync
import java.util.*

class TasksViewModel(private val resources: ResourceProvider,
                     private val prefs: Prefs,
                     private val db: AppDatabase,
                     private val taskRepo: TaskRepository,
                     private val listDao: TaskListDao,
                     application: Application) : AndroidViewModel(application) {

    private val _lists = NonNullMutableLiveData<List<TaskList>>(emptyList())
    val lists: NonNullLiveData<List<TaskList>>
        get() = _lists

    val message: SingleLiveEvent<String> = SingleLiveEvent<String>()

    val hasLocalChanges: ObservableField<Boolean> = ObservableField()

    private val currList get() = selectedList.value!!
    private val _selectedList = MutableLiveData<TaskList>(TaskList.ALL_TASKS)
    val selectedList: LiveData<TaskList>
        get() = _selectedList

    private val _lastSync = MutableLiveData<String>("")
    val lastSync: LiveData<String>
        get() = _lastSync

    private val _isOutOfSync = MutableLiveData<Boolean>(false) // TODO consider merging with hasLocalChanges and true=sync now
    val isOutOfSync : LiveData<Boolean>
        get() = _isOutOfSync

    private val _syncing = MutableLiveData<Boolean>(false)
    val syncing: LiveData<Boolean>
        get() = _syncing

    private val _deletedTask = MutableLiveData<Task>()
    val deletedTask: LiveData<Task>
        get() = _deletedTask

    val tasks: LiveData<List<Task>> = selectedList.switchMap { taskRepo.getTasksForList(it) }

    val defaultList get() = lists.value.getDefault()!!

    init {
        load()
    }

    private val handler = CoroutineExceptionHandler { _, throwable ->
        Log.e("Exception", ":$throwable")
    }

    fun sync(token: String) {
        if (_syncing.value!!)
            return

        _syncing.value = true
        viewModelScope.launch(handler) {
            var success = false
            var error: String? = null

            try {
                success = syncInBackground(token)
            }
            catch (e: Exception) {
                error = e.message
            }

            _syncing.value = false
            if (success) {
                updateLastSync() //Update last sync time only if successful
                hasLocalChanges.set(false)
            }
            else
                message.value = if(error.isNullOrBlank()) "Sync Failed, unknown error" else error

            load() //refresh since data may have changed
        }
    }

    private suspend fun syncInBackground(token: String): Boolean {
        val sync = Sync.getInstance(getApplication(), token)
        return withContext(Dispatchers.IO) {
            if(sync.lists().success)
                sync.tasks(currList.id).success
            else
                false
        }
    }

    fun setList(list: TaskList) {
        prefs.setString(Prefs.KEY_LAST_SELECTED_LIST_ID, list.id)
        load()
    }

    private fun load() {
        Log.d(TAG, "load")
        //db.log()

        val dbLists = listDao.getAll().sortedBy { it.title.toLowerCase() }.toMutableList()
        dbLists.add(0, TaskList.ALL_TASKS.apply {
            lastSync = prefs.getDate(Prefs.KEY_LAST_LIST_SYNC)
        })

        val lastId = prefs.getString(Prefs.KEY_LAST_SELECTED_LIST_ID)
        val lastSaved = dbLists.firstOrNull { it.id == lastId }
        _selectedList.value = (lastSaved ?: TaskList.ALL_TASKS) //If nothing valid is saved default to "all tasks" list

        _lists.value = dbLists

        updateLastSync()
    }

    fun clearCompleted() = taskRepo.clearCompleted(tasks.value!!)
    fun toggleCompleted(task: Task) = taskRepo.toggleCompleted(task)
    fun undoDelete(task: Task) = taskRepo.undoDelete(task)

    fun toggleDeleted(task: Task) {
        if (task.deleted)
            undoDelete(task)
        else
            delete(task)
    }

    fun delete(task: Task) {
        _deletedTask.value = task
        taskRepo.delete(task)
    }

    fun deleteConfirmed() {
        _deletedTask.value = null
    }

    fun moveTaskToList(task: Task, newList: TaskList) {
        if (newList.id != task.listId) {
            taskRepo.moveTaskToList(task, newList)
            setList(newList)
        }
    }

    fun moveLeft() {
        val index = lists.value.indexOf(currList)
        setList(lists.value[(index + 1) % lists.value.size])
    }

    fun moveRight() {
        val index = lists.value.indexOf(currList)
        setList(lists.value[(index - 1 + lists.value.size) % lists.value.size])
    }

    fun logDatabase() {
        db.log()
    }

    fun deleteCurrentList() {
        if (tasks.value == null) // List not loaded yet
            return

        // Only take action if list is empty
        if (currList.isAllTasks || currList.isDefault)
            message.value = resources.getString(R.string.warning_delete_system_list)
        else if (tasks.value!!.isNotEmpty())
            message.value = resources.getString(R.string.warning_delete_nonEmpty_list)
        else if (currList.hasTempId) {
            // If never synced just delete
            listDao.delete(currList)
            message.value = resources.getString(R.string.message_deleted_list, currList.title)
            load()
        }
        else {
            // Set deleted and sync
            currList.deleted = true
            listDao.update(currList)
            hasLocalChanges.set(true) // triggers a sync
        }
    }

    private fun updateLastSync() {
        var lastSyncText = "Last Sync: "
        val lastSyncTime = currList.lastSync

        if (lastSyncTime.time == 0L)
            lastSyncText += "Never"
        else {
            val now = Date().time

            lastSyncText +=
                    if (now - lastSyncTime.time < 60 * 1000)
                        "Less than 1 minute ago"
                    else
                        DateUtils.getRelativeTimeSpanString(lastSyncTime.time, now, DateUtils.SECOND_IN_MILLIS).toString()

            _isOutOfSync.value = (now - lastSyncTime.time > 24 * 60 * 60 * 1000)
        }

        _lastSync.value = lastSyncText
    }
}
