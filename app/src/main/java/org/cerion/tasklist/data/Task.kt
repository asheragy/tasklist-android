package org.cerion.tasklist.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
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
class Task(var listId: String, var id: String, var title: String, var notes: String, var due: Date, var updated: Date, var completed: Boolean, var deleted: Boolean) {

    // val isBlank: Boolean get() = title.isEmpty() && notes.isEmpty() && !hasDueDate
    val hasDueDate: Boolean get() = due.time > 0
    val hasTempId: Boolean get() = id.startsWith("temp_")

    @Ignore
    @JvmOverloads
    constructor(listId: String, id: String = AppDatabase.generateTempId()) : this(listId, id, "", "", Date(0), Date(0), false, false)

    override fun toString(): String = title + if (deleted) " (Deleted)" else ""

    fun setModified() {
        updated = Date()
    }

    fun logString(dateFormat: DateFormat): String {
        var result = String.format("Task(id=%s, updated='%s', title='%s'", id, dateFormat.format(updated), title)
        if (!notes.isEmpty())
            result += ", notes=" + notes.length
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
