package org.cerion.tasklist.googleapi

import org.cerion.tasklist.database.Task
import org.cerion.tasklist.database.TaskList
import java.util.*

class GoogleTasksRepository(private val api: GoogleApi) {

    private val lists = api.taskListsApi
    private val tasks = api.tasksApi

    fun getLists(): List<GoogleTaskList> = lists.getAll()
    fun getList(id: String): GoogleTaskList? = lists.get(id)
    fun createList(list: TaskList): GoogleTaskList {
        val googleList = list.toGoogleList()
        return lists.insert(googleList)
    }

    fun updateList(list: TaskList): GoogleTaskList = lists.update(list.toGoogleList())
    fun deleteList(list: TaskList): Boolean = lists.delete(list.id)

    fun getTasks(listId: String, dtUpdatedMin: Date? = null): List<Task> = tasks.list(listId, dtUpdatedMin)
    fun createTask(task: Task): Task? = tasks.insert(task)
    fun updateTask(task: Task): Task = tasks.update(task)
    fun deleteTask(task: Task): Boolean = tasks.delete(task)
}

fun TaskList.toGoogleList() = GoogleTaskList(id, title)
/*
fun GoogleTaskList.toTaskList() = TaskList(id, title).also {
    it.updated = updated
    it.isDefault = default
}

 */