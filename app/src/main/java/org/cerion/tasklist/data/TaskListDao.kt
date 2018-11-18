package org.cerion.tasklist.data

import androidx.room.*
import java.util.*

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

    @Query("UPDATE " + TaskList.TABLE_NAME + " SET id = :newId WHERE id = :oldId")
    fun updateId(oldId: String, newId: String)

    @Query("UPDATE " + TaskList.TABLE_NAME + " SET updated = :updated WHERE id = :id")
    fun setLastUpdated(id: String, updated: Date)
}
