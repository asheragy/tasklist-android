package org.cerion.tasklist.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.cerion.tasklist.data.Database;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskList;

import java.util.List;

public class MoveTaskDialogFragment extends DialogFragment {

    private static final String TASK_LISTID = "taskListId";
    private static final String TASK_ID = "taskId";
    private static final String TAG = MoveTaskDialogFragment.class.getSimpleName();

    public static MoveTaskDialogFragment newInstance(Task task) {
        MoveTaskDialogFragment frag = new MoveTaskDialogFragment();

        Bundle args = new Bundle();
        args.putString(TASK_LISTID, task.listId);
        args.putString(TASK_ID, task.id);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ArrayAdapter adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_dropdown_item);
        final Database db = Database.getInstance(getActivity());
        final List<TaskList> lists = db.taskLists.getList();
        adapter.addAll(lists);

        Bundle bundle = getArguments();
        String taskId = bundle.getString(TASK_ID);
        final String listId = bundle.getString(TASK_LISTID);

        List<Task> tasks = db.tasks.getList(listId);
        final Task task = Task.getTask(tasks, taskId);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Move to list")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(task != null && listId != null) {
                            TaskList list = lists.get(which);
                            if(list.id.contentEquals(listId)) {
                                Log.d(TAG,"Ignoring moving since same list");
                            } else {
                                // Delete task, add to new list and refresh
                                task.setDeleted(true);
                                db.tasks.update(task);
                                task.setDeleted(false);

                                task.moveToList(list.id);
                                db.tasks.add(task);
                                ((TaskListsChangedListener) getActivity()).onTaskListsChanged(list);
                            }

                        } else {
                            Log.e(TAG, "Error, unable to find task");
                        }
                    }
                });

        return builder.create();
    }
}
