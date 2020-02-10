package org.cerion.tasklist.database

import androidx.room.*
import java.util.*

@Dao
interface TaskDao {

    //String orderBy = COLUMN_DELETED + " ASC, " + COLUMN_COMPLETE + " ASC, " + COLUMN_TITLE + " ASC";

    @Query("SELECT * FROM " + Task.TABLE_NAME + " WHERE listId=:listId AND id=:id")
    fun get(listId: String, id: String): Task

    @Query("SELECT * FROM " + Task.TABLE_NAME)
    fun getAll(): List<Task>

    @Query("SELECT * FROM " + Task.TABLE_NAME + " WHERE listId=:listId")
    fun getAllByList(listId: String): List<Task>

    @Insert
    fun add(task: Task)

    @Update
    fun update(task: Task)

    @Delete
    fun delete(task: Task)

    @Query("UPDATE " + Task.TABLE_NAME + " SET id=:newId, updated=:updated WHERE listId=:listId AND id=:oldId")
    fun updateId(listId: String, oldId: String, newId: String, updated: Date)
}

@Dao
interface TaskListDao {

    @Query("SELECT * FROM " + TaskList.TABLE_NAME)
    fun getAll(): List<TaskList>

    @Insert
    fun add(taskList: TaskList)

    @Update
    fun update(taskList: TaskList)

    @Delete
    fun delete(taskList: TaskList)

    @Query("UPDATE " + TaskList.TABLE_NAME + " SET id=:newId WHERE id=:oldId")
    fun updateId(oldId: String, newId: String)

    @Query("UPDATE " + TaskList.TABLE_NAME + " SET updated=:updated WHERE id=:id")
    fun setLastUpdated(id: String, updated: Date)
}