package org.cerion.tasklist.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.DateFormat
import java.util.*

@Entity(tableName = TaskList.TABLE_NAME)
data class TaskList(@PrimaryKey var id: String, var title: String) {

    var updated: Date = Date(0)
    @ColumnInfo(name = "updated_tasks")
    var lastSync: Date = Date(0)
    var isDefault: Boolean = false
    var deleted: Boolean = false

    val isAllTasks: Boolean
        get() = id.isEmpty()

    val hasTempId: Boolean
        get() = id.startsWith("temp_")

    override fun toString(): String = title

    fun logString(dateFormat: DateFormat?): String {
        var result = String.format("List(id=%s, updated='%s', lastSync='%s', title='%s'",
                id,
                if (dateFormat != null) dateFormat.format(updated) else updated.toString(),
                if (dateFormat != null) dateFormat.format(lastSync) else lastSync.toString(),
                title)

        if (isDefault)
            result += ", **default"
        if (deleted)
            result += ", **deleted"

        result += ")"

        return result
    }

    companion object {
        internal const val TABLE_NAME = "tasklists"

        // Special list to represent a list containing tasks from all available lists
        val ALL_TASKS = TaskList("", "All Tasks")
    }
}

fun List<TaskList>.getById(id: String): TaskList? = this.firstOrNull { list -> !list.isAllTasks && list.id.contentEquals(id) }
fun List<TaskList>.getDefault(): TaskList? = this.first { list -> list.isDefault }
