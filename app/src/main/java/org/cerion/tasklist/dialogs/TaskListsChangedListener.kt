package org.cerion.tasklist.dialogs

import org.cerion.tasklist.data.TaskList

interface TaskListsChangedListener {
    fun onTaskListsChanged(currList: TaskList)
}
