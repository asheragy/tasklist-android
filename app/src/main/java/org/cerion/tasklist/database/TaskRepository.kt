package org.cerion.tasklist.database

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import org.cerion.tasklist.common.TAG
import java.util.*
import kotlin.Comparator

class TaskRepository(private val taskDao: TaskDao) {


    fun getTasksForList(list: TaskList): LiveData<List<Task>> {
        val dbTasks = if (list.isAllTasks)
            taskDao.getAllAsync()
        else
            taskDao.getAllByListAsync(list.id)

        return dbTasks.switchMap { tasks ->
            liveData {
                Collections.sort(tasks, Comparator { task, t1 ->
                    if (task.deleted != t1.deleted)
                        return@Comparator if (task.deleted) 1 else -1
                    if (task.completed != t1.completed) if (task.completed) 1 else -1 else task.title.compareTo(t1.title, ignoreCase = true)
                })
                emit(tasks)
            }
        }
    }

    fun clearCompleted(tasks: List<Task>) {
        Log.i(TAG, "onClearCompleted")
        val completedTasks = tasks.filter { it.completed && !it.deleted }

        completedTasks?.forEach { task ->
            if (task.hasTempId)
                taskDao.delete(task)
            else {
                task.setModified()
                task.deleted = true
                taskDao.update(task)
            }
        }
    }

    fun toggleCompleted(task: Task) {
        task.setModified()
        task.completed = !task.completed
        taskDao.update(task)
    }

    fun delete(task: Task) {
        if (task.hasTempId)
            taskDao.delete(task)
        else {
            task.setModified()
            task.deleted = true
            taskDao.update(task)
        }
    }

    fun undoDelete(task: Task) {
        if (task.hasTempId)
            taskDao.add(task)
        else {
            task.setModified()
            task.deleted = false
            taskDao.update(task)
        }
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
    }

}