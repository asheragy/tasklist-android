package org.cerion.tasklist.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.cerion.tasklist.data.AppDatabase;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskDao;
import org.cerion.tasklist.data.TaskList;

import java.util.List;

import androidx.fragment.app.DialogFragment;

public class MoveTaskDialogFragment extends DialogFragment {

    private static final String TASK_LISTID = "taskListId";
    private static final String TASK_ID = "taskId";
    private static final String TAG = MoveTaskDialogFragment.class.getSimpleName();

    public static MoveTaskDialogFragment newInstance(Task task) {
        MoveTaskDialogFragment frag = new MoveTaskDialogFragment();

        Bundle args = new Bundle();
        args.putString(TASK_LISTID, task.getListId());
        args.putString(TASK_ID, task.getId());

        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ArrayAdapter<TaskList> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item);

        final AppDatabase db = AppDatabase.getInstance(getActivity());
        final TaskDao taskDb = db.taskDao();
        final List<TaskList> lists = db.taskListDao().getAll();

        adapter.addAll(lists);

        Bundle bundle = getArguments();
        String taskId = bundle.getString(TASK_ID);
        final String listId = bundle.getString(TASK_LISTID);
        final Task task = taskDb.get(listId, taskId);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Move to list")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(listId.length() > 0) {
                            TaskList list = lists.get(which);
                            if(list.getId().contentEquals(listId)) {
                                Log.d(TAG,"Ignoring moving since same list");
                            } else {
                                // Delete task, add to new list and refresh
                                // TODO if temp ID permanently delete
                                task.setModified();
                                task.setDeleted(true);
                                taskDb.update(task);

                                task.setDeleted(false);
                                task.setListId(list.getId());
                                task.setId(AppDatabase.generateTempId());
                                taskDb.add(task);
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
