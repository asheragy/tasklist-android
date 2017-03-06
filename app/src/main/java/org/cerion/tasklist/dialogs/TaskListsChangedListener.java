package org.cerion.tasklist.dialogs;

import org.cerion.tasklist.data.TaskList;

public interface TaskListsChangedListener {
    void onTaskListsChanged(TaskList currList);
}
