package org.cerion.tasklist.data

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

        internal const val API_KEY = "346378052412-b0lrj3jgnucf299u3qf23c4sh4agdgsk.apps.googleusercontent.com"
        //private static final String API_KEY = "AIzaSyAHgDGorXJ1I0WqzpWE6Pm2o894T-j4pdQ";
        internal const val mLogFullResults = true
        internal const val TASKS_BASE_URL = "https://www.googleapis.com/tasks/v1/"
    }
}
