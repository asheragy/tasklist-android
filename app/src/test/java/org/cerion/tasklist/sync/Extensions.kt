package org.cerion.tasklist.sync

import org.cerion.tasklist.database.TaskList
import java.util.*

fun TaskList.fullCopy() = TaskList(id, title).also { copy ->
    copy.deleted = deleted
    copy.updated = updated
    copy.isDefault = isDefault
}

fun Date.add(seconds: Int) = Date(time + (seconds * 1000))