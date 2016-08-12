package org.cerion.tasklist.ui;


import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toolbar;


import org.cerion.tasklist.R;
import org.cerion.tasklist.data.TaskList;

import java.util.ArrayList;
import java.util.List;

public class TaskListsToolbar extends Toolbar {

    private static final String TAG = TaskListsToolbar.class.getSimpleName();

    private final List<TaskList> mTaskLists = new ArrayList<>();
    private Spinner mSpinner;
    private ArrayAdapter<TaskList> mSpinnerAdapter;
    private TaskListsChangeListener mListener;

    public interface TaskListsChangeListener {
        TaskList getCurrentList();
        void setCurrentList(TaskList list);
        void onListChanged();
    }

    public TaskListsToolbar(Context context) {
        super(context);
        init(context);
    }

    public TaskListsToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TaskListsToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.toolbar_tasklists, this);
        mSpinner = (Spinner)findViewById(R.id.spinner);

        mSpinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, mTaskLists);
        mSpinner.setAdapter(mSpinnerAdapter);

        mListener = (TaskListsChangeListener)context;

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onNavigationItemSelected: " + position + " index = " + mSpinner.getSelectedItemPosition());
                mListener.setCurrentList( mTaskLists.get(position) );
                mListener.onListChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private int getListPosition(TaskList list) {
        String id = list.id;
        int index = 0;
        if (id != null) {
            for (int i = 1; i < mSpinnerAdapter.getCount(); i++) { //Skip first since its default
                TaskList curr = mSpinnerAdapter.getItem(i);
                if (curr.id.contentEquals(id))
                    index = i;
            }
        }

        Log.d(TAG, "listPosition = " + index);
        return index;
    }

    public void refresh(List<TaskList> lists) {
        mTaskLists.clear();
        mTaskLists.add(TaskList.ALL_TASKS);
        mTaskLists.addAll(lists);
        mSpinnerAdapter.notifyDataSetChanged();

        //Re-select last position
        mSpinner.setSelection(getListPosition( mListener.getCurrentList()));
    }

    public TaskList getDefaultList() {
        return TaskList.getDefault(mTaskLists);
    }
}
