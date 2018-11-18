package org.cerion.tasklist.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import org.cerion.tasklist.data.AppDatabase;
import org.cerion.tasklist.data.TaskList;
import org.cerion.tasklist.data.TaskListDao;

import androidx.fragment.app.DialogFragment;

public class TaskListDialogFragment extends DialogFragment {

    private static final String TAG = TaskListDialogFragment.class.getSimpleName();
    private static final String TYPE = "type";
    private static final String LISTID = "id";
    private static final String LISTNAME = "name";

    public static final int TYPE_ADD = 0;
    public static final int TYPE_RENAME = 1;

    public static TaskListDialogFragment newInstance(int type, TaskList list) {
        TaskListDialogFragment frag = new TaskListDialogFragment();

        Bundle args = new Bundle();
        args.putInt(TYPE, type);
        if(list != null) {
            args.putString(LISTID,list.id);
            args.putString(LISTNAME,list.title);
        }
        frag.setArguments(args);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText edittext= new EditText(getActivity());
        alert.setTitle("Task List Name");
        alert.setView(edittext);

        Bundle bundle = getArguments();
        int type = bundle.getInt(TYPE);
        if(type == TYPE_ADD) {


            alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String name = edittext.getText().toString();

                    TaskListDao db = AppDatabase.getInstance(getActivity()).taskListDao();
                    TaskList update = new TaskList(name);
                    db.add(update);

                    ((TaskListsChangedListener) getActivity()).onTaskListsChanged(update);
                }
            });

        }
        else if(type == TYPE_RENAME) {
            final String listId = bundle.getString(LISTID);
            final String listName = bundle.getString(LISTNAME);
            edittext.setText(listName);

            alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String newName = edittext.getText().toString();
                    Log.d(TAG, "Rename " + listName + " to " + newName);

                    TaskList update = new TaskList(listId, newName, true);
                    TaskListDao db = AppDatabase.getInstance(getActivity()).taskListDao();
                    db.update(update);

                    ((TaskListsChangedListener) getActivity()).onTaskListsChanged(update);
                }
            });
        }

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d(TAG,"onCancel");
            }
        });

        return alert.create();
    }
}
