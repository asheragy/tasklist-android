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
import org.cerion.tasklist.common.ResourceProvider
import org.cerion.tasklist.common.SingleLiveEvent
import org.cerion.tasklist.database.*
import org.cerion.tasklist.sync.Sync
import java.util.*

class TasksViewModel(private val resources: ResourceProvider,
                     private val prefs: Prefs,
                     private val db: AppDatabase,
                     private val taskRepo: TaskRepository,
                     private val listDao: TaskListDao,
                     application: Application) : AndroidViewModel(application) {

    val lists: LiveData<List<TaskList>> = listDao.getAllAsync().map {
        val lists = it.sortedBy { list -> list.title.toLowerCase() }.toMutableList()
        lists.add(0, TaskList.ALL_TASKS.apply {
            lastSync = prefs.getDate(Prefs.KEY_LAST_LIST_SYNC)
        })

        lists
    }

    val message: SingleLiveEvent<String> = SingleLiveEvent<String>()

    val hasLocalChanges: ObservableField<Boolean> = ObservableField()

    private val currList get() = selectedList.value!!
    val selectedList = MediatorLiveData<TaskList>()

    private val _lastSync = MediatorLiveData<String>()
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

    val defaultList get() = lists.value!!.getDefault()!!

    init {
        selectedList.addSource(lists) { lists ->
            val lastId = prefs.getString(Prefs.KEY_LAST_SELECTED_LIST_ID)
            val lastSaved = lists.firstOrNull { it.id == lastId }
            selectedList.value = (lastSaved ?: TaskList.ALL_TASKS) //If nothing valid is saved default to "all tasks" list
        }

        selectedList.addSource(selectedList) {
            prefs.setString(Prefs.KEY_LAST_SELECTED_LIST_ID, it.id)
        }

        _lastSync.addSource(selectedList) {
            setLastSyncText(it.lastSync)
        }
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
                hasLocalChanges.set(false)
                // Manually update since dao w/livedata does not handle this list
                if (currList.isAllTasks) {
                    currList.lastSync = prefs.getDate(Prefs.KEY_LAST_LIST_SYNC)
                    setLastSyncText(currList.lastSync)
                }
            }
            else
                message.value = if(error.isNullOrBlank()) "Sync Failed, unknown error" else error
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
            selectedList.value = newList
        }
    }

    fun moveLeft() {
        lists.value?.run {
            selectedList.value = this[(indexOf(currList) + 1) % size]
        }
    }

    fun moveRight() {
        lists.value?.run {
            selectedList.value = this[(indexOf(currList) - 1 + size) % size]
        }
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
        }
        else {
            // Set deleted and sync
            currList.deleted = true
            listDao.update(currList)
            hasLocalChanges.set(true) // triggers a sync
        }
    }

    private fun setLastSyncText(lastSync: Date) {
        var lastSyncText = "Last Sync: "

        if (lastSync.time == 0L)
            lastSyncText += "Never"
        else {
            val now = Date().time

            lastSyncText +=
                    if (now - lastSync.time < 60 * 1000)
                        "Less than 1 minute ago"
                    else
                        DateUtils.getRelativeTimeSpanString(lastSync.time, now, DateUtils.SECOND_IN_MILLIS).toString()

            _isOutOfSync.value = (now - lastSync.time > 24 * 60 * 60 * 1000)
        }

        _lastSync.value = lastSyncText
    }
}
