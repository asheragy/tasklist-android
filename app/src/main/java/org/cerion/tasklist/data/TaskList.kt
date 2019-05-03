package org.cerion.tasklist.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.text.DateFormat
import java.util.*

@Entity(tableName = TaskList.TABLE_NAME)
class TaskList(@field:PrimaryKey var id: String, var title: String, var updated: Date, var isRenamed: Boolean, var isDefault: Boolean, var deleted: Boolean) {

    val isAllTasks: Boolean
        get() = id.isEmpty()

    val hasTempId: Boolean
        get() = id.startsWith("temp_")

    @Ignore
    constructor(title: String) : this(AppDatabase.generateTempId(), title)

    @Ignore
    constructor(id: String, title: String) : this(id, title, Date(0), false, false, false)

    override fun toString(): String = title

    fun logString(dateFormat: DateFormat?): String {
        var result = String.format("List(id=%s, updated='%s', title='%s'", id, if (dateFormat != null) dateFormat.format(updated) else updated.toString(), title)
        if (isDefault)
            result += ", **default"
        if (isRenamed)
            result += ", **renamed"
        if (deleted)
            result += ", **deleted"
        result += ")"

        return result
    }

    companion object {
        internal const val TABLE_NAME = "tasklists"

        // Special list to represent a list containing tasks from all available lists
        val ALL_TASKS = TaskList("", "All Tasks")

        // TODO remove this and just use kotlin lambdas to get the list we want
        operator fun get(lists: List<TaskList>, sId: String): TaskList? {
            for (list in lists) {
                if (list.isAllTasks)
                    continue

                if (list.id.contentEquals(sId))
                    return list
            }

            return null
        }

        fun getDefault(lists: List<TaskList>): TaskList? {
            for (list in lists) {
                if (list.isDefault)
                    return list
            }

            return null
        }
    }
}
