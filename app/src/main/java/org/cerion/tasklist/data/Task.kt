package org.cerion.tasklist.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import java.util.*

@Entity(tableName = Task.TABLE_NAME,
        primaryKeys = arrayOf("id", "listId"),
        foreignKeys = arrayOf(ForeignKey(
                entity = TaskList::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("listId"),
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE)))
class Task(var listId: String, var id: String, var title: String, var notes: String, var due: Date, var updated: Date, var completed: Boolean, var deleted: Boolean) {

    val isBlank: Boolean get() = title.length == 0 && notes.length == 0 && due.time == 0L
    val hasDueDate: Boolean get() = due.time > 0
    val hasTempId: Boolean get() = id.startsWith("temp_")

    @Ignore
    @JvmOverloads
    constructor(listId: String, id: String = AppDatabase.generateTempId()) : this(listId, id, "", "", Date(0), Date(0), false, false)

    override fun toString(): String = title + if (deleted) " (Deleted)" else ""

    fun setModified() = {
        updated = Date()
    }

    companion object {
        internal const val TABLE_NAME = "tasks"
    }
}
