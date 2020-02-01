package org.cerion.tasklist.googleapi

import org.cerion.tasklist.BuildConfig
import org.cerion.tasklist.database.Task
import org.cerion.tasklist.database.TaskList
import java.util.*

//Error from tasks API, json encoded with error code and message
class GoogleApiException(message: String, val errorCode: Int) : Exception("Error $errorCode: $message")

interface GoogleTasklistsApi {
    @Throws(GoogleApiException::class)
    fun get(id: String): TaskList?

    @Throws(GoogleApiException::class)
    fun getAll(): List<TaskList>

    @Throws(GoogleApiException::class)
    fun insert(list: TaskList): TaskList?

    @Throws(GoogleApiException::class)
    fun update(list: TaskList): Boolean

    fun delete(list: TaskList): Boolean
}

interface GoogleTasksApi {
    fun delete(task: Task): Boolean

    @Throws(GoogleApiException::class)
    fun list(listId: String, dtUpdatedMin: Date?): List<Task>?

    @Throws(GoogleApiException::class)
    fun insert(task: Task): Task?

    @Throws(GoogleApiException::class)
    fun update(task: Task): Boolean
}

class GoogleApi(private val mAuthKey: String) {

    val taskListsApi: GoogleTasklistsApi
        get() = GoogleTasklistsApi_Impl(mAuthKey)

    val tasksApi: GoogleTasksApi
        get() = GoogleTasksApi_Impl(mAuthKey)

    companion object {
        internal const val API_KEY = BuildConfig.GOOGLE_TASKS_APIKEY
        internal const val mLogFullResults = true
        internal const val TASKS_BASE_URL = "https://www.googleapis.com/tasks/v1/"
    }
}
