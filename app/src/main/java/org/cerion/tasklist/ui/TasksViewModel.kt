package org.cerion.tasklist.ui

import android.app.Application
import android.text.format.DateUtils
import android.util.Log
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.cerion.tasklist.R
import org.cerion.tasklist.common.ResourceProvider
import org.cerion.tasklist.common.SingleLiveEvent
import org.cerion.tasklist.common.TAG
import org.cerion.tasklist.data.*
import org.cerion.tasklist.sync.Sync
import java.util.*

class TasksViewModel(private val resources: ResourceProvider,
                     private val prefs: Prefs,
                     private val db: AppDatabase,
                     private val taskDao: TaskDao,
                     private val listDao: TaskListDao,
                     application: Application) : AndroidViewModel(application) {

    private val job = Job()
    private val uiScope =  CoroutineScope(Dispatchers.Main + job)

    val lists: ObservableList<TaskList> = ObservableArrayList()
    val tasks: ObservableList<Task> = ObservableArrayList()
    val message: SingleLiveEvent<String> = SingleLiveEvent()

    val hasLocalChanges: ObservableField<Boolean> = ObservableField()

    private val _selectedList = MutableLiveData<TaskList>(TaskList.ALL_TASKS)
    val selectedList: LiveData<TaskList>
        get() = _selectedList
    private val currList get() = selectedList.value!!

    private val _lastSync = MutableLiveData<String>("")
    val lastSync: LiveData<String>
        get() = _lastSync

    private val _isOutOfSync = MutableLiveData<Boolean>(false) // TODO consider merging with hasLocalChanges and true=sync now
    val isOutOfSync : LiveData<Boolean>
        get() = _isOutOfSync

    private val _syncing = MutableLiveData<Boolean>(false)
    val syncing: LiveData<Boolean>
        get() = _syncing

    val defaultList get() = TaskList.getDefault(lists)!!

    init {
        load()
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    private val handler = CoroutineExceptionHandler { _, throwable ->
        Log.e("Exception", ":$throwable")
    }

    fun sync(token: String) {
        if (_syncing.value!!)
            return

        _syncing.value = true
        uiScope.launch(handler) {
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
            } else {
                message.value = if(error.isNullOrBlank()) "Sync Failed, unknown error" else error
                //val dialog = AlertDialogFragment.newInstance("Sync failed", message)
                //dialog.show(requireFragmentManager(), "dialog")
            }

            load() //refresh since data may have changed
        }
    }

    suspend fun syncInBackground(token: String): Boolean {
        val sync = Sync.getInstance(getApplication(), token)
        return withContext(Dispatchers.IO) {
            sync.run()
        }
    }

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
        _selectedList.value = (lastSaved ?: TaskList.ALL_TASKS) //If nothing valid is saved default to "all tasks" list

        lists.clear()
        lists.addAll(dbLists)

        refreshTasks()
    }

    fun clearCompleted() {
        Log.d(TAG, "onClearCompleted")

        // TODO should be able to use current list and not have to get from database
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

    fun moveTaskToList(task: Task, newList: TaskList) {
        if (newList.id == task.listId) {
            Log.i(TAG, "Ignoring moving since same list")
            return
        }

        // Delete task, add to new list and refresh
        // TODO if temp ID permanently delete
        task.setModified()
        task.deleted = true
        taskDao.update(task)

        task.deleted = false
        task.listId = newList.id
        task.id = AppDatabase.generateTempId()
        taskDao.add(task)

        setList(newList)
    }

    fun moveLeft() {
        val index = lists.indexOf(currList)
        setList(lists[(index + 1) % lists.size])
    }

    fun moveRight() {
        val index = lists.indexOf(currList)
        setList(lists[(index - 1 + lists.size) % lists.size])
    }

    fun logDatabase() {
        db.log()
    }

    fun deleteCurrentList() {
        // Only take action if list is empty
        if (taskDao.getAllbyList(currList.id).isEmpty()) {
            if (currList.hasTempId) {
                listDao.delete(currList)
                message.value = resources.getString(R.string.message_deleted_list, currList.title)
                load()
            }
            else {
                currList.deleted = true
                listDao.update(currList)
                hasLocalChanges.set(true) // triggers a sync
            }
        }
        else {
            message.value = resources.getString(R.string.warning_delete_nonEmpty_list)
        }

        db.log()
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
        var lastSyncText = "Last Sync: "
        val lastSyncTime = prefs.getDate(Prefs.KEY_LAST_SYNC)
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
