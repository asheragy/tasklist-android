package org.cerion.tasklist.ui.dialogs

import org.cerion.tasklist.data.TaskList

interface TaskListsChangedListener {
    fun onTaskListsChanged(currList: TaskList)
}
