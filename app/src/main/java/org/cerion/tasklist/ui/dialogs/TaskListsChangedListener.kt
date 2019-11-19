package org.cerion.tasklist.ui.dialogs

import org.cerion.tasklist.database.TaskList

interface TaskListsChangedListener {
    fun onTaskListsChanged(currList: TaskList)
}
