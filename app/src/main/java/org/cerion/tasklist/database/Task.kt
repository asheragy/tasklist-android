package org.cerion.tasklist.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.text.DateFormat
import java.util.*

@Entity(tableName = Task.TABLE_NAME,
        indices = [Index("listId")],
        primaryKeys = ["id", "listId"],
        foreignKeys = [ForeignKey(
                entity = TaskList::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("listId"),
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE)])
data class Task(var listId: String) {

    var id: String = AppDatabase.generateTempId()
    var title: String = ""
    var notes: String = ""
    var due: Date = Date(0)
    var updated: Date = Date(0)
    var completed: Boolean = false
    var deleted: Boolean = false

    val hasDueDate: Boolean get() = due.time > 0
    val hasTempId: Boolean get() = id.startsWith("temp_")

    fun setModified() {
        updated = Date()
    }

    fun logString(dateFormat: DateFormat): String {
        var result = String.format("Task(id=%s, updated='%s', title='%s'", id, dateFormat.format(updated), title)
        if (notes.isNotEmpty())
            result += ", note_length=" + notes.length
        if (hasDueDate)
            result += ", due=" + dateFormat.format(updated)
        if (completed)
            result += ", *completed"
        if (deleted)
            result += ", *deleted"

        result += ")"

        return result
    }

    companion object {
        internal const val TABLE_NAME = "tasks"
    }
}

fun List<Task>.getById(id: String): Task? = this.firstOrNull { task -> task.id == id }